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

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.persist.timescale.Archive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>> implements IDebugInfoProvider {

    private static final Logger LOG = Logger.getLogger(AbstractDataItemArchive.class.getName());

    protected static final int MAX_STORAGE_QUEUE = 10000; // items
    protected static final int STORAGE_QUEUE_FLUSH_LIMIT = MAX_STORAGE_QUEUE - 100; // size for flush
    protected static final int MAX_LATENCY_TIME = 1000; // milliseconds
    protected static final int LOOK_AHEAD_SPAN = 100; // items to look ahead

    protected static final Instant MINIMUM_TIME = Instant.EPOCH;
    protected static final Instant MAXIMUM_TIME = Instant.EPOCH.plusSeconds(1000L * 365 * 24 * 3600); // 1000 years -> 2970 ... fair enough

    protected final Archive controller;

    protected final Timer latencyStoreTimer = new Timer();
    protected TimerTask latencyTask;

    protected final BlockingQueue<T> storageQueue = new ArrayBlockingQueue<>(MAX_STORAGE_QUEUE);
    protected final List<T> drainingQueue = new ArrayList<>(MAX_STORAGE_QUEUE);

    private Connection storeConnection;
    private PreparedStatement storeStatement;

    protected Connection retrieveConnection; // subclasses should access this field in a synchronized block/method

    private final AtomicLong storedItemsInLastSamplingPeriod = new AtomicLong();
    private Instant lastSamplingTime = Instant.now();
    private final Timer sampler = new Timer();
    private final AtomicReference<List<DebugInformation>> lastStats = new AtomicReference<>(Arrays.asList(
            DebugInformation.of(Archive.ARCHIVE_NAME, toString() + " Input Queue", 0, MAX_STORAGE_QUEUE, ""),
            DebugInformation.of(Archive.ARCHIVE_NAME, toString() + " Storage Rate", 0, null, "items/second")
    ));

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
        this.sampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(this + " instance - parent constructor completed");
        }
    }

    protected synchronized void storeBuffer() {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - storeBuffer called: storageQueue.size() = " + storageQueue.size());
        }
        drainingQueue.clear();
        if (!storageQueue.isEmpty()) {
            storageQueue.drainTo(drainingQueue);
            try {
                doStore(storeConnection, drainingQueue);
                storeConnection.commit();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, this + " - exception on data storage", e);
                try {
                    storeConnection.rollback();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, this + " - exception on rollback", ex);
                    // At this stage, close the connection and re-open it
                    try {
                        storeConnection.close();
                        storeConnection = this.controller.createConnection(true);
                    } catch (SQLException ex1) {
                        // Well... log and give up
                        LOG.log(Level.SEVERE, this + " - exception on connection re-instantiation", ex1);
                    }
                }
            }
            storedItemsInLastSamplingPeriod.addAndGet(drainingQueue.size());
        }
    }

    protected void doStore(Connection connection, List<T> itemsToStore) throws SQLException, IOException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - request to store " + itemsToStore.size() + " items");
            if (LOG.isLoggable(Level.FINEST)) {
                for (T item : itemsToStore) {
                    LOG.finest("Storing " + item);
                }
            }
        }
        if (storeStatement == null) {
            storeStatement = createStoreStatement(connection);
        }
        storeStatement.clearBatch();
        for (T item : itemsToStore) {
            setItemPropertiesToStatement(storeStatement, item);
            storeStatement.addBatch();
        }
        int[] numUpdates = storeStatement.executeBatch();
        if (LOG.isLoggable(Level.FINEST)) {
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
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - store(List) called: items.size() = " + items.size());
        }
        if (items.isEmpty()) {
            // Just ignore the call
            return;
        }
        checkDisposed();
        checkStorageQueueFull(items.size());
        // It can happen that items has more elements than the storageQueue. So we do an incremental mass storage.
        if(items.size() < storageQueue.remainingCapacity()) {
            // Optimisation: add the items straight
            storageQueue.addAll(items);
        } else {
            // Start splitting
            int cycles = (int) Math.ceil(items.size() / (double) STORAGE_QUEUE_FLUSH_LIMIT);
            for(int i = 0; i < cycles; ++i) {
                int start = i * STORAGE_QUEUE_FLUSH_LIMIT;
                int end = i == (cycles - 1) ? items.size() : STORAGE_QUEUE_FLUSH_LIMIT * (i + 1);
                storageQueue.addAll(items.subList(start, end));
                // Force storage
                checkStorageQueueFull(MAX_STORAGE_QUEUE);
            }
        }
    }

    public void store(T item) throws ArchiveException {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - store(T) called");
        }
        checkDisposed();
        checkStorageQueueFull(1);
        storageQueue.add(item);
    }

    protected void checkDisposed() throws ArchiveException {
        if (this.disposed) {
            throw new ArchiveException("Archive disposed");
        }
    }

    private synchronized void checkStorageQueueFull(int wishToInsert) {
        // Close to full: try storage
        if (storageQueue.size() + wishToInsert > STORAGE_QUEUE_FLUSH_LIMIT) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(this + " - storageQueue almost full: storageQueue.size() = " + storageQueue.size());
            }
            storeBuffer();
        }
    }

    public synchronized T retrieve(IUniqueId uniqueId) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieve(IUniqueId) called: uniqueId=" + uniqueId);
        }
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, uniqueId);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected T doRetrieve(Connection connection, IUniqueId uniqueId) throws SQLException {
        String finalQuery = buildRetrieveByIdQuery();
        T result = null;
        try (PreparedStatement prepStmt = connection.prepareStatement(finalQuery)) {
            if (LOG.isLoggable(Level.FINEST)) {
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
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieve(Instant,int,RetrievalDirection,K) called: startTime=" + startTime + ", numRecords=" + numRecords + ", direction=" + direction);
        }
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, startTime, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected List<T> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws SQLException {
        if (startTime.isBefore(MINIMUM_TIME)) {
            startTime = MINIMUM_TIME;
        } else if (startTime.isAfter(MAXIMUM_TIME)) {
            startTime = MAXIMUM_TIME;
        }
        String finalQuery = buildRetrieveQuery(startTime, numRecords, direction, filter);
        List<T> result = new ArrayList<>(numRecords);
        try (Statement prepStmt = connection.createStatement()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(this + " - retrieve statement: " + finalQuery);
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

    protected abstract String buildRetrieveQuery(Instant startTime, Instant endTime, boolean ascending, K filter);

    public synchronized List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieve(T,int,RetrievalDirection,K) called: startItem=" + startItem + ", numRecords=" + numRecords + ", direction=" + direction);
        }
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, startItem, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    public synchronized List<T> retrieve(Instant startTime, Instant endTime, K filter) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieve(T,int,RetrievalDirection,K) called: startItem=" + startTime + ", endTime=" + endTime);
        }
        checkDisposed();
        try {
            return doRetrieve(retrieveConnection, startTime, endTime, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected List<T> doRetrieve(Connection connection, Instant startTime, Instant endTime, K filter) throws SQLException {
        if (startTime.isBefore(MINIMUM_TIME)) {
            startTime = MINIMUM_TIME;
        } else if (startTime.isAfter(MAXIMUM_TIME)) {
            startTime = MAXIMUM_TIME;
        }
        if (endTime.isBefore(MINIMUM_TIME)) {
            endTime = MINIMUM_TIME;
        } else if (endTime.isAfter(MAXIMUM_TIME)) {
            endTime = MAXIMUM_TIME;
        }
        String finalQuery = buildRetrieveQuery(startTime, endTime, startTime.isBefore(endTime), filter);
        List<T> result = new LinkedList<>();
        try (Statement prepStmt = connection.createStatement()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(this + " - retrieve statement: " + finalQuery);
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

    protected List<T> doRetrieve(Connection connection, T startItem, int numRecords, RetrievalDirection direction, K filter) throws SQLException {
        // Use the startItem generationTime to retrieve all the items from that point in time: increase limit by 100
        List<T> largeSize = doRetrieve(connection, startItem.getGenerationTime(), startItem.getInternalId(), numRecords + LOOK_AHEAD_SPAN, direction, filter);
        // Now scan and get rid of the startItem object: in order to work, equality must work correctly (typically only on the internalId)
        int position = largeSize.indexOf(startItem);
        if (position == -1) {
            return new ArrayList<>(largeSize.subList(0, Math.min(numRecords, largeSize.size())));
        } else {
            return new ArrayList<>(largeSize.subList(position + 1, position + 1 + Math.min(numRecords, largeSize.size() - position - 1)));
        }
    }

    protected List<T> doRetrieve(Connection connection, Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, K filter) throws SQLException {
        if (startTime.isBefore(MINIMUM_TIME)) {
            startTime = MINIMUM_TIME;
        } else if (startTime.isAfter(MAXIMUM_TIME)) {
            startTime = MAXIMUM_TIME;
        }
        String finalQuery = buildRetrieveQuery(startTime, internalId, numRecords, direction, filter);
        List<T> result = new ArrayList<>(numRecords);
        try (Statement prepStmt = connection.createStatement()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(this + " - retrieve statement: " + finalQuery);
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

    protected abstract String buildRetrieveQuery(Instant startTime, IUniqueId internalId, int numRecords, RetrievalDirection direction, K filter);

    protected void addTimeInfo(StringBuilder query, Instant startTime, IUniqueId internalId, RetrievalDirection direction) {
        if (direction == RetrievalDirection.TO_FUTURE) {
            query.append("(GenerationTime > '").append(toTimestamp(startTime).toString())
                    .append("' OR (GenerationTime = '").append(toTimestamp(startTime).toString()).append("' AND UniqueId >= ").append(internalId.asLong()).append(") ) ");
        } else {
            query.append("(GenerationTime < '").append(toTimestamp(startTime).toString())
                    .append("' OR (GenerationTime = '").append(toTimestamp(startTime).toString()).append("' AND UniqueId <= ").append(internalId.asLong()).append(") ) ");
        }
    }

    protected void addTimeInfo(StringBuilder query, Instant startTime, RetrievalDirection direction) {
        if (direction == RetrievalDirection.TO_FUTURE) {
            query.append("GenerationTime >= '").append(toTimestamp(startTime).toString()).append("' ");
        } else {
            query.append("GenerationTime <= '").append(toTimestamp(startTime).toString()).append("' ");
        }
    }

    protected void addTimeRangeInfo(StringBuilder query, Instant startTime, Instant endTime, boolean ascending) {
        if (ascending) { // startTime < endTime
            query.append("GenerationTime >= '").append(toTimestamp(startTime).toString())
                    .append("' AND GenerationTime <= '").append(toTimestamp(endTime).toString()).append("' ");
        } else { // endTime < startTime
            query.append("GenerationTime >= '").append(toTimestamp(endTime).toString())
                    .append("' AND GenerationTime <= '").append(toTimestamp(startTime).toString()).append("' ");
        }
    }

    public synchronized IUniqueId retrieveLastId() throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieveLastId() called");
        }
        checkDisposed();
        try {
            return doRetrieveLastId(retrieveConnection, getMainType());
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    public synchronized IUniqueId retrieveLastId(Class<? extends AbstractDataItem> type) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieveLastId(Class) called: type=" + type.getSimpleName());
        }
        checkDisposed();
        try {
            return doRetrieveLastId(retrieveConnection, type);
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    protected IUniqueId doRetrieveLastId(Connection connection, Class<? extends AbstractDataItem> type) throws SQLException {
        try (Statement prepStmt = connection.createStatement()) {
            String finalQuery = getLastIdQuery(type);
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(this + " - retrieve statement: " + finalQuery);
            }
            try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
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
        if (!getMainType().equals(type)) {
            throw new UnsupportedOperationException("Provided type " + type.getName() + " not supported by " + this);
        } else {
            return getLastIdQuery();
        }
    }



    public synchronized Instant retrieveLastGenerationTime() throws ArchiveException {
        return retrieveLastGenerationTime(getMainType());
    }

    public synchronized Instant retrieveLastGenerationTime(Class<? extends AbstractDataItem> type) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - retrieveLastGenerationTime(Class) called: type=" + type.getSimpleName());
        }
        checkDisposed();
        try {
            try (Statement prepStmt = retrieveConnection.createStatement()) {
                String finalQuery = getLastGenerationTimeQuery(type);
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer(this + " - retrieve statement: " + finalQuery);
                }
                try (ResultSet rs = prepStmt.executeQuery(finalQuery)) {
                    if (rs.next()) {
                        return toInstant(rs.getTimestamp(1));
                    } else {
                        return null;
                    }
                } finally {
                    retrieveConnection.commit();
                }
            }
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    /**
     * This method must return: SELECT MAX(GenerationTime) FROM [TABLE from type].
     *
     * @param type the type of data item
     * @return the query
     */
    protected abstract String getLastGenerationTimeQuery(Class<? extends AbstractDataItem> type);

    public synchronized void purge(Instant referenceTime, RetrievalDirection direction) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - purge(Instant,RetrievalDirection) called: referenceTime=" + referenceTime + ", direction=" + direction);
        }
        checkDisposed();
        try {
            try (Statement prepStmt = storeConnection.createStatement()) {
                try {
                    for (String query : getPurgeQuery(referenceTime, direction)) {
                        if (LOG.isLoggable(Level.FINER)) {
                            LOG.finer(this + " - delete statement: " + query);
                        }
                        prepStmt.executeQuery(query);
                    }
                } finally {
                    storeConnection.commit();
                }
            }
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    /**
     * This method must return a list of queries in the form: DELETE FROM [TABLE from type] WHERE GenerationTime [&lt; or &gt; from direction] 'referenceTime'
     *
     * @param referenceTime the reference time
     * @param direction     the direction of deletion
     * @return the query
     */
    protected abstract List<String> getPurgeQuery(Instant referenceTime, RetrievalDirection direction);

    /**
     * This method must return the class type of the main data item type supported by this archive specific implementation.
     *
     * @return the data item type
     */
    protected abstract Class<T> getMainType();

    public synchronized void remove(IUniqueId id) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - remove(IUniqueId) called: id=" + id);
        }
        checkDisposed();
        try {
            try (Statement prepStmt = storeConnection.createStatement()) {
                try {
                    String query = getRemoveQuery(id);
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer(this + " - delete statement: " + query);
                    }
                    prepStmt.execute(query);
                } finally {
                    storeConnection.commit();
                }
            }
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    protected String getRemoveQuery(IUniqueId id) {
        throw new UnsupportedOperationException("Not implemented for this data type");
    }

    public synchronized void remove(K filter) throws ArchiveException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(this + " - remove(AbstractDataItemFilter) called: filter=" + filter);
        }
        checkDisposed();
        try {
            try (Statement prepStmt = storeConnection.createStatement()) {
                try {
                    String query = getRemoveQuery(filter);
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer(this + " - delete statement: " + query);
                    }
                    prepStmt.execute(query);
                } finally {
                    storeConnection.commit();
                }
            }
        } catch (SQLException | UnsupportedOperationException e) {
            throw new ArchiveException(e);
        }
    }

    protected String getRemoveQuery(K filter) {
        throw new UnsupportedOperationException("Not implemented for this data type");
    }

    /**
     * This method closes all connections and disposes the internal resources, if any. The class is marked as disposed
     * and cannot be used anymore.
     */
    public synchronized void dispose() throws ArchiveException {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest(this + " - dispose() called");
        }
        checkDisposed();
        this.disposed = true;
        this.latencyTask.cancel();
        this.latencyTask = null;
        this.latencyStoreTimer.cancel();
        if (storeConnection != null) {
            try {
                this.storeConnection.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, this + " - exception when closing store connection", e);
            }
        }
        this.storeConnection = null;
        if (retrieveConnection != null) {
            try {
                this.retrieveConnection.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, this + " - exception when closing retrieve connection", e);
            }
        }
        this.retrieveConnection = null;
        this.storageQueue.clear();
        this.sampler.cancel();
    }

    /**************************************************************************************************
     * Conversion methods
     **************************************************************************************************/

    protected static Timestamp toTimestamp(Instant t) {
        return t == null ? null : Timestamp.from(t);
    }

    protected static Instant toInstant(Timestamp genTime) {
        return genTime == null ? null : genTime.toInstant();
    }

    protected static Object toObject(Blob b) throws IOException, SQLException {
        Object toReturn = null;
        if (b != null) {
            InputStream ois = b.getBinaryStream();
            toReturn = ValueUtil.deserialize(ois.readAllBytes());
        }
        return toReturn;
    }

    protected static InputStream toInputstream(Object data) {
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(ValueUtil.serialize(data));
    }

    protected static byte[] toByteArray(InputStream is) throws IOException {
        return is.readAllBytes();
    }

    protected static byte[] toBytes(Object data) {
        if (data == null) {
            return null;
        }
        return ValueUtil.serialize(data);
    }

    protected static Object toObject(byte[] data) {
        if (data == null) {
            return null;
        }
        return ValueUtil.deserialize(data);
    }

    protected static <E extends Enum<E>> String toEnumFilterListString(Set<E> enumList) {
        return toFilterListString(enumList, Enum::ordinal, null);
    }

    protected static <E> String toFilterListString(Set<E> list, Function<E, Object> extractor, String delimiter) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (E o : list) {
            if (delimiter != null) {
                sb.append(delimiter);
            }
            sb.append(extractor.apply(o));
            if (delimiter != null) {
                sb.append(delimiter);
            }
            if (i != list.size() - 1) {
                sb.append(",");
            }
            ++i;
        }
        return sb.toString();
    }

    private void sample() {
        Instant now = Instant.now();
        long items = storedItemsInLastSamplingPeriod.getAndSet(0);
        long millis = now.toEpochMilli() - lastSamplingTime.toEpochMilli();
        double itemsPerSec = (items / (millis / 1000.0));
        lastSamplingTime = now;
        List<DebugInformation> toSet = Arrays.asList(
                DebugInformation.of(Archive.ARCHIVE_NAME, toString() + " Input Queue", storageQueue.size(), MAX_STORAGE_QUEUE, ""),
                DebugInformation.of(Archive.ARCHIVE_NAME, toString() + " Storage Rate", (int) itemsPerSec, null, "items/second")
        );
        lastStats.set(toSet);
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return lastStats.get();
    }
}
