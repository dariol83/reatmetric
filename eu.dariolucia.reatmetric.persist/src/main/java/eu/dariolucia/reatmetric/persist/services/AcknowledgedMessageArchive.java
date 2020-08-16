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

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcknowledgedMessageArchive extends AbstractDataItemArchive<AcknowledgedMessage, AcknowledgedMessageFilter> implements IAcknowledgedMessageArchive {

    private static final Logger LOG = Logger.getLogger(AcknowledgedMessageArchive.class.getName());

    private static final String INSERT_STATEMENT = "INSERT INTO ACK_MESSAGE_TABLE(UniqueId,GenerationTime,MessageId,State,UserName,AcknowledgementTime,AdditionalData) VALUES (?,?,?,?,?,?,?)";
    private static final String UPDATE_STATEMENT = "UPDATE ACK_MESSAGE_TABLE SET GenerationTime = ?, MessageId = ?, State = ?, UserName = ?, AcknowledgementTime = ?, AdditionalData = ? WHERE UniqueId = ?";
    private static final String STORE_STATEMENT = "MERGE INTO ACK_MESSAGE_TABLE USING SYSIBM.SYSDUMMY1 ON UniqueId = ? " +
            "WHEN MATCHED THEN UPDATE SET GenerationTime = ?, MessageId = ?, State = ?, UserName = ?, AcknowledgementTime = ?, AdditionalData = ? " +
            "WHEN NOT MATCHED THEN INSERT (UniqueId,GenerationTime,MessageId,State,UserName,AcknowledgementTime,AdditionalData) VALUES (?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM ACK_MESSAGE_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
            "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.AdditionalData " +
            "FROM ACK_MESSAGE_TABLE as a JOIN OPERATIONAL_MESSAGE_TABLE as b " +
            "ON (a.MessageId = b.UniqueId) " +
            "WHERE a.UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM ACK_MESSAGE_TABLE";

    public AcknowledgedMessageArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, AcknowledgedMessage item) throws SQLException {
        storeStatement.setLong(1, item.getInternalId().asLong());
        storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
        storeStatement.setLong(3, item.getMessage().getInternalId().asLong());
        storeStatement.setShort(4, (short) item.getState().ordinal());
        if(item.getUser() == null) {
            storeStatement.setNull(5, Types.VARCHAR);
        } else {
            storeStatement.setString(5, item.getUser());
        }
        storeStatement.setTimestamp(6, toTimestamp(item.getAcknowledgementTime()));
        Object extension = item.getExtension();
        if(extension == null) {
            storeStatement.setNull(7, Types.BLOB);
        } else {
            storeStatement.setBlob(7, toInputstream(item.getExtension()));
        }

        storeStatement.setLong(8, item.getInternalId().asLong());
        storeStatement.setTimestamp(9, toTimestamp(item.getGenerationTime()));
        storeStatement.setLong(10, item.getMessage().getInternalId().asLong());
        storeStatement.setShort(11, (short) item.getState().ordinal());
        if(item.getUser() == null) {
            storeStatement.setNull(12, Types.VARCHAR);
        } else {
            storeStatement.setString(12, item.getUser());
        }
        storeStatement.setTimestamp(13, toTimestamp(item.getAcknowledgementTime()));
        if(extension == null) {
            storeStatement.setNull(14, Types.BLOB);
        } else {
            storeStatement.setBlob(14, toInputstream(item.getExtension()));
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
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
                "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.AdditionalData " +
                "FROM ACK_MESSAGE_TABLE as a JOIN OPERATIONAL_MESSAGE_TABLE as b " +
                "ON (a.MessageId = b.UniqueId) " +
                "WHERE a.");
        // add time info
        addTimeInfo(query, startTime, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getUserList() != null && !filter.getUserList().isEmpty()) {
                query.append("AND a.UserName IN (").append(toFilterListString(filter.getUserList(), o -> o, "'")).append(") ");
            }
            if(filter.getStateList() != null && !filter.getStateList().isEmpty()) {
                query.append("AND a.State IN (").append(toEnumFilterListString(filter.getStateList())).append(") ");
            }
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY a.GenerationTime ASC, a.UniqueId ASC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        } else {
            query.append("ORDER BY a.GenerationTime DESC, a.UniqueId DESC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
                "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.AdditionalData " +
                "FROM ACK_MESSAGE_TABLE as a JOIN OPERATIONAL_MESSAGE_TABLE as b " +
                "ON (a.MessageId = b.UniqueId) " +
                "WHERE a.");
        // add time info
        addTimeInfo(query, startTime, internalId, direction);
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getUserList() != null && !filter.getUserList().isEmpty()) {
                query.append("AND a.UserName IN (").append(toFilterListString(filter.getUserList(), o -> o, "'")).append(") ");
            }
            if(filter.getStateList() != null && !filter.getStateList().isEmpty()) {
                query.append("AND a.State IN (").append(toEnumFilterListString(filter.getStateList())).append(") ");
            }
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY a.GenerationTime ASC, a.UniqueId ASC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        } else {
            query.append("ORDER BY a.GenerationTime DESC, a.UniqueId DESC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        }
        return query.toString();
    }

    @Override
    protected AcknowledgedMessage mapToItem(ResultSet rs, AcknowledgedMessageFilter usedFilder) throws SQLException, IOException {
        OperationalMessage om = OperationalMessageArchive.mapToItem(rs, null, 6);
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        AcknowledgementState state = AcknowledgementState.values()[rs.getShort(3)];
        String user = rs.getString(4);
        Timestamp ackTime = rs.getTimestamp(5);
        Blob extensionBlob = rs.getBlob(6);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new AcknowledgedMessage(new LongUniqueId(uniqueId), toInstant(genTime), om, state, toInstant(ackTime), user, extension);
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
                "DELETE FROM ACK_MESSAGE_TABLE WHERE GenerationTime " + (direction == RetrievalDirection.TO_FUTURE ? ">" : "<") + "'" + toTimestamp(referenceTime) + "'"
        );
    }

    @Override
    public String toString() {
        return "Acknowledgement Message Archive";
    }

    @Override
    protected Class<AcknowledgedMessage> getMainType() {
        return AcknowledgedMessage.class;
    }
}
