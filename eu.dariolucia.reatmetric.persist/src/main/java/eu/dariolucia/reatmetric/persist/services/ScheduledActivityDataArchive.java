/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduledActivityDataArchive extends AbstractDataItemArchive<ScheduledActivityData, ScheduledActivityDataFilter> implements IScheduledActivityDataArchive {

    private static final Logger LOG = Logger.getLogger(ScheduledActivityDataArchive.class.getName());

    private static final String STORE_STATEMENT = "MERGE INTO SCHEDULED_ACTIVITY_DATA_TABLE USING SYSIBM.SYSDUMMY1 ON UniqueId = ? " +
            "WHEN MATCHED THEN UPDATE SET GenerationTime = ?, ActivityRequest = ?, Path = ?, ActivityOccurrence = ?, Resources = ?, Source = ?, ExternalId = ?, Trigger = ?, LatestInvocationTime = ?, StartTime = ?, Duration = ?, ConflictStrategy = ?, State = ?, AdditionalData = ? " +
            "WHEN NOT MATCHED THEN INSERT (UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,StartTime,Duration,ConflictStrategy,State,AdditionalData) VALUES (?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM SCHEDULED_ACTIVITY_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,StartTime,Duration,ConflictStrategy,State,AdditionalData " +
            "FROM SCHEDULED_ACTIVITY_DATA_TABLE " +
            "WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM SCHEDULED_ACTIVITY_DATA_TABLE";

    public ScheduledActivityDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, ScheduledActivityData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setBlob(3, toInputstream(item.getRequest()));
        storeStatement.setString(4, item.getRequest().getPath().asString());
        if(item.getActivityOccurrence() != null) {
            storeStatement.setLong(5, item.getActivityOccurrence().asLong());
        } else {
            storeStatement.setNull(5, Types.BIGINT);
        }
        String resources = formatResources(item.getResources());
        storeStatement.setString(6, resources);
        storeStatement.setString(7, item.getSource());
        storeStatement.setString(8, item.getExternalId());
        storeStatement.setBlob(9, toInputstream(item.getTrigger()));
        if(item.getLatestInvocationTime() != null) {
            storeStatement.setTimestamp(10, toTimestamp(item.getLatestInvocationTime()));
        } else {
            storeStatement.setNull(10, Types.TIMESTAMP);
        }
        storeStatement.setTimestamp(11, toTimestamp(item.getStartTime()));
        storeStatement.setInt(12, (int) item.getDuration().toSeconds());
        storeStatement.setShort(13, (short) item.getConflictStrategy().ordinal());
        storeStatement.setShort(14, (short) item.getState().ordinal());
        Object extension = item.getExtension();
        if(extension == null) {
            storeStatement.setNull(15, Types.BLOB);
        } else {
            storeStatement.setBlob(15, toInputstream(item.getExtension()));
        }

        storeStatement.setLong(16, item.getInternalId().asLong());
        storeStatement.setTimestamp(17, toTimestamp(item.getGenerationTime()));
        storeStatement.setBlob(18, toInputstream(item.getRequest()));
        storeStatement.setString(19, item.getRequest().getPath().asString());
        if(item.getActivityOccurrence() != null) {
            storeStatement.setLong(20, item.getActivityOccurrence().asLong());
        } else {
            storeStatement.setNull(20, Types.BIGINT);
        }
        storeStatement.setString(21, resources);
        storeStatement.setString(22, item.getSource());
        storeStatement.setString(23, item.getExternalId());
        storeStatement.setBlob(24, toInputstream(item.getTrigger()));
        if(item.getLatestInvocationTime() != null) {
            storeStatement.setTimestamp(25, toTimestamp(item.getLatestInvocationTime()));
        } else {
            storeStatement.setNull(25, Types.TIMESTAMP);
        }
        storeStatement.setTimestamp(26, toTimestamp(item.getStartTime()));
        storeStatement.setInt(27, (int) item.getDuration().toSeconds());
        storeStatement.setShort(28, (short) item.getConflictStrategy().ordinal());
        storeStatement.setShort(29, (short) item.getState().ordinal());
        if(extension == null) {
            storeStatement.setNull(30, Types.BLOB);
        } else {
            storeStatement.setBlob(30, toInputstream(item.getExtension()));
        }
    }

    private String formatResources(Set<String> resources) {
        if(resources == null || resources.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ");
        for(String res : resources) {
            sb.append(res).append(' ');
        }
        return sb.toString();
    }

    @Override
    protected PreparedStatement createStoreStatement(Connection connection) throws SQLException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - preparing store statement: " + STORE_STATEMENT);
        }
        return connection.prepareStatement(STORE_STATEMENT);
    }

    @Override
    protected String buildRetrieveByIdQuery() {
        return RETRIEVE_BY_ID_QUERY;
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) {
        return buildRetrieveQuery(startTime, null, numRecords, direction, filter);
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,StartTime,Duration,ConflictStrategy,State,AdditionalData " +
                "FROM SCHEDULED_ACTIVITY_DATA_TABLE " +
                "WHERE ");
        // add time info
        if(internalId == null) {
            addTimeInfo(query, startTime, direction);
        } else {
            addTimeInfo(query, startTime, internalId, direction);
        }
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            if(filter.getSchedulingStateList() != null && !filter.getSchedulingStateList().isEmpty()) {
                query.append("AND State IN (").append(toEnumFilterListString(filter.getSchedulingStateList())).append(") ");
            }
            if(filter.getResourceList() != null && !filter.getResourceList().isEmpty()) {
                List<String> resourcesList = new ArrayList<>(filter.getResourceList());
                query.append("AND (");
                for(int i = 0; i < resourcesList.size(); ++i) {
                    query.append("Resources LIKE '").append("% ").append(resourcesList.get(i)).append(" %'");
                    if(i != resourcesList.size() - 1) {
                        query.append(" OR ");
                    }
                }
                query.append(")");
            }
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        }
        return query.toString();
    }

    @Override
    protected ScheduledActivityData mapToItem(ResultSet rs, ScheduledActivityDataFilter usedFilter) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        ActivityRequest request = (ActivityRequest) toObject(rs.getBlob(3));
        Long actOcc = rs.getLong(5);
        if(rs.wasNull()) {
            actOcc = null;
        }
        Set<String> resources = parseResources(rs.getString(6));
        String source = rs.getString(7);
        String extId = rs.getString(8);
        AbstractSchedulingTrigger trigger = (AbstractSchedulingTrigger) toObject(rs.getBlob(9));
        Timestamp latestInvokeTime = rs.getTimestamp(10);
        if(rs.wasNull()) {
            latestInvokeTime = null;
        }
        Timestamp startTime = rs.getTimestamp(11);

        int duration = rs.getInt(12);

        ConflictStrategy conflictStrategy = ConflictStrategy.values()[rs.getShort(13)];
        SchedulingState state = SchedulingState.values()[rs.getShort(14)];
        Blob extensionBlob = rs.getBlob(15);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new ScheduledActivityData(new LongUniqueId(uniqueId), toInstant(genTime), request,
                actOcc == null ? null : new LongUniqueId(actOcc), resources, source, extId, trigger, toInstant(latestInvokeTime), toInstant(startTime), Duration.ofSeconds(duration), conflictStrategy, state, extension);
    }

    private Set<String> parseResources(String string) {
        Set<String> toReturn = new TreeSet<>();
        if(string.isBlank()) {
            return toReturn;
        }
        // Remove the first and the last whitespace
        string = string.trim();
        // Split on space
        int currentIdx = 0;
        int wsIdx;
        while((wsIdx = string.indexOf(' ', currentIdx)) != -1) {
            toReturn.add(string.substring(currentIdx, wsIdx).trim());
            currentIdx = wsIdx + 1;
        }
        // Add the last one
        toReturn.add(string.substring(currentIdx));
        return toReturn;
    }


    @Override
    public synchronized List<ScheduledActivityData> retrieve(Instant time, ScheduledActivityDataFilter filter, Instant maxLookBackTime) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter, maxLookBackTime);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<ScheduledActivityData> doRetrieve(Connection connection, Instant time, ScheduledActivityDataFilter filter, Instant maxLookBackTime) throws SQLException {
        if(time.isBefore(MINIMUM_TIME)) {
            time = MINIMUM_TIME;
        } else if(time.isAfter(MAXIMUM_TIME)) {
            time = MAXIMUM_TIME;
        }
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,StartTime,Duration,ConflictStrategy,State,AdditionalData " +
                "FROM SCHEDULED_ACTIVITY_DATA_TABLE " +
                "WHERE ");
        // add time info
        query.append("GenerationTime >= '").append(toTimestamp(maxLookBackTime).toString()).append("' AND GenerationTime <= '").append(toTimestamp(time).toString()).append("' ");
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            if(filter.getSchedulingStateList() != null && !filter.getSchedulingStateList().isEmpty()) {
                query.append("AND State IN (").append(toEnumFilterListString(filter.getSchedulingStateList())).append(") ");
            }
            if(filter.getResourceList() != null && !filter.getResourceList().isEmpty()) {
                List<String> resourcesList = new ArrayList<>(filter.getResourceList());
                query.append("AND (");
                for(int i = 0; i < resourcesList.size(); ++i) {
                    query.append("Resources LIKE '").append("% ").append(resourcesList.get(i)).append(" %'");
                    if(i != resourcesList.size() - 1) {
                        query.append(" OR ");
                    }
                }
                query.append(")");
            }
        }
        // order by and limit
        query.append("ORDER BY GenerationTime ASC, UniqueId ASC");

        String finalQuery = query.toString();
        List<ScheduledActivityData> result = new LinkedList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                while (rs.next()) {
                    try {
                        ScheduledActivityData object = mapToItem(rs, filter);
                        result.add(object);
                    } catch (IOException e) {
                        throw new SQLException(e);
                    }
                }
            } finally {
                connection.commit();
            }
        }
        return result;
    }


    @Override
    protected String getLastIdQuery() {
        return LAST_ID_QUERY;
    }

    @Override
    protected String getLastGenerationTimeQuery(Class<? extends AbstractDataItem> type) {
        return LAST_GENERATION_TIME_QUERY;
    }

    @Override
    protected List<String> getPurgeQuery(Instant referenceTime, RetrievalDirection direction) {
        return Collections.singletonList(
                "DELETE FROM SCHEDULED_ACTIVITY_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    protected String getRemoveQuery(IUniqueId id) {
        return "DELETE FROM SCHEDULED_ACTIVITY_DATA_TABLE WHERE UniqueId = " + id.asLong();
    }

    @Override
    protected String getRemoveQuery(ScheduledActivityDataFilter filter) {
        StringBuilder query = new StringBuilder("DELETE " +
                "FROM SCHEDULED_ACTIVITY_DATA_TABLE " +
                "WHERE TRUE "); // Use to simplify the cascade of conditions
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getActivityPathList() != null && !filter.getActivityPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getActivityPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
            if(filter.getSchedulingStateList() != null && !filter.getSchedulingStateList().isEmpty()) {
                query.append("AND State IN (").append(toEnumFilterListString(filter.getSchedulingStateList())).append(") ");
            }
            if(filter.getResourceList() != null && !filter.getResourceList().isEmpty()) {
                List<String> resourcesList = new ArrayList<>(filter.getResourceList());
                query.append("AND (");
                for(int i = 0; i < resourcesList.size(); ++i) {
                    query.append("Resources LIKE '").append("% ").append(resourcesList.get(i)).append(" %'");
                    if(i != resourcesList.size() - 1) {
                        query.append(" OR ");
                    }
                }
                query.append(")");
            }
        }
        return query.toString();
    }

    @Override
    public String toString() {
        return "Scheduled Activity Archive";
    }

    @Override
    protected Class<ScheduledActivityData> getMainType() {
        return ScheduledActivityData.class;
    }
}
