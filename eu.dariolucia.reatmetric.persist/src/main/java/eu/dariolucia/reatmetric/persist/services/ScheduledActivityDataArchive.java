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
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduledActivityDataArchive extends AbstractDataItemArchive<ScheduledActivityData, ScheduledActivityDataFilter> implements IScheduledActivityDataArchive {

    private static final Logger LOG = Logger.getLogger(ScheduledActivityDataArchive.class.getName());

    private static final String STORE_STATEMENT = "MERGE INTO SCHEDULED_ACTIVITY_DATA_TABLE USING SYSIBM.SYSDUMMY1 ON UniqueId = ? " +
            "WHEN MATCHED THEN UPDATE SET GenerationTime = ?, ActivityRequest = ?, Path = ?, ActivityOccurrence = ?, Resources = ?, Source = ?, ExternalId = ?, Trigger = ?, LatestInvocationTime = ?, ConflictStrategy = ?, State = ?, AdditionalData = ? " +
            "WHEN NOT MATCHED THEN INSERT (UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,ConflictStrategy,State,AdditionalData) VALUES (?,?,?, ?,?,?, ?,?,?, ?,?,?, ?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM SCHEDULED_ACTIVITY_DATA_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";
    private static final String RETRIEVE_BY_ID_QUERY = "SELECT UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,ConflictStrategy,State,AdditionalData " +
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
        storeStatement.setLong(8, item.getExternalId());
        storeStatement.setBlob(9, toInputstream(item.getTrigger()));
        if(item.getLatestInvocationTime() != null) {
            storeStatement.setTimestamp(10, toTimestamp(item.getLatestInvocationTime()));
        } else {
            storeStatement.setNull(10, Types.BLOB);
        }
        storeStatement.setShort(11, (short) item.getConflictStrategy().ordinal());
        storeStatement.setShort(12, (short) item.getState().ordinal());
        Object extension = item.getExtension();
        if(extension == null) {
            storeStatement.setNull(13, Types.BLOB);
        } else {
            storeStatement.setBlob(13, toInputstream(item.getExtension()));
        }


        storeStatement.setLong(14, item.getInternalId().asLong());
        storeStatement.setTimestamp(15, toTimestamp(item.getGenerationTime()));
        storeStatement.setBlob(16, toInputstream(item.getRequest()));
        storeStatement.setString(17, item.getRequest().getPath().asString());
        if(item.getActivityOccurrence() != null) {
            storeStatement.setLong(18, item.getActivityOccurrence().asLong());
        } else {
            storeStatement.setNull(18, Types.BIGINT);
        }
        storeStatement.setString(19, resources);
        storeStatement.setString(20, item.getSource());
        storeStatement.setLong(21, item.getExternalId());
        storeStatement.setBlob(22, toInputstream(item.getTrigger()));
        if(item.getLatestInvocationTime() != null) {
            storeStatement.setTimestamp(23, toTimestamp(item.getLatestInvocationTime()));
        } else {
            storeStatement.setNull(23, Types.BLOB);
        }
        storeStatement.setShort(24, (short) item.getConflictStrategy().ordinal());
        storeStatement.setShort(25, (short) item.getState().ordinal());
        if(extension == null) {
            storeStatement.setNull(26, Types.BLOB);
        } else {
            storeStatement.setBlob(26, toInputstream(item.getExtension()));
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
        StringBuilder query = new StringBuilder("SELECT UniqueId,GenerationTime,ActivityRequest,Path,ActivityOccurrence,Resources,Source,ExternalId,Trigger,LatestInvocationTime,ConflictStrategy,State,AdditionalData " +
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
                List<String> reses = new ArrayList<>(filter.getResourceList());
                query.append("AND (");
                for(int i = 0; i < reses.size(); ++i) {
                    query.append("Resources LIKE '").append("% ").append(reses.get(i)).append(" %'"); // TODO check if there is a contains function
                    if(i != reses.size() - 1) {
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
        long extId = rs.getLong(8);
        AbstractSchedulingTrigger trigger = (AbstractSchedulingTrigger) toObject(rs.getBlob(9));
        Timestamp latestInvocTime = rs.getTimestamp(10);
        if(rs.wasNull()) {
            latestInvocTime = null;
        }
        ConflictStrategy conflictStrategy = ConflictStrategy.values()[rs.getShort(11)];
        SchedulingState state = SchedulingState.values()[rs.getShort(12)];
        Blob extensionBlob = rs.getBlob(13);
        Object extension = null;
        if(extensionBlob != null && !rs.wasNull()) {
            extension = toObject(extensionBlob);
        }
        return new ScheduledActivityData(new LongUniqueId(uniqueId), toInstant(genTime), request,
                actOcc == null ? null : new LongUniqueId(actOcc), resources, source, extId, trigger, toInstant(latestInvocTime), conflictStrategy, state, extension);
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
                "WHERE TRUE "); // TODO check if it works
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
                List<String> reses = new ArrayList<>(filter.getResourceList());
                query.append("AND (");
                for(int i = 0; i < reses.size(); ++i) {
                    query.append("Resources LIKE '").append("% ").append(reses.get(i)).append(" %'"); // TODO check if there is a contains function
                    if(i != reses.size() - 1) {
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
