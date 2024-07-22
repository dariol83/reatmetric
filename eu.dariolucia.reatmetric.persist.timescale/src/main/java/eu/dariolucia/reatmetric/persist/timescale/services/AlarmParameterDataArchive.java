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

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
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

public class AlarmParameterDataArchive extends AbstractDataItemArchive<AlarmParameterData, AlarmParameterDataFilter> implements IAlarmParameterDataArchive {

    private static final Logger LOG = Logger.getLogger(AlarmParameterDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO ALARM_PARAMETER_DATA_TABLE(UniqueId,GenerationTime,ExternalId,Name,Path,CurrentAlarmState,CurrentValue,ReceptionTime,LastNominalValue,LastNominalValueTime,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM ALARM_PARAMETER_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ExternalId,Name,Path,CurrentAlarmState,CurrentValue,ReceptionTime,LastNominalValue,LastNominalValueTime,AdditionalData FROM ALARM_PARAMETER_DATA_TABLE WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM ALARM_PARAMETER_DATA_TABLE";

    public AlarmParameterDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, AlarmParameterData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setInt(3, item.getExternalId());
        storeStatement.setString(4, item.getName());
        storeStatement.setString(5, item.getPath().asString());
        storeStatement.setShort(6, (short) item.getCurrentAlarmState().ordinal());
        storeStatement.setBytes(7, toBytes(item.getCurrentValue()));
        storeStatement.setTimestamp(8, toTimestamp(item.getReceptionTime()));
        storeStatement.setBytes(9, toBytes(item.getLastNominalValue()));
        storeStatement.setTimestamp(10, toTimestamp(item.getLastNominalValueTime()));
        storeStatement.setBytes(11, toBytes(item.getExtension()));
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM ALARM_PARAMETER_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND CurrentAlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
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
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, AlarmParameterDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM ALARM_PARAMETER_DATA_TABLE WHERE ");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND CurrentAlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
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
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM ALARM_PARAMETER_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND CurrentAlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
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
    protected AlarmParameterData mapToItem(ResultSet rs, AlarmParameterDataFilter usedFilter) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        int externalId = rs.getInt(3);
        String name = rs.getString(4);
        String path = rs.getString(5);
        AlarmState currentAlarmState = AlarmState.values()[rs.getShort(6)];
        Object currentValue = toObject(rs.getBytes(7));
        Timestamp receptionTime = rs.getTimestamp(8);
        Object lastNominalValue = toObject(rs.getBytes(9));
        Timestamp lastNominalValueTime = rs.getTimestamp(10);
        byte[] extensionBlob = rs.getBytes(11);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new AlarmParameterData(new LongUniqueId(uniqueId), toInstant(genTime), externalId, name, SystemEntityPath.fromString(path), currentAlarmState, currentValue, lastNominalValue, toInstant(lastNominalValueTime), toInstant(receptionTime), extension);
    }

    @Override
    public synchronized List<AlarmParameterData> retrieve(Instant time, AlarmParameterDataFilter filter, Instant maxLookbackTime) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter, maxLookbackTime);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<AlarmParameterData> doRetrieve(Connection connection, Instant time, AlarmParameterDataFilter filter, Instant maxLookbackTime) throws SQLException {
        if(time.isBefore(MINIMUM_TIME)) {
            time = MINIMUM_TIME;
        } else if(time.isAfter(MAXIMUM_TIME)) {
            time = MAXIMUM_TIME;
        }
        StringBuilder query = new StringBuilder("SELECT ALARM_PARAMETER_DATA_TABLE.* FROM (SELECT DISTINCT Path, MAX(GenerationTime) as LatestTime FROM ALARM_PARAMETER_DATA_TABLE WHERE GenerationTime <= '");
        query.append(toTimestamp(time));
        query.append("' ");
        if(maxLookbackTime != null) {
            query.append(" AND GenerationTime >= '").append(toTimestamp(maxLookbackTime)).append("' ");
        }
        if(filter != null) {
            if (filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if (filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
        }
        query.append(" GROUP BY Path) AS LATEST_SAMPLES INNER JOIN ALARM_PARAMETER_DATA_TABLE ON ALARM_PARAMETER_DATA_TABLE.Path = LATEST_SAMPLES.Path AND ALARM_PARAMETER_DATA_TABLE.GenerationTime = LATEST_SAMPLES.LatestTime ");
        if(filter != null) {
            if (filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND CurrentAlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
            }
        }
        String finalQuery = query.toString();
        List<AlarmParameterData> result = new LinkedList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                while (rs.next()) {
                    try {
                        AlarmParameterData object = mapToItem(rs, filter);
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
                "DELETE FROM ALARM_PARAMETER_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Alarm Parameter Archive";
    }

    @Override
    protected Class<AlarmParameterData> getMainType() {
        return AlarmParameterData.class;
    }
}
