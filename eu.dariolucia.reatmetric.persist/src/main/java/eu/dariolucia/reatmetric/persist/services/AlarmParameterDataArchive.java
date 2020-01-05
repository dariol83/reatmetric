package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlarmParameterDataArchive extends AbstractDataItemArchive<AlarmParameterData, AlarmParameterDataFilter> implements IAlarmParameterDataArchive {

    private static final Logger LOG = Logger.getLogger(AlarmParameterDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO ALARM_PARAMETER_DATA_TABLE(UniqueId,GenerationTime,ExternalId,Name,Path,CurrentAlarmState,CurrentValue,ReceptionTime,LastNominalValue,LastNominalValueTime,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM ALARM_PARAMETER_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ExternalId,Name,Path,CurrentAlarmState,CurrentValue,ReceptionTime,LastNominalValue,LastNominalValueTime,AdditionalData FROM ALARM_PARAMETER_DATA_TABLE WHERE UniqueId=?";

    public AlarmParameterDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, AlarmParameterData item) throws SQLException, IOException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setInt(3, item.getExternalId());
        storeStatement.setString(4, item.getName());
        storeStatement.setString(5, item.getPath().asString());
        storeStatement.setShort(6, (short) item.getCurrentAlarmState().ordinal());
        storeStatement.setBlob(7, toInputstream(item.getCurrentValue()));
        storeStatement.setTimestamp(8, toTimestamp(item.getReceptionTime()));
        storeStatement.setBlob(9, toInputstream(item.getLastNominalValue()));
        storeStatement.setTimestamp(10, toTimestamp(item.getLastNominalValueTime()));
        storeStatement.setBlob(11, toInputstreamArray(item.getAdditionalFields()));
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
            if(filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
            }
            if(filter.getAlarmStateList() != null && !filter.getAlarmStateList().isEmpty()) {
                query.append("AND CurrentAlarmState IN (").append(toEnumFilterListString(filter.getAlarmStateList())).append(") ");
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
    protected AlarmParameterData mapToItem(ResultSet rs, AlarmParameterDataFilter usedFilter) throws SQLException, IOException, ClassNotFoundException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        int externalId = rs.getInt(3);
        String name = rs.getString(4);
        String path = rs.getString(5);
        AlarmState currentAlarmState = AlarmState.values()[rs.getShort(6)];
        Object currentValue = toObject(rs.getBlob(7));
        Timestamp receptionTime = rs.getTimestamp(8);
        Object lastNominalValue = toObject(rs.getBlob(9));
        Timestamp lastNominalValueTime = rs.getTimestamp(10);
        Object[] additionalDataArray = toObjectArray(rs.getBlob(11));

        return new AlarmParameterData(new LongUniqueId(uniqueId), toInstant(genTime), externalId, name, SystemEntityPath.fromString(path), currentAlarmState, currentValue, lastNominalValue, toInstant(lastNominalValueTime), toInstant(receptionTime), additionalDataArray);
    }

    @Override
    public synchronized List<AlarmParameterData> retrieve(Instant time, AlarmParameterDataFilter filter) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, time, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    private List<AlarmParameterData> doRetrieve(Connection connection, Instant time, AlarmParameterDataFilter filter) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT ALARM_PARAMETER_DATA_TABLE.* FROM (SELECT DISTINCT Path, MAX(GenerationTime) as LatestTime FROM ALARM_PARAMETER_DATA_TABLE WHERE GenerationTime <= '");
        query.append(toTimestamp(time));
        query.append("' ");
        if(filter != null) {
            if (filter.getParentPath() != null) {
                query.append("AND Path LIKE '").append(filter.getParentPath().asString()).append("%' ");
            }
            if (filter.getParameterPathList() != null) {
                query.append("AND Path IN (").append(toFilterListString(filter.getParameterPathList(), SystemEntityPath::asString, "'")).append(") ");
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
                    } catch (IOException | ClassNotFoundException e) {
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
    public String toString() {
        return "Alarm Parameter Data Archive";
    }

    @Override
    protected Class<AlarmParameterData> getMainType() {
        return AlarmParameterData.class;
    }
}
