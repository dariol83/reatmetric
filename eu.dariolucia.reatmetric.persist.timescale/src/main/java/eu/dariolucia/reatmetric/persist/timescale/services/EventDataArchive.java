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

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventDataArchive extends AbstractDataItemArchive<EventData, EventDataFilter> implements IEventDataArchive {

    private static final Logger LOG = Logger.getLogger(EventDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO EVENT_DATA_TABLE(UniqueId,GenerationTime,ExternalId,Name,Path,Qualifier,ReceptionTime,Type,Route,Source,Severity,ContainerId,Report,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT MAX(UniqueId) FROM EVENT_DATA_TABLE";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ExternalId,Name,Path,Qualifier,ReceptionTime,Type,Route,Source,Severity,ContainerId,Report,AdditionalData FROM EVENT_DATA_TABLE WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM EVENT_DATA_TABLE";

    public EventDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, EventData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setInt(3, item.getExternalId());
        storeStatement.setString(4, item.getName());
        storeStatement.setString(5, item.getPath().asString());
        storeStatement.setString(6, item.getQualifier());
        storeStatement.setTimestamp(7, toTimestamp(item.getReceptionTime()));
        storeStatement.setString(8, item.getType());
        storeStatement.setString(9, item.getRoute() != null && item.getRoute().length() > 48 ? item.getRoute().substring(0, 48): item.getRoute());
        storeStatement.setString(10, item.getSource());
        storeStatement.setShort(11, (short) item.getSeverity().ordinal());
        if(item.getRawDataContainerId() == null) {
            storeStatement.setNull(12, Types.BIGINT);
        } else {
            storeStatement.setLong(12, item.getRawDataContainerId().asLong());
        }
        storeStatement.setBytes(13, toBytes(item.getReport()));
        storeStatement.setBytes(14, toBytes(item.getExtension()));
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, EventDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM EVENT_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getEventPathList() != null && !filter.getEventPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getEventPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
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
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, EventDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM EVENT_DATA_TABLE WHERE ");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getEventPathList() != null && !filter.getEventPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getEventPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
        }
        // order by and limit
        if(ascending) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, EventDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM EVENT_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getEventPathList() != null && !filter.getEventPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getEventPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
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
    protected EventData mapToItem(ResultSet rs, EventDataFilter usedFilter) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        int externalId = rs.getInt(3);
        String name = rs.getString(4);
        String path = rs.getString(5);
        String qualifier = rs.getString(6);
        Timestamp receptionTime = rs.getTimestamp(7);
        String type = rs.getString(8);
        String route = rs.getString(9);
        String source = rs.getString(10);
        Severity severity = Severity.values()[rs.getShort(11)];
        Long containerId = rs.getLong(12);
        if(rs.wasNull()) {
            containerId = null;
        }
        Object report = toObject(rs.getBytes(13));
        byte[] extensionData = rs.getBytes(14);
        Object extension = null;
        if(extensionData != null && !rs.wasNull()) {
            extension = toObject(extensionData);
        }
        return new EventData(new LongUniqueId(uniqueId), toInstant(genTime), externalId, name, SystemEntityPath.fromString(path), qualifier, type, route, source, severity, report, containerId == null ? null : new LongUniqueId(containerId), toInstant(receptionTime), extension);
    }

    @Override
    public synchronized List<EventData> retrieve(Instant time, EventDataFilter filter, Instant maxLookbackTime) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter, maxLookbackTime);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<EventData> doRetrieve(Connection connection, Instant time, EventDataFilter filter, Instant maxLookbackTime) throws SQLException {
        if(time.isBefore(MINIMUM_TIME)) {
            time = MINIMUM_TIME;
        } else if(time.isAfter(MAXIMUM_TIME)) {
            time = MAXIMUM_TIME;
        }
        StringBuilder query = new StringBuilder("SELECT EVENT_DATA_TABLE.* FROM (SELECT DISTINCT Path, MAX(GenerationTime) as LatestTime FROM EVENT_DATA_TABLE WHERE GenerationTime <= '");
        query.append(toTimestamp(time));
        query.append("' ");
        if(maxLookbackTime != null) {
            query.append(" AND GenerationTime >= '").append(toTimestamp(maxLookbackTime)).append("' ");
        }
        if(filter != null) {
            if (filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if (filter.getEventPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getEventPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
        }
        query.append(" GROUP BY Path) AS LATEST_SAMPLES INNER JOIN EVENT_DATA_TABLE ON EVENT_DATA_TABLE.Path = LATEST_SAMPLES.Path AND EVENT_DATA_TABLE.GenerationTime = LATEST_SAMPLES.LatestTime ");
        if(filter != null) {
            if (filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
            }
            if (filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if (filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if (filter.getTypeList() != null && !filter.getTypeList().isEmpty()) {
                query.append("AND Type IN (").append(toFilterListString(filter.getTypeList(), o -> o, "'")).append(") ");
            }
        }
        String finalQuery = query.toString();
        List<EventData> result = new LinkedList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                while (rs.next()) {
                    try {
                        EventData object = mapToItem(rs, filter);
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
        return Arrays.asList(
                "DELETE FROM EVENT_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Event Archive";
    }

    @Override
    protected Class<EventData> getMainType() {
        return EventData.class;
    }
}
