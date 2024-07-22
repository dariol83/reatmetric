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

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RawDataArchive extends AbstractDataItemArchive<RawData, RawDataFilter> implements IRawDataArchive {

    private static final Logger LOG = Logger.getLogger(RawDataArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO RAW_DATA_TABLE(UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Handler,Quality,RelatedItem,Contents,AdditionalData) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM RAW_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Handler,Quality,RelatedItem,Contents,AdditionalData FROM RAW_DATA_TABLE WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM RAW_DATA_TABLE";

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
        storeStatement.setString(6, item.getRoute() != null && item.getRoute().length() > 48 ? item.getRoute().substring(0, 48): item.getRoute());
        storeStatement.setString(7, item.getSource());
        storeStatement.setString(8, item.getHandler());
        storeStatement.setShort(9, (short) item.getQuality().ordinal());
        if(item.getRelatedItem() == null) {
            storeStatement.setNull(10, Types.BIGINT);
        } else {
            storeStatement.setLong(10, item.getRelatedItem().asLong());
        }
        if(item.isContentsSet()) {
            storeStatement.setBytes(11, item.getContents());
        } else {
            storeStatement.setBytes(11, null);
        }
        storeStatement.setBytes(12, toBytes(item.getExtension()));
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
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Handler,Quality,RelatedItem,Contents,AdditionalData");
        query.append(" FROM RAW_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, direction);
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
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, RawDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Handler,Quality,RelatedItem,Contents,AdditionalData");
        query.append(" FROM RAW_DATA_TABLE WHERE ");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
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
        if(ascending) {
            query.append("ORDER BY GenerationTime ASC, UniqueId ASC");
        } else {
            query.append("ORDER BY GenerationTime DESC, UniqueId DESC");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,Name,ReceptionTime,Type,Route,Source,Handler,Quality,RelatedItem,Contents,AdditionalData");
        query.append(" FROM RAW_DATA_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
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
        String handler = rs.getString(8);
        Quality quality = Quality.values()[rs.getShort(9)];
        Long relatedItemUniqueId = rs.getLong(10);
        if(rs.wasNull()) {
            relatedItemUniqueId = null;
        }
        // retrieve Contents if present
        byte[] contents = null;
        if(usedFilter == null || usedFilter.isWithData()) {
            contents = rs.getBytes(11);
        }
        byte[] extensionBlob = rs.getBytes(12);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new RawData(new LongUniqueId(uniqueId), toInstant(genTime), name, type, route, source, quality, relatedItemUniqueId != null ? new LongUniqueId(relatedItemUniqueId) : null, contents, toInstant(receptionTime), handler, extension);
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
                "DELETE FROM RAW_DATA_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
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
