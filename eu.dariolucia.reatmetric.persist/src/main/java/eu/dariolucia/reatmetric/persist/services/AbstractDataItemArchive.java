package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.persist.Archive;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.function.Function;

public abstract class AbstractDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter> {

    protected static final int MAX_STORAGE_QUEUE = 1000; // items
    protected static final int STORAGE_QUEUE_FLUSH_LIMIT = MAX_STORAGE_QUEUE - 100; // size for flush
    protected static final int MAX_LATENCY_TIME = 1000; // milliseconds
    protected static final int LOOK_AHEAD_SPAN = 100; // items to look ahead

    protected final Archive controller;

    protected final Timer latencyStoreTimer = new Timer();
    protected TimerTask latencyTask = null;

    protected final BlockingQueue<T> storageQueue = new ArrayBlockingQueue<>(MAX_STORAGE_QUEUE);
    protected final List<T> drainingQueue = new ArrayList<>(MAX_STORAGE_QUEUE);

    private Connection storeConnection;
    private Connection retrieveConnection;

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
        System.out.println("Constructor is over");
    }

    protected void storeBuffer() {
        System.out.println("storeBuffer called");
        drainingQueue.clear();
        if(!storageQueue.isEmpty()) {
            storageQueue.drainTo(drainingQueue);
            try {
                doStore(storeConnection, drainingQueue);
                storeConnection.commit();
            } catch (Exception e) {
                // TODO: severe
                e.printStackTrace();
                try {
                    storeConnection.rollback();
                } catch (SQLException ex) {
                    // TODO: severe
                    ex.printStackTrace();
                }
            }
        }
    }

    public void store(List<T> items) {
        // Close to full: try storage
        if(storageQueue.size() > STORAGE_QUEUE_FLUSH_LIMIT) {
            this.latencyStoreTimer.schedule(this.latencyTask, 0);
        }
        storageQueue.addAll(items);
    }

    public void store(T item) {
        // Close to full: try storage
        if(storageQueue.size() > STORAGE_QUEUE_FLUSH_LIMIT) {
            this.latencyStoreTimer.schedule(this.latencyTask, 0);
        }
        storageQueue.add(item);
    }

    protected abstract void doStore(Connection connection, List<T> itemsToStore) throws SQLException, IOException;

    public synchronized List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException {
        try {
            return doRetrieve(retrieveConnection, startTime, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected abstract List<T> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws SQLException;

    public synchronized List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException {
        try {
            return doRetrieve(retrieveConnection, startItem, numRecords, direction, filter);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected abstract List<T> doRetrieve(Connection connection, T startItem, int numRecords, RetrievalDirection direction, K filter) throws SQLException;

    public IUniqueId retrieveLastId() throws ArchiveException {
        try {
            return doRetrieveLastId(retrieveConnection);
        } catch (SQLException e) {
            throw new ArchiveException(e);
        }
    }

    protected abstract IUniqueId doRetrieveLastId(Connection connection) throws SQLException;

    public void dispose() {
        // TODO
    }

    /**************************************************************************************************
     * Conversion methods
     **************************************************************************************************/

    protected Timestamp toTimestamp(Instant t) {
        Timestamp ts = new Timestamp(t.toEpochMilli());
        ts.setNanos(t.getNano());
        return ts;
    }

    protected Instant toInstant(Timestamp genTime) {
        return Instant.ofEpochSecond(genTime.getTime() / 1000, genTime.getNanos());
    }

    protected InputStream toInputstream(Object[] data) throws IOException {
        if(data == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        oos.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    protected <E extends Enum<E>> String toEnumFilterListString(List<E> enumList) {
        return toFilterListString(enumList, Enum::ordinal);
    }

    protected <E> String toFilterListString(List<E> list, Function<E, Object> extractor) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < list.size(); ++i) {
            sb.append(extractor.apply(list.get(i)));
            if(i != list.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

}
