package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RawDataArchive extends AbstractDataItemArchive<RawData, RawDataFilter> implements IRawDataArchive {

    private static final Logger LOG = Logger.getLogger(RawDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO RAW_DATA_TABLE(UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Quality,RelatedItem,Contents,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM RAW_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Quality,RelatedItem,Contents,AdditionalData FROM RAW_DATA_TABLE WHERE UniqueId=?";


    public RawDataArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, RawData item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setString(3, item.getName());
        storeStatement.setTimestamp(4, toTimestamp(item.getReceptionTime()));
        storeStatement.setString(5, item.getType());
        storeStatement.setString(6, item.getRoute());
        storeStatement.setString(7, item.getSource());
        storeStatement.setShort(8, (short) item.getQuality().ordinal());
        if(item.getRelatedItem() == null) {
            storeStatement.setNull(9, Types.BIGINT);
        } else {
            storeStatement.setLong(9, item.getRelatedItem().asLong());
        }
        if(item.isContentsSet()) {
            storeStatement.setBlob(10, new ByteArrayInputStream(item.getContents()));
        } else {
            storeStatement.setNull(10, Types.BLOB);
        }
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Quality,RelatedItem,AdditionalData");
        if(filter == null || filter.isWithData()) {
            query.append(",Contents");
        }
        query.append(" FROM RAW_DATA_TABLE WHERE ");
        // add time info
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("GenerationTime >= '").append(toTimestamp(startTime).toString()).append("' ");
        } else {
            query.append("GenerationTime <= '").append(toTimestamp(startTime).toString()).append("' ");
        }
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getNameContains() != null) {
                query.append("AND Name LIKE '%").append(filter.getNameContains()).append("%' ");
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
            if(filter.getQualityList() != null && !filter.getQualityList().isEmpty()) {
                query.append("AND Quality IN (").append(toEnumFilterListString(filter.getQualityList())).append(") ");
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
    protected RawData mapToItem(ResultSet rs, RawDataFilter usedFilter) throws SQLException, IOException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        String name = rs.getString(3);
        Timestamp receptionTime = rs.getTimestamp(4);
        String type = rs.getString(5);
        String route = rs.getString(6);
        String source = rs.getString(7);
        Quality quality = Quality.values()[rs.getShort(8)];
        Long relatedItemUniqueId = rs.getLong(9);
        if(rs.wasNull()) {
            relatedItemUniqueId = null;
        }
        Object[] additionalDataArray = toObjectArray(rs.getBlob(10));
        // retrieve Contents if present
        byte[] contents = null;
        if(usedFilter == null || usedFilter.isWithData()) {
            Blob blob = rs.getBlob(11);
            if(blob != null) {
                contents = toByteArray(blob.getBinaryStream());
            }
        }
        return new RawData(new LongUniqueId(uniqueId), toInstant(genTime), name, type, route, source, quality, relatedItemUniqueId != null ? new LongUniqueId(relatedItemUniqueId) : null, contents, toInstant(receptionTime), additionalDataArray);
    }

    @Override
    protected String getLastIdQuery() {
        return LAST_ID_QUERY;
    }

    @Override
    public String toString() {
        return "Raw Data Archive";
    }

    @Override
    protected Class<RawData> getMainType() {
        return RawData.class;
    }
}
