/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.persist.timescale.services;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivityOccurrenceDataArchive extends AbstractDataItemArchive<ActivityOccurrenceData, ActivityOccurrenceDataFilter> implements IActivityOccurrenceDataArchive {

    private static final Logger LOG = Logger.getLogger(ActivityOccurrenceDataArchive.class.getName());

    private static final String OCCURRENCE_STORE_STATEMENT = "INSERT INTO ACTIVITY_OCCURRENCE_DATA_TABLE(UniqueId,GenerationTime,ExternalId,Name,Path,Type,Route,Source,Arguments,Properties,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String REPORT_STORE_STATEMENT = "INSERT INTO ACTIVITY_REPORT_DATA_TABLE(UniqueId,GenerationTime,Name,ExecutionTime,State,NextState,ReportStatus,Result,ActivityOccurrenceId,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?)";

    private static final String OCCURRENCE_LAST_ID_QUERY = "SELECT UniqueId FROM ACTIVITY_OCCURRENCE_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String REPORT_LAST_ID_QUERY = "SELECT UniqueId FROM ACTIVITY_REPORT_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";

    private static final String START_FULL_JOIN_QUERY = "SELECT ao.UniqueId,ao.GenerationTime,ao.ExternalId,ao.Name,ao.Path,ao.Type,ao.Route,ao.Source,ao.Arguments,ao.Properties,ao.AdditionalData," +
            "r.UniqueId,r.GenerationTime,r.Name,r.ExecutionTime,r.State,r.NextState,r.ReportStatus,r.Result,r.ActivityOccurrenceId,r.AdditionalData " +
            "FROM ACTIVITY_OCCURRENCE_DATA_TABLE AS ao JOIN ACTIVITY_REPORT_DATA_TABLE AS r ON ao.UniqueId = r.ActivityOccurrenceId ";
    private static final String RETRIEVE_BY_ID_QUERY = START_FULL_JOIN_QUERY + "WHERE ao.UniqueId=? ORDER BY r.GenerationTime ASC, r.UniqueId ASC";

    private static final String OCCURRENCE_LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM ACTIVITY_OCCURRENCE_DATA_TABLE";
    private static final String REPORT_LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM ACTIVITY_REPORT_DATA_TABLE";

    private PreparedStatement occurrenceStoreStatement;
    private PreparedStatement reportStoreStatement;

    public ActivityOccurrenceDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void doStore(Connection connection, List<ActivityOccurrenceData> itemsToStore) throws SQLException {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - request to store " + itemsToStore.size() + " items");
        }
        if(occurrenceStoreStatement != null) {
            occurrenceStoreStatement.clearBatch();
        }
        for(ActivityOccurrenceData aod : itemsToStore) {
            // If the activity occurrence has no progress reports, or a single progress report marked as CREATION, then add the activity occurrence.
            if(aod.getProgressReports().size() == 0 || (aod.getProgressReports().size() == 1 && aod.getProgressReports().get(0).getState() == ActivityOccurrenceState.CREATION)) {
                if (occurrenceStoreStatement == null) {
                    if(LOG.isLoggable(Level.FINER)) {
                        LOG.finer(this + " - creating occurrence store statement");
                    }
                    occurrenceStoreStatement = createStoreStatement(connection);
                }
                setItemPropertiesToStatement(occurrenceStoreStatement, aod);
                occurrenceStoreStatement.addBatch();
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Storing activity occurrence data: " + aod);
                }
            }
            // Then, add only the 'last' progress report: this strategy assumes that the reports have an increasing IUniqueId,
            // which is not necessarily the case. But we cannot add/replace everything everytime (even though this is not telemetry,
            // we should avoid it.
            if(!aod.getProgressReports().isEmpty()) {
                ActivityOccurrenceReport reportToStore = aod.getProgressReports().get(aod.getProgressReports().size() - 1);
                if (reportStoreStatement == null) {
                    reportStoreStatement = createReportStoreStatement(connection);
                }
                setItemPropertiesToReportStatement(reportStoreStatement, aod, reportToStore);
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Storing activity occurrence report for activity " + aod.getInternalId() + ": " + aod);
                }
                reportStoreStatement.addBatch();
            }
        }
        // Execute the occurrenceStoreStatement batches
        executeStoreBatchAndClear(occurrenceStoreStatement);
        // Execute the progress report batches
        executeStoreBatchAndClear(reportStoreStatement);
    }

    private void executeStoreBatchAndClear(PreparedStatement preparedStatement) throws SQLException {
        if(preparedStatement == null) {
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer(this + " - Not storing the prepared statement, it is null");
            }
            return;
        }
        int[] numUpdates = preparedStatement.executeBatch();
        if (LOG.isLoggable(Level.FINEST)) {
            for (int i = 0; i < numUpdates.length; i++) {
                if (numUpdates[i] == -2) {
                    LOG.finest("Batch job[" + i +
                            "]: unknown number of rows added/updated");
                } else {
                    LOG.finest("Batch job[" + i +
                            "]: " + numUpdates[i] + " rows added/updated");
                }
            }
        }
        preparedStatement.clearBatch();
    }

    protected void setItemPropertiesToReportStatement(PreparedStatement storeStatement, ActivityOccurrenceData parent, ActivityOccurrenceReport item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setString(3, item.getName());
        if(item.getExecutionTime() == null) {
            storeStatement.setNull(4, Types.TIMESTAMP);
        } else {
            storeStatement.setTimestamp(4, toTimestamp(item.getExecutionTime()));
        }
        storeStatement.setShort(5, (short) item.getState().ordinal());
        storeStatement.setShort(6, (short) item.getStateTransition().ordinal());
        storeStatement.setShort(7, (short) item.getStatus().ordinal());
        storeStatement.setBytes(8, toBytes(item.getResult()));
        storeStatement.setLong(9, parent.getInternalId().asLong());
        storeStatement.setBytes(10, toBytes(item.getExtension()));
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, ActivityOccurrenceData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setInt(3, item.getExternalId());
        storeStatement.setString(4, item.getName());
        storeStatement.setString(5, item.getPath().asString());
        storeStatement.setString(6, item.getType());
        storeStatement.setString(7, item.getRoute() != null && item.getRoute().length() > 48 ? item.getRoute().substring(0, 48): item.getRoute());
        storeStatement.setString(8, item.getSource());
        storeStatement.setBytes(9, toBytes(item.getArguments()));
        storeStatement.setBytes(10, toBytes(item.getProperties()));
        storeStatement.setBytes(11, toBytes(item.getExtension()));
    }

    @Override
    protected PreparedStatement createStoreStatement(Connection connection) throws SQLException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - preparing activity occurrence store statement: " + OCCURRENCE_STORE_STATEMENT);
        }
        return connection.prepareStatement(OCCURRENCE_STORE_STATEMENT);
    }

    protected PreparedStatement createReportStoreStatement(Connection connection) throws SQLException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - preparing activity report store statement: " + REPORT_STORE_STATEMENT);
        }
        return connection.prepareStatement(REPORT_STORE_STATEMENT);
    }

    @Override
    protected ActivityOccurrenceData doRetrieve(Connection connection, IUniqueId uniqueId) throws SQLException {
        // Make a selection on both tables with a join on the activity occurrence ID, sort by report generation time ASC, report unique ID ASC
        String finalQuery = RETRIEVE_BY_ID_QUERY;
        ActivityOccurrenceData result = null;
        ActivityOccurrenceData temporaryResult = null;
        try (PreparedStatement prepStmt = connection.prepareStatement(finalQuery)) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            prepStmt.setLong(1, uniqueId.asLong());
            try (ResultSet rs = prepStmt.executeQuery()) {
                List<ActivityOccurrenceReport> reports = new LinkedList<>();
                while (rs.next()) {
                    try {
                        // Build an empty activity occurrence
                        if(temporaryResult == null) {
                            temporaryResult = mapToOccurrenceItem(rs, reports);
                        }
                        // Build the report and add it to the list
                        reports.add(mapToReportItem(rs, 11));
                    } catch (IOException e) {
                        throw new SQLException(e);
                    }
                }
                // Build the final activity occurrence
                if(temporaryResult != null) {
                    result = new ActivityOccurrenceData(temporaryResult.getInternalId(), temporaryResult.getGenerationTime(),
                            temporaryResult.getExtension(), temporaryResult.getExternalId(), temporaryResult.getName(),
                            temporaryResult.getPath(), temporaryResult.getType(), temporaryResult.getArguments(),
                            temporaryResult.getProperties(), reports, temporaryResult.getRoute(), temporaryResult.getSource());
                }
            } finally {
                connection.commit();
            }
        }
        return result;
    }

    private ActivityOccurrenceReport mapToReportItem(ResultSet rs, int offset) throws SQLException, IOException {
        long uniqueId = rs.getLong(offset + 1);
        Timestamp genTime = rs.getTimestamp(offset + 2);
        String name = rs.getString(offset + 3);
        Timestamp execTime = rs.getTimestamp(offset + 4);
        if(rs.wasNull()) {
            execTime = null;
        }
        ActivityOccurrenceState state = ActivityOccurrenceState.values()[rs.getShort(offset + 5)];
        ActivityOccurrenceState nextState = ActivityOccurrenceState.values()[rs.getShort(offset + 6)];
        ActivityReportState reportStatus = ActivityReportState.values()[rs.getShort(offset + 7)];
        Object result = toObject(rs.getBytes(offset + 8));
        byte[] extensionData = rs.getBytes(offset + 10);
        Object extension = null;
        if(extensionData != null && !rs.wasNull()) {
            extension = toObject(extensionData);
        }
        return new ActivityOccurrenceReport(new LongUniqueId(uniqueId),toInstant(genTime),extension, name, state, toInstant(execTime),reportStatus, nextState, result);
    }

    private ActivityOccurrenceData mapToOccurrenceItem(ResultSet rs, List<ActivityOccurrenceReport> reports) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        int externalId = rs.getInt(3);
        String name = rs.getString(4);
        String path = rs.getString(5);
        String type = rs.getString(6);
        String route = rs.getString(7);
        String source = rs.getString(8);
        Map<String, Object> arguments = (Map<String, Object>) toObject(rs.getBytes(9));
        Map<String, String> properties = (Map<String, String>) toObject(rs.getBytes(10));
        byte[] extensionData = rs.getBytes(11);
        Object extension = null;
        if(extensionData != null && !rs.wasNull()) {
            extension = toObject(extensionData);
        }
        return new ActivityOccurrenceData(new LongUniqueId(uniqueId), toInstant(genTime),
                extension, externalId, name, SystemEntityPath.fromString(path),
                type, arguments, properties, reports, route, source);
    }

    @Override
    protected String buildRetrieveByIdQuery() {
        throw new UnsupportedOperationException("Operation not supposed to be called in this implementation");
    }

    @Override
    protected List<ActivityOccurrenceData> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, ActivityOccurrenceDataFilter filter) throws SQLException {
        if(startTime.isBefore(MINIMUM_TIME)) {
            startTime = MINIMUM_TIME;
        } else if(startTime.isAfter(MAXIMUM_TIME)) {
            startTime = MAXIMUM_TIME;
        }
        String finalQuery = buildRetrieveQuery(startTime, numRecords, direction, filter);
        return retrieveAndBuild(connection, filter, finalQuery);
    }

    @Override
    protected List<ActivityOccurrenceData> doRetrieve(Connection connection, Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, ActivityOccurrenceDataFilter filter) throws SQLException {
        if(startTime.isBefore(MINIMUM_TIME)) {
            startTime = MINIMUM_TIME;
        } else if(startTime.isAfter(MAXIMUM_TIME)) {
            startTime = MAXIMUM_TIME;
        }
        String finalQuery = buildRetrieveQuery(startTime, internalId, numRecords, direction, filter);
        return retrieveAndBuild(connection, filter, finalQuery);
    }

    private List<ActivityOccurrenceData> retrieveAndBuild(Connection connection, ActivityOccurrenceDataFilter filter, String finalQuery) throws SQLException {
        List<ActivityOccurrenceData> result = new ArrayList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                ActivityOccurrenceData temporaryResult = null;
                List<ActivityOccurrenceReport> reports = new LinkedList<>();
                while (rs.next()) {
                    try {
                        ActivityOccurrenceData theOccurrence = mapToOccurrenceItem(rs, reports);
                        // Build an empty activity occurrence
                        if(temporaryResult == null) {
                            temporaryResult = theOccurrence;
                        } else if(!temporaryResult.getInternalId().equals(theOccurrence.getInternalId())) {
                            // Close the occurrence
                            ActivityOccurrenceData fullOccurrence = new ActivityOccurrenceData(temporaryResult.getInternalId(), temporaryResult.getGenerationTime(),
                                    temporaryResult.getExtension(), temporaryResult.getExternalId(), temporaryResult.getName(),
                                    temporaryResult.getPath(), temporaryResult.getType(), temporaryResult.getArguments(),
                                    temporaryResult.getProperties(), reports, temporaryResult.getRoute(), temporaryResult.getSource());
                            if(checkStateFilter(filter, fullOccurrence)) {
                                result.add(fullOccurrence);
                            }
                            reports = new LinkedList<>();
                            // Set the next occurrence
                            temporaryResult = theOccurrence;
                        }
                        // Build the report and add it to the list
                        reports.add(mapToReportItem(rs, 11));
                    } catch (IOException e) {
                        throw new SQLException(e);
                    }
                }
                // Last occurrence, if there is one
                if(temporaryResult != null) {
                    // Close the occurrence
                    ActivityOccurrenceData fullOccurrence = new ActivityOccurrenceData(temporaryResult.getInternalId(), temporaryResult.getGenerationTime(),
                            temporaryResult.getExtension(), temporaryResult.getExternalId(), temporaryResult.getName(),
                            temporaryResult.getPath(), temporaryResult.getType(), temporaryResult.getArguments(),
                            temporaryResult.getProperties(), reports, temporaryResult.getRoute(), temporaryResult.getSource());
                    if(checkStateFilter(filter, fullOccurrence)) {
                        result.add(fullOccurrence);
                    }
                }
            } finally {
                connection.commit();
            }
        }
        return result;
    }

    private boolean checkStateFilter(ActivityOccurrenceDataFilter filter, ActivityOccurrenceData fullOccurrence) {
        return filter == null || filter.isClear() || filter.getStateList() == null || filter.getStateList().contains(fullOccurrence.getCurrentState());
    }

    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, ActivityOccurrenceDataFilter filter) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ao.UniqueId,ao.GenerationTime,ao.ExternalId,ao.Name,ao.Path,ao.Type,ao.Route,ao.Source,ao.Arguments,ao.Properties,ao.AdditionalData," +
                "r.UniqueId,r.GenerationTime,r.Name,r.ExecutionTime,r.State,r.NextState,r.ReportStatus,r.Result,r.ActivityOccurrenceId,r.AdditionalData " +
                "FROM ACTIVITY_REPORT_DATA_TABLE AS r JOIN ");
        query.append("(SELECT UniqueId,GenerationTime,ExternalId,Name,Path,Type,Route,Source,Arguments,Properties,AdditionalData FROM ACTIVITY_OCCURRENCE_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            // For the activity occurrence state we use application post-filtering... for the time being
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC ");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC ");
        }
        query.append("FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        query.append(") AS ao ON ao.UniqueId = r.ActivityOccurrenceId ");
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY ao.GenerationTime ASC, ao.UniqueId ASC, r.UniqueId ASC");
        } else {
            query.append("ORDER BY ao.GenerationTime DESC, ao.UniqueId DESC, r.UniqueId ASC");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, ActivityOccurrenceDataFilter filter) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ao.UniqueId,ao.GenerationTime,ao.ExternalId,ao.Name,ao.Path,ao.Type,ao.Route,ao.Source,ao.Arguments,ao.Properties,ao.AdditionalData," +
                "r.UniqueId,r.GenerationTime,r.Name,r.ExecutionTime,r.State,r.NextState,r.ReportStatus,r.Result,r.ActivityOccurrenceId,r.AdditionalData " +
                "FROM ACTIVITY_REPORT_DATA_TABLE AS r JOIN ");
        query.append("(SELECT UniqueId,GenerationTime,ExternalId,Name,Path,Type,Route,Source,Arguments,Properties,AdditionalData FROM ACTIVITY_OCCURRENCE_DATA_TABLE WHERE ");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            // For the activity occurrence state we use application post-filtering... for the time being
        }
        // order by and limit
        if(ascending) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC ");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC ");
        }
        query.append(") AS ao ON ao.UniqueId = r.ActivityOccurrenceId ");
        // order by and limit
        if(ascending) {
            query.append("ORDER BY ao.GenerationTime ASC, ao.UniqueId ASC, r.UniqueId ASC");
        } else {
            query.append("ORDER BY ao.GenerationTime DESC, ao.UniqueId DESC, r.UniqueId ASC");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, ActivityOccurrenceDataFilter filter) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ao.UniqueId,ao.GenerationTime,ao.ExternalId,ao.Name,ao.Path,ao.Type,ao.Route,ao.Source,ao.Arguments,ao.Properties,ao.AdditionalData," +
                "r.UniqueId,r.GenerationTime,r.Name,r.ExecutionTime,r.State,r.NextState,r.ReportStatus,r.Result,r.ActivityOccurrenceId,r.AdditionalData " +
                "FROM ACTIVITY_REPORT_DATA_TABLE AS r JOIN ");
        query.append("(SELECT UniqueId,GenerationTime,ExternalId,Name,Path,Type,Route,Source,Arguments,Properties,AdditionalData FROM ACTIVITY_OCCURRENCE_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            // For the activity occurrence state we use application post-filtering... for the time being
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC ");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC ");
        }
        query.append("FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        query.append(") AS ao ON ao.UniqueId = r.ActivityOccurrenceId ");
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY ao.GenerationTime ASC, ao.UniqueId ASC, r.UniqueId ASC");
        } else {
            query.append("ORDER BY ao.GenerationTime DESC, ao.UniqueId DESC, r.UniqueId ASC");
        }
        return query.toString();
    }

    @Override
    public synchronized List<ActivityOccurrenceData> retrieve(Instant time, ActivityOccurrenceDataFilter filter, Instant maxLookbackTime) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter, maxLookbackTime);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<ActivityOccurrenceData> doRetrieve(Connection retrieveConnection, Instant time, ActivityOccurrenceDataFilter filter, Instant maxLookbackTime) throws SQLException {
        if(time.isBefore(MINIMUM_TIME)) {
            time = MINIMUM_TIME;
        } else if(time.isAfter(MAXIMUM_TIME)) {
            time = MAXIMUM_TIME;
        }
        String query = buildRetrieveByTimeQuery(time, filter, maxLookbackTime);
        return retrieveAndBuild(retrieveConnection, filter, query);
    }

    protected String buildRetrieveByTimeQuery(Instant time, ActivityOccurrenceDataFilter filter, Instant maxLookbackTime) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ao.UniqueId,ao.GenerationTime,ao.ExternalId,ao.Name,ao.Path,ao.Type,ao.Route,ao.Source,ao.Arguments,ao.Properties,ao.AdditionalData," +
                "r.UniqueId,r.GenerationTime,r.Name,r.ExecutionTime,r.State,r.NextState,r.ReportStatus,r.Result,r.ActivityOccurrenceId,r.AdditionalData " +
                "FROM ACTIVITY_REPORT_DATA_TABLE AS r JOIN ");
        query.append("(SELECT UniqueId,GenerationTime,ExternalId,Name,Path,Type,Route,Source,Arguments,Properties,AdditionalData FROM ACTIVITY_OCCURRENCE_DATA_TABLE WHERE ");
        // add time info
        query.append("GenerationTime <= '").append(toTimestamp(time)).append("' ");
        if(maxLookbackTime != null) {
            query.append(" AND GenerationTime >= '").append(toTimestamp(maxLookbackTime)).append("' ");
        }
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            // For the activity occurrence state we use application post-filtering... for the time being
        }
        // order by and limit
        query.append("ORDER BY GenerationTime DESC, UniqueId DESC");
        query.append(") AS ao ON ao.UniqueId = r.ActivityOccurrenceId ");
        // order by and limit
        query.append("ORDER BY ao.GenerationTime DESC, ao.UniqueId DESC, r.UniqueId ASC");
        return query.toString();
    }

    @Override
    protected ActivityOccurrenceData mapToItem(ResultSet rs, ActivityOccurrenceDataFilter usedFilter) {
        throw new UnsupportedOperationException("Operation not supposed to be called in this implementation");
    }

    @Override
    protected String getLastIdQuery(Class<? extends AbstractDataItem> type) {
        if(type.equals(ActivityOccurrenceData.class)) {
            return getLastIdQuery();
        } else if(type.equals(ActivityOccurrenceReport.class)) {
            return REPORT_LAST_ID_QUERY;
        } else {
            throw new UnsupportedOperationException("Provided type " + type.getName() + " not supported by " + this);
        }
    }

    @Override
    protected String getLastIdQuery() {
        return OCCURRENCE_LAST_ID_QUERY;
    }

    @Override
    protected String getLastGenerationTimeQuery(Class<? extends AbstractDataItem> type) {
        if(type.equals(ActivityOccurrenceData.class)) {
            return OCCURRENCE_LAST_GENERATION_TIME_QUERY;
        } else if(type.equals(ActivityOccurrenceReport.class)) {
            return REPORT_LAST_GENERATION_TIME_QUERY;
        } else {
            throw new UnsupportedOperationException("Provided type " + type.getName() + " not supported by " + this);
        }
    }

    @Override
    protected List<String> getPurgeQuery(Instant referenceTime, RetrievalDirection direction) {
        return Arrays.asList(
                "DELETE FROM ACTIVITY_REPORT_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'",
                "DELETE FROM ACTIVITY_OCCURRENCE_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Activity Occurrence Archive";
    }

    @Override
    protected Class<ActivityOccurrenceData> getMainType() {
        return ActivityOccurrenceData.class;
    }
}
