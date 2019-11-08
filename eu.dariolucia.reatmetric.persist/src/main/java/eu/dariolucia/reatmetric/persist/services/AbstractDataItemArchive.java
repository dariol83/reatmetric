package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.persist.Archive;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter> {

    protected static final int MAX_STORAGE_QUEUE = 1000; // items
    protected static final int STORAGE_QUEUE_FLUSH_LIMIT = MAX_STORAGE_QUEUE - 100; // size for flush
    protected static final int MAX_LATENCY_TIME = 1000; // milliseconds

    protected final Archive controller;

    protected final ScheduledThreadPoolExecutor latencyStoreTimer = new ScheduledThreadPoolExecutor(1, (ThreadFactory) r -> {
        Thread t = new Thread();
        t.setDaemon(true);
        t.setName(AbstractDataItemArchive.this.getClass().getSimpleName() + " Storage Thread");
        return t;
    });

    protected final BlockingQueue<T> storageQueue = new ArrayBlockingQueue<T>(MAX_STORAGE_QUEUE);
    protected final List<T> drainingQueue = new ArrayList<>(MAX_STORAGE_QUEUE);

    private Connection storeConnection;
    private Connection retrieveConnection;

    protected AbstractDataItemArchive(Archive controller) {
        this.controller = controller;
        // Get store and retrieve JDBC connections from Archive
        this.storeConnection = this.controller.createConnection(true);
        this.retrieveConnection = this.controller.createConnection(false);
        // Initialise tables (if not present)
        initTables(storeConnection);
        // Initialise related unique counter
        // TODO:
        // Attempt to store every MAX_LATENCY_TIME milliseconds
        this.latencyStoreTimer.scheduleAtFixedRate(this::storeBuffer, 0, MAX_LATENCY_TIME, TimeUnit.MILLISECONDS);
    }

    protected abstract void initTables(Connection storeConnection);

    protected void storeBuffer() {
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
            this.latencyStoreTimer.execute(this::storeBuffer);
        }
        storageQueue.addAll(items);
    }

    public void store(T item) {
        // Close to full: try storage
        if(storageQueue.size() > STORAGE_QUEUE_FLUSH_LIMIT) {
            this.latencyStoreTimer.execute(this::storeBuffer);
        }
        storageQueue.add(item);
    }

    protected abstract void doStore(Connection connection, List<T> itemsToStore);

    public synchronized List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) {
        return doRetrieve(retrieveConnection, startTime, numRecords, direction, filter);
    }

    protected abstract List<T> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, K filter);

    public synchronized List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) {
        return doRetrieve(retrieveConnection, startItem, numRecords, direction, filter);
    }

    protected abstract List<T> doRetrieve(Connection connection, T startItem, int numRecords, RetrievalDirection direction, K filter);
}
