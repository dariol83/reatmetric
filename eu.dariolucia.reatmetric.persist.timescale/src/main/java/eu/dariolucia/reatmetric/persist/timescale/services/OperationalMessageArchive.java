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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OperationalMessageArchive extends AbstractDataItemArchive<OperationalMessage, OperationalMessageFilter> implements IOperationalMessageArchive {

    private static final Logger LOG = Logger.getLogger(OperationalMessageArchive.class.getName());

    private static final String STORE_STATEMENT = "INSERT INTO OPERATIONAL_MESSAGE_TABLE(UniqueId,GenerationTime,Id,Text,Source,Severity,LinkedEntityId,AdditionalData) VALUES (?,?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM OPERATIONAL_MESSAGE_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,Id,Text,Source,Severity,LinkedEntityId,AdditionalData FROM OPERATIONAL_MESSAGE_TABLE WHERE UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM OPERATIONAL_MESSAGE_TABLE";

    public OperationalMessageArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, OperationalMessage item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setString(3, item.getId());
        storeStatement.setString(4, item.getMessage().length() > 255 ? item.getMessage().substring(0,255) : item.getMessage());
        storeStatement.setString(5, item.getSource());
        storeStatement.setShort(6, (short) item.getSeverity().ordinal());
        if(item.getLinkedEntityId() == null) {
            storeStatement.setNull(7, Types.INTEGER);
        } else {
            storeStatement.setInt(7, item.getLinkedEntityId());
        }
        storeStatement.setBytes(8, toBytes(item.getExtension()));
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM OPERATIONAL_MESSAGE_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getMessageTextContains() != null) {
                query.append("AND Text LIKE '%").append(filter.getMessageTextContains()).append("%' ");
            }
            if(filter.getIdList() != null && !filter.getIdList().isEmpty()) {
                query.append("AND Id IN (").append(toFilterListString(filter.getIdList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
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
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, OperationalMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM OPERATIONAL_MESSAGE_TABLE WHERE ");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getMessageTextContains() != null) {
                query.append("AND Text LIKE '%").append(filter.getMessageTextContains()).append("%' ");
            }
            if(filter.getIdList() != null && !filter.getIdList().isEmpty()) {
                query.append("AND Id IN (").append(toFilterListString(filter.getIdList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
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
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT * FROM OPERATIONAL_MESSAGE_TABLE WHERE ");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getMessageTextContains() != null) {
                query.append("AND Text LIKE '%").append(filter.getMessageTextContains()).append("%' ");
            }
            if(filter.getIdList() != null && !filter.getIdList().isEmpty()) {
                query.append("AND Id IN (").append(toFilterListString(filter.getIdList(), o -> o, "'")).append(") ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND Source IN (").append(toFilterListString(filter.getSourceList(), o -> o, "'")).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND Severity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
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
    protected OperationalMessage mapToItem(ResultSet rs, OperationalMessageFilter usedFilter) throws SQLException {
        return mapToItem(rs, 0);
    }

    static OperationalMessage mapToItem(ResultSet rs, int offset) throws SQLException {
        long uniqueId = rs.getLong(offset + 1);
        Timestamp genTime = rs.getTimestamp(offset + 2);
        String messageId = rs.getString(offset + 3);
        String messageText = rs.getString(offset + 4);
        String messageSource = rs.getString(offset + 5);
        Severity severity = Severity.values()[rs.getShort(offset + 6)];
        Integer linkedEntityId = rs.getInt(offset + 7);
        if(rs.wasNull()) {
            linkedEntityId = null;
        }
        byte[] extensionBlob = rs.getBytes(offset + 8);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new OperationalMessage(new LongUniqueId(uniqueId), toInstant(genTime), messageId, messageText, messageSource, severity, linkedEntityId, extension);
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
                "DELETE FROM OPERATIONAL_MESSAGE_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Message Archive";
    }

    @Override
    protected Class<OperationalMessage> getMainType() {
        return OperationalMessage.class;
    }
}
