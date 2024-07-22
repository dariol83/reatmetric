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
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcknowledgedMessageArchive extends AbstractDataItemArchive<AcknowledgedMessage, AcknowledgedMessageFilter> implements IAcknowledgedMessageArchive {

    private static final Logger LOG = Logger.getLogger(AcknowledgedMessageArchive.class.getName());

    private static final String INSERT_STATEMENT = "INSERT INTO ACK_MESSAGE_TABLE(UniqueId,GenerationTime,MessageId,State,UserName,AcknowledgementTime,AdditionalData) VALUES (?,?,?,?,?,?,?)";
    private static final String UPDATE_STATEMENT = "UPDATE ACK_MESSAGE_TABLE SET GenerationTime = ?, MessageId = ?, State = ?, UserName = ?, AcknowledgementTime = ?, AdditionalData = ? WHERE UniqueId = ?";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM ACK_MESSAGE_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
            "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.LinkedEntityId, b.AdditionalData " +
            "FROM ACK_MESSAGE_TABLE as a JOIN OPERATIONAL_MESSAGE_TABLE as b " +
            "ON (a.MessageId = b.UniqueId) " +
            "WHERE a.UniqueId=?";
    private static final String LAST_GENERATION_TIME_QUERY = "SELECT MAX(GenerationTime) FROM ACK_MESSAGE_TABLE";

    public AcknowledgedMessageArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void setItemPropertiesToStatement(PreparedStatement storeStatement, AcknowledgedMessage item) {
        throw new UnsupportedOperationException("This operation should not be called for this class implementation. This is a software bug.");
    }

    @Override
    protected PreparedStatement createStoreStatement(Connection connection) {
        throw new UnsupportedOperationException("This operation should not be called for this class implementation. This is a software bug.");
    }

    @Override
    protected void doStore(Connection connection, List<AcknowledgedMessage> itemsToStore) throws SQLException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - request to store " + itemsToStore.size() + " items");
            if (LOG.isLoggable(Level.FINEST)) {
                for (AcknowledgedMessage item : itemsToStore) {
                    LOG.finest("Storing " + item);
                }
            }
        }

        // For each item, try to insert it if the ack state is pending: in case of issues, or if ack state is not pending, run an update
        for(AcknowledgedMessage item : itemsToStore) {
            boolean inserted = false;
            if(item.getState() == AcknowledgementState.PENDING) {
                // Run INSERT
                inserted = tryInsert(connection, item);
            }
            if(!inserted) {
                // Run UPDATE
                tryUpdate(connection, item);
            }
        }
    }

    private void tryUpdate(Connection connection, AcknowledgedMessage item) throws SQLException {
        try (PreparedStatement storeStatement = connection.prepareStatement(UPDATE_STATEMENT)) {
            storeStatement.setTimestamp(1, toTimestamp(item.getGenerationTime()));
            storeStatement.setLong(2, item.getMessage().getInternalId().asLong());
            storeStatement.setShort(3, (short) item.getState().ordinal());
            if(item.getUser() == null) {
                storeStatement.setNull(4, Types.VARCHAR);
            } else {
                storeStatement.setString(4, item.getUser());
            }
            storeStatement.setTimestamp(5, toTimestamp(item.getAcknowledgementTime()));
            storeStatement.setBytes(6, toBytes(item.getExtension()));
            storeStatement.setLong(7, item.getInternalId().asLong());
            storeStatement.addBatch();
            int[] numUpdates = storeStatement.executeBatch();
            if (LOG.isLoggable(Level.FINEST)) {
                for (int i = 0; i < numUpdates.length; i++) {
                    if (numUpdates[i] == -2) {
                        LOG.finest("Batch job[" + i +
                                "]: unknown number of rows updated");
                    } else {
                        LOG.finest("Batch job[" + i +
                                "]: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
        }
    }

    private boolean tryInsert(Connection connection, AcknowledgedMessage item) {
        boolean inserted = false;
        try (PreparedStatement storeStatement = connection.prepareStatement(INSERT_STATEMENT)) {
            storeStatement.setLong(1, item.getInternalId().asLong());
            storeStatement.setTimestamp(2, toTimestamp(item.getGenerationTime()));
            storeStatement.setLong(3, item.getMessage().getInternalId().asLong());
            storeStatement.setShort(4, (short) item.getState().ordinal());
            if (item.getUser() == null) {
                storeStatement.setNull(5, Types.VARCHAR);
            } else {
                storeStatement.setString(5, item.getUser());
            }
            storeStatement.setTimestamp(6, toTimestamp(item.getAcknowledgementTime()));
            storeStatement.setBytes(7, toBytes(item.getExtension()));
            storeStatement.addBatch();
            int[] numUpdates = storeStatement.executeBatch();
            if (LOG.isLoggable(Level.FINEST)) {
                for (int i = 0; i < numUpdates.length; i++) {
                    if (numUpdates[i] == -2) {
                        LOG.finest("Batch job[" + i +
                                "]: unknown number of rows updated");
                    } else {
                        LOG.finest("Batch job[" + i +
                                "]: " + numUpdates[i] + " rows updated");
                    }
                }
            }
            connection.commit();
            inserted = true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return inserted;
    }

    @Override
    protected String buildRetrieveByIdQuery() {
        return RETRIEVE_BY_ID_QUERY;
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
                "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.LinkedEntityId, b.AdditionalData " +
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
    protected String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, AcknowledgedMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
                "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.LinkedEntityId, b.AdditionalData " +
                "FROM ACK_MESSAGE_TABLE as a JOIN OPERATIONAL_MESSAGE_TABLE as b " +
                "ON (a.MessageId = b.UniqueId) " +
                "WHERE a.");
        // add time info
        addTimeRangeInfo(query, startTime, endTime, ascending);
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
        if(ascending) {
            query.append("ORDER BY a.GenerationTime ASC, a.UniqueId ASC");
        } else {
            query.append("ORDER BY a.GenerationTime DESC, a.UniqueId DESC");
        }
        return query.toString();
    }

    @Override
    protected String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) {
        StringBuilder query = new StringBuilder("SELECT a.UniqueId, a.GenerationTime, a.State, a.UserName, a.AcknowledgementTime, a.AdditionalData, " +
                "b.UniqueId, b.GenerationTime, b.Id, b.Text, b.Source, b.Severity, b.LinkedEntityId, b.AdditionalData " +
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
        OperationalMessage om = OperationalMessageArchive.mapToItem(rs, 6);
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        AcknowledgementState state = AcknowledgementState.values()[rs.getShort(3)];
        String user = rs.getString(4);
        Timestamp ackTime = rs.getTimestamp(5);
        byte[] extensionBlob = rs.getBytes(6);
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
        return List.of(
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
