package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OperationalMessageArchive extends AbstractDataItemArchive<OperationalMessage, OperationalMessageFilter> implements IOperationalMessageArchive {

    private static final String STORE_STATEMENT = "INSERT INTO OPERATIONAL_MESSAGE_TABLE(UniqueId,GenerationTime,MessageId,MessageText,MessageSource,MessageSeverity,AdditionalData) VALUES (?,?,?,?,?,?,?)";
    private static final String LAST_ID_QUERY = "SELECT UniqueId FROM OPERATIONAL_MESSAGE_TABLE ORDER BY UniqueId DESC FETCH FIRST ROW ONLY";

    public OperationalMessageArchive(Archive controller) throws SQLException {
        super(controller);
    }

    @Override
    protected void doStore(Connection connection, List<OperationalMessage> itemsToStore) throws SQLException, IOException {
        System.out.println("Execute: " + STORE_STATEMENT);
        try (PreparedStatement prepStmt = connection.prepareStatement(STORE_STATEMENT)) {
            Iterator<OperationalMessage> it = itemsToStore.iterator();
            while (it.hasNext()) {
                OperationalMessage p = it.next();
                prepStmt.setLong(1, p.getInternalId().asLong());
                prepStmt.setTimestamp(2, toTimestamp(p.getGenerationTime()));
                prepStmt.setString(3, p.getId());
                prepStmt.setString(4, p.getMessage());
                prepStmt.setString(5, p.getSource());
                prepStmt.setShort(6, (short) p.getSeverity().ordinal());
                prepStmt.setBlob(7, toInputstream(p.getAdditionalFields()));
                prepStmt.addBatch();
            }
            int[] numUpdates = prepStmt.executeBatch();
            for (int i = 0; i < numUpdates.length; i++) {
                if (numUpdates[i] == -2) {
                    System.out.println("Execution " + i +
                            ": unknown number of rows updated");
                } else {
                    System.out.println("Execution " + i +
                            "successful: " + numUpdates[i] + " rows updated");
                }
            }
        }
    }

    @Override
    protected List<OperationalMessage> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM OPERATIONAL_MESSAGE_TABLE WHERE ");
        // add time info
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("GenerationTime >= '").append(toTimestamp(startTime).toString()).append("' ");
        } else {
            query.append("GenerationTime <= '").append(toTimestamp(startTime).toString()).append("' ");
        }
        // process filter
        if(filter != null && !filter.isClear()) {
            if(filter.getMessageRegExp() != null) {
                query.append("AND MessageText LIKE %").append(filter.getMessageRegExp()).append("% ");
            }
            if(filter.getSourceList() != null && !filter.getSourceList().isEmpty()) {
                query.append("AND MessageSource IN (").append(toFilterListString(filter.getSourceList(), o -> o)).append(") ");
            }
            if(filter.getSeverityList() != null && !filter.getSeverityList().isEmpty()) {
                query.append("AND MessageSeverity IN (").append(toEnumFilterListString(filter.getSeverityList())).append(") ");
            }
        }
        // order by and limit
        if(direction == RetrievalDirection.TO_FUTURE) {
            query.append("ORDER BY GenerationTime ASC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        } else {
            query.append("ORDER BY GenerationTime DESC FETCH NEXT ").append(numRecords).append(" ROWS ONLY");
        }

        List<OperationalMessage> result = new ArrayList<>(numRecords);
        try (Statement prepStmt = connection.createStatement()) {
            System.out.println("Query: " + query.toString());
            try (ResultSet rs = prepStmt.executeQuery(query.toString())) {
                while (rs.next()) {
                    try {
                        OperationalMessage object = map(rs);
                        result.add(object);
                    } catch (IOException | ClassNotFoundException e) {
                        throw new SQLException(e);
                    }
                }
            }
        }
        return result;
    }

    private OperationalMessage map(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        long uniqueId = rs.getLong(1);
        Timestamp genTime = rs.getTimestamp(2);
        String messageId = rs.getString(3);
        String messageText = rs.getString(4);
        String messageSource = rs.getString(5);
        Severity severity = Severity.values()[rs.getShort(6)];
        Blob additionalData = rs.getBlob(7);
        Object[] additionalDataArray = null;
        if(additionalData != null) {
            ObjectInputStream ois = new ObjectInputStream(additionalData.getBinaryStream());
            additionalDataArray = (Object[]) ois.readObject();
        }

        return new OperationalMessage(new LongUniqueId(uniqueId), messageId, messageText, toInstant(genTime), messageSource, severity, additionalDataArray);
    }

    @Override
    protected List<OperationalMessage> doRetrieve(Connection connection, OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws SQLException {
        // Use the startItem generationTime to retrieve all the items from that point in time: increase limit by 100
        List<OperationalMessage> largeSize = doRetrieve(connection, startItem.getGenerationTime(), numRecords + LOOK_AHEAD_SPAN, direction, filter);
        // Now scan and get rid of the startItem object
        int position = largeSize.indexOf(startItem);
        if(position == -1) {
            return largeSize.subList(0, Math.min(numRecords, largeSize.size()));
        } else {
            return largeSize.subList(position + 1, position + 1 + Math.min(numRecords, largeSize.size() - position - 1));
        }
    }

    @Override
    protected IUniqueId doRetrieveLastId(Connection connection) throws SQLException {
        try (Statement prepStmt = connection.createStatement()) {
            try (ResultSet rs = prepStmt.executeQuery(LAST_ID_QUERY)) {
                while (rs.next()) {
                    return new LongUniqueId(rs.getLong(1));
                }
            }
        }
        throw new SQLException("Cannot retrieve last ID for OperationalMessage objects");
    }
}
