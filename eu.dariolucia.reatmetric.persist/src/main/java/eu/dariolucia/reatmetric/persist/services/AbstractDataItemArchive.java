package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter> {

    private static final Logger LOG = Logger.getLogger(AbstractDataItemArchive.class.getName());

    protected static final int MAX_STORAGE_QUEUE = 1000; // items
    protected static final int STORAGE_QUEUE_FLUSH_LIMIT = MAX_STORAGE_QUEUE - 100; // size for flush
    protected static final int MAX_LATENCY_TIME = 1000; // milliseconds
    protected static final int LOOK_AHEAD_SPAN = 100; // items to look ahead

    protected final Archive controller;

    protected final Timer latencyStoreTimer = new Timer();
    protected TimerTask latencyTask;

    protected final BlockingQueue<T> storageQueue = new ArrayBlockingQueue<>(MAX_STORAGE_QUEUE);
    protected final List<T> drainingQueue = new ArrayList<>(MAX_STORAGE_QUEUE);

    private Connection storeConnection;
    private PreparedStatement storeStatement;

    protected Connection retrieveConnection; // subclasses should access this field in a synchronized block/method

    private volatile boolean disposed;

    protected AbstractDataItemArchive(Archive controller) throws SQLException {
        this.controller = controller;
        // Get store and retrieve JDBC connections from Archive
        this.storeConnection = this.controller.createConnection(true);
        this.retrieveConnection = this.controller.createConnection(false);
        // Attempt to store every MAX_LATENCY_TIME milliseconds
        this.latencyTask = new TimerTask() {
            @Override
            public void run() {
                storeBuffer();
            }
        };
        this.latencyStoreTimer.schedule(this.latencyTask, 0, MAX_LATENCY_TIME);
        this.disposed = false;
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine(this + " instance - parent constructor completed");
        }
    }

    protected synchronized void storeBuffer() {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - storeBuffer called: storageQueue.size() = " + storageQueue.size());
        }
        drainingQueue.clear();
        if(!storageQueue.isEmpty()) {
            storageQueue.drainTo(drainingQueue);
            try {
                doStore(storeConnection, drainingQueue);
                storeConnection.commit();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, this + " - exception on data storage", e);
                try {
                    storeConnection.rollback();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, this + " - exception on rollback", e);
                }
            }
        }
    }

    protected void doStore(Connection connection, List<T> itemsToStore) throws SQLException, IOException {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - request to store " + itemsToStore.size() + " items");
            // TODO: move to FINEST
            for(T item : itemsToStore) {
                LOG.finer("Storing " + item);
            }
        }
        if(storeStatement == null) {
            storeStatement = createStoreStatement(connection);
        }
        storeStatement.clearBatch();
        for(T item : itemsToStore) {
            setItemPropertiesToStatement(storeStatement, item);
            storeStatement.addBatch();
        }
        int[] numUpdates = storeStatement.executeBatch();
        if(LOG.isLoggable(Level.FINEST)) {
            for (int i = 0; i < numUpdates.length; i++) {
                if (numUpdates[i] == -2) {
                    LOG.finest("Batch job[" + i +
                            "]: unknown number of rows added/updated");
                } else {
                    LOG.finest("Batch job[" + i +
                            "]: " + numUpdates[i] + " rows added/updated");
                }
            }
        }
        storeStatement.clearBatch();
    }

    protected abstract void setItemPropertiesToStatement(PreparedStatement storeStatement, T item) throws SQLException, IOException;

    protected abstract PreparedStatement createStoreStatement(Connection connection) throws SQLException;

    public void store(List<T> items) throws ArchiveException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - store(List) called: items.size() = " + items.size());
        }
        checkDisposed();
        checkStorageQueueFull();
        storageQueue.addAll(items);
    }

    public void store(T item) throws ArchiveException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - store(T) called");
        }
        checkDisposed();
        checkStorageQueueFull();
        storageQueue.add(item);
    }

    protected void checkDisposed() throws ArchiveException {
        if(this.disposed) {
            throw new ArchiveException("Archive disposed");
        }
    }

    private void checkStorageQueueFull() {
        // Close to full: try storage
        if (storageQueue.size() > STORAGE_QUEUE_FLUSH_LIMIT) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - storageQueue almost full: storageQueue.size() = " + storageQueue.size());
            }
            storeBuffer();
        }
    }

    public synchronized T retrieve(IUniqueId uniqueId) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, uniqueId);
        } catch(SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected T doRetrieve(Connection connection, IUniqueId uniqueId) throws SQLException {
        String finalQuery = buildRetrieveByIdQuery();
        T result = null;
        try (PreparedStatement prepStmt = connection.prepareStatement(finalQuery)) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            prepStmt.setLong(1, uniqueId.asLong());
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {
                    try {
                        result = mapToItem(rs, null);
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

    protected abstract String buildRetrieveByIdQuery();

    public synchronized List<T> retrieve(Instant time, K filter, Instant maxLookbackTime) throws ArchiveException {
        throw new UnsupportedOperationException("This operation is not supported by this archive service");
    }

    public synchronized List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, startTime, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected List<T> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws SQLException {
        String finalQuery = buildRetrieveQuery(startTime, numRecords, direction, filter);
        List<T> result = new ArrayList<>(numRecords);
        try (Statement prepStmt = connection.createStatement()) {
            if(LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                while (rs.next()) {
                    try {
                        T object = mapToItem(rs, filter);
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

    protected abstract T mapToItem(ResultSet rs, K usedFilter) throws IOException, SQLException, ClassNotFoundException;

    protected abstract String buildRetrieveQuery(Instant startTime, int numRecords, RetrievalDirection direction, K filter);

    public synchronized List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, startItem, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected List<T> doRetrieve(Connection connection, T startItem, int numRecords, RetrievalDirection direction, K filter) throws SQLException {
        // Use the startItem generationTime to retrieve all the items from that point in time: increase limit by 100
        List<T> largeSize = doRetrieve(connection, startItem.getGenerationTime(), numRecords + LOOK_AHEAD_SPAN, direction, filter);
        // Now scan and get rid of the startItem object: in order to work, equality must work correctly (typically only on the internalId)
        int position = largeSize.indexOf(startItem);
        if(position == -1) {
            return largeSize.subList(0, Math.min(numRecords, largeSize.size()));
        } else {
            return largeSize.subList(position + 1, position + 1 + Math.min(numRecords, largeSize.size() - position - 1));
        }
    }

    public IUniqueId retrieveLastId() throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieveLastId(retrieveConnection, getMainType());
        } catch (SQLException|UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    public IUniqueId retrieveLastId(Class<? extends AbstractDataItem> type) throws ArchiveException {
        checkDisposed();
        try {
            return doRetrieveLastId(retrieveConnection, type);
        } catch (SQLException|UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    protected IUniqueId doRetrieveLastId(Connection connection, Class<? extends AbstractDataItem> type) throws SQLException {
        try (Statement prepStmt = connection.createStatement()) {
            try (ResultSet rs = prepStmt.executeQuery(getLastIdQuery(type))) {
                if (rs.next()) {
                    return new LongUniqueId(rs.getLong(1));
                } else {
                    return null;
                }
            } finally {
                connection.commit();
            }
        }
    }

    /**
     * This method must return a SELECT query, delivering only a single result and a single field. The calling method
     * will read the associated ID (as long) using: resultSet.getLong(1).
     *
     * @return the SELECT query to retrieve the last stored unique ID (as long) for the specific data item
     */
    protected abstract String getLastIdQuery();

    /**
     * This method must return a SELECT query, delivering only a single result and a single field. The calling method
     * will read the associated ID (as long) using: resultSet.getLong(1).
     *
     * @param type the data item class, for which the last stored unique ID shall be retrieved
     * @return the SELECT query to retrieve the last stored unique ID (as long) for the specific data item type
     * @throws UnsupportedOperationException if the type is not supported
     */
    protected String getLastIdQuery(Class<? extends AbstractDataItem> type) throws UnsupportedOperationException {
        if(!getMainType().equals(type)) {
            throw new UnsupportedOperationException("Provided type " + type.getName() + " not supported by " + this);
        } else {
            return getLastIdQuery();
        }
    }



    /**
     * This method must return the class type of the main data item type supported by this archive specific implementation.
     *
     * @return the data item type
     */
    protected abstract Class<T> getMainType();

    /**
     * This method closes all connections and disposes the internal resources, if any. The class is marked as disposed
     * and cannot be used anymore.
     */
    public void dispose() throws ArchiveException {
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - dispose() called");
        }
        checkDisposed();
        this.disposed = true;
        this.latencyTask.cancel();
        this.latencyTask = null;
        this.latencyStoreTimer.cancel();
        if(storeConnection != null) {
            try {
                this.storeConnection.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, this + " - exception when closing store connection", e);
            }
        }
        this.storeConnection = null;
        if(retrieveConnection != null) {
            try {
                this.retrieveConnection.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, this + " - exception when closing retrieve connection", e);
            }
        }
        this.retrieveConnection = null;
        this.storageQueue.clear();
    }

    /**************************************************************************************************
     * Conversion methods
     **************************************************************************************************/

    protected Timestamp toTimestamp(Instant t) {
        return t == null ? null : Timestamp.from(t);
    }

    protected Instant toInstant(Timestamp genTime) {
        return genTime == null ? null : genTime.toInstant();
    }

    protected Object toObject(Blob b) throws IOException, SQLException {
        Object toReturn = null;
        if(b != null) {
            InputStream ois = b.getBinaryStream();
            toReturn = ValueUtil.deserialize(ois.readAllBytes());
        }
        return toReturn;
    }

    // TODO: define efficient serialisation for typical types, fallback to Java Serialisation if not available
    protected Object[] toObjectArray(Blob b) throws IOException, SQLException {
        Object[] toReturn = null;
        if(b != null) {
            InputStream ois = b.getBinaryStream();
            toReturn = (Object[]) ValueUtil.deserialize(ois.readAllBytes());
        }
        return toReturn;
    }

    // TODO: define efficient serialisation for typical types, fallback to Java Serialisation if not available
    protected InputStream toInputstreamArray(Object[] data) {
        if(data == null) {
            return null;
        }
        return new ByteArrayInputStream(ValueUtil.serialize(data));
    }

    protected InputStream toInputstream(Object data) {
        if(data == null) {
            return null;
        }
        return new ByteArrayInputStream(ValueUtil.serialize(data));
    }

    protected byte[] toByteArray(InputStream is) throws IOException {
        return is.readAllBytes();
    }

    protected <E extends Enum<E>> String toEnumFilterListString(List<E> enumList) {
        return toFilterListString(enumList, Enum::ordinal, null);
    }

    protected <E> String toFilterListString(List<E> list, Function<E, Object> extractor, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < list.size(); ++i) {
            if(delimiter != null) {
                sb.append(delimiter);
            }
            sb.append(extractor.apply(list.get(i)));
            if(delimiter != null) {
                sb.append(delimiter);
            }
            if(i != list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
