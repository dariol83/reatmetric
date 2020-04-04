package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParameterDataArchive extends AbstractDataItemArchive<ParameterData, ParameterDataFilter> implements IParameterDataArchive {

    private static final Logger LOG = Logger.getLogger(ParameterDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO PARAMETER_DATA_TABLE(UniqueId,GenerationTime,ExternalId,Name,Path,EngValue,SourceValue,ReceptionTime,Route,Validity,AlarmState,ContainerId,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT MAX(UniqueId) FROM PARAMETER_DATA_TABLE";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ExternalId,Name,Path,EngValue,SourceValue,ReceptionTime,Route,Validity,AlarmState,ContainerId,AdditionalData FROM PARAMETER_DATA_TABLE WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM PARAMETER_DATA_TABLE";

    public ParameterDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, ParameterData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setInt(3, item.getExternalId());
        storeStatement.setString(4, item.getName());
        storeStatement.setString(5, item.getPath().asString());
        if(item.getEngValue() == null) {
            storeStatement.setNull(6, Types.BLOB);
        } else {
            storeStatement.setBlob(6, toInputstream(item.getEngValue()));
        }
        if(item.getSourceValue() == null) {
            storeStatement.setNull(7, Types.BLOB);
        } else {
            storeStatement.setBlob(7, toInputstream(item.getSourceValue()));
        }
        storeStatement.setTimestamp(8, toTimestamp(item.getReceptionTime()));
        if(item.getRoute() == null) {
            storeStatement.setNull(9, Types.VARCHAR);
        } else {
            storeStatement.setString(9, item.getRoute());
        }
        storeStatement.setShort(10, (short) item.getValidity().ordinal());
        storeStatement.setShort(11, (short) item.getAlarmState().ordinal());
        if(item.getRawDataContainerId() == null) {
            storeStatement.setNull(12, Types.BIGINT);
        } else {
            storeStatement.setLong(12, item.getRawDataContainerId().asLong());
        }
        Object extension = item.getExtension();
        if(extension == null) {
            storeStatement.setNull(13, Types.BLOB);
        } else {
            storeStatement.setBlob(13, toInputstream(item.getExtension()));
        }
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM PARAMETER_DATA_TABLE WHERE ");
        // add time info
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("GenerationTime >= '").append(toTimestamp(startTime).toString()).append("' ");
        } else {
            query.append("GenerationTime <= '").append(toTimestamp(startTime).toString()).append("' ");
        }
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if(filter.getParameterPathList() != null && !filter.getParameterPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getValidityList() != null && !filter.getValidityList().isEmpty()) {
                query.append("AND Validity IN (").append(toEnumFilterListString(filter.getValidityList())).append(") ");
            }
            if(filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND AlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
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
    protected ParameterData mapToItem(ResultSet rs, ParameterDataFilter usedFilter) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        int externalId = rs.getInt(3);
        String name = rs.getString(4);
        String path = rs.getString(5);
        Object engValue = toObject(rs.getBlob(6));
        Object sourceValue = toObject(rs.getBlob(7));
        Timestamp receptionTime = rs.getTimestamp(8);
        String route = rs.getString(9);
        Validity validity = Validity.values()[rs.getShort(10)];
        AlarmState alarmState = AlarmState.values()[rs.getShort(11)];
        Long containerId = rs.getLong(12);
        if(rs.wasNull()) {
            containerId = null;
        }
        Blob extensionBlob = rs.getBlob(13);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new ParameterData(new LongUniqueId(uniqueId), toInstant(genTime), externalId, name, SystemEntityPath.fromString(path), engValue, sourceValue, route, validity, alarmState, containerId == null ? null : new LongUniqueId(containerId), toInstant(receptionTime), extension);
    }

    @Override
    public synchronized List<ParameterData> retrieve(Instant time, ParameterDataFilter filter, Instant maxLookbackTime) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter, maxLookbackTime);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<ParameterData> doRetrieve(Connection connection, Instant time, ParameterDataFilter filter, Instant maxLookbackTime) throws SQLException {
        if(time.isBefore(MINIMUM_TIME)) {
            time = MINIMUM_TIME;
        } else if(time.isAfter(MAXIMUM_TIME)) {
            time = MAXIMUM_TIME;
        }
        StringBuilder query = new StringBuilder("SELECT PARAMETER_DATA_TABLE.* FROM (SELECT DISTINCT Path, MAX(GenerationTime) as LatestTime FROM PARAMETER_DATA_TABLE WHERE GenerationTime <= '");
        query.append(toTimestamp(time));
        query.append("' ");
        if(maxLookbackTime != null) {
            query.append(" AND GenerationTime >= '").append(toTimestamp(maxLookbackTime)).append("' ");
        }
        if(filter != null) {
            if (filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if (filter.getParameterPathList() != null && !filter.getParameterPathList().isEmpty()) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if (filter.getRouteList() != null && !filter.getRouteList().isEmpty()) {
                query.append("AND Route IN (").append(toFilterListString(filter.getRouteList(), o -> o, "'")).append(") ");
            }
            if(filter.getExternalIdList() != null && !filter.getExternalIdList().isEmpty()) {
                query.append("AND ExternalId IN (").append(toFilterListString(filter.getExternalIdList(), o -> o, null)).append(") ");
            }
        }
        query.append(" GROUP BY Path) AS LATEST_SAMPLES INNER JOIN PARAMETER_DATA_TABLE ON PARAMETER_DATA_TABLE.Path = LATEST_SAMPLES.Path AND PARAMETER_DATA_TABLE.GenerationTime = LATEST_SAMPLES.LatestTime ");
        if(filter != null) {
            if (filter.getValidityList() != null && !filter.getValidityList().isEmpty()) {
                query.append("AND Validity IN (").append(toEnumFilterListString(filter.getValidityList())).append(") ");
            }
            if (filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND AlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
            }
        }
        String finalQuery = query.toString();
        List<ParameterData> result = new LinkedList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                while (rs.next()) {
                    try {
                        ParameterData object = mapToItem(rs, filter);
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
                "DELETE FROM PARAMETER_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Parameter Data Archive";
    }

    @Override
    protected Class<ParameterData> getMainType() {
        return ParameterData.class;
    }
}
