/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.core.api.IOperationalMessageBroker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OperationalMessageBrokerImpl extends Handler implements IOperationalMessageProvisionService, IOperationalMessageBroker {

    public static final String REATMETRIC_ID = "ReatMetric";
    private static final Logger LOG = Logger.getLogger(OperationalMessageBrokerImpl.class.getName());

    private final IOperationalMessageArchive archive;
    private final AtomicLong sequencer;

    private final List<OperationalMessageSubscriptionManager> subscribers = new CopyOnWriteArrayList<>();
    private final Map<IOperationalMessageSubscriber, OperationalMessageSubscriptionManager> subscriberIndex = new ConcurrentHashMap<>();

    public OperationalMessageBrokerImpl(IOperationalMessageArchive archive) throws ArchiveException {
        this.archive = archive;
        this.sequencer = new AtomicLong();
        IUniqueId lastStoredUniqueId = archive != null ? archive.retrieveLastId() : null;
        if(lastStoredUniqueId == null) {
            this.sequencer.set(0);
        } else {
            this.sequencer.set(lastStoredUniqueId.asLong());
        }
        setLevel(Level.INFO);
        Logger.getLogger("eu.dariolucia.reatmetric").addHandler(this);
    }

    @Override
    public void subscribe(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
        OperationalMessageSubscriptionManager manager = subscriberIndex.get(subscriber);
        if(manager == null) {
            manager = new OperationalMessageSubscriptionManager(subscriber, filter, false);
            subscriberIndex.put(subscriber, manager);
            subscribers.add(manager);
        } else {
            manager.update(filter);
        }
    }

    @Override
    public List<OperationalMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException{
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startTime, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<OperationalMessage> retrieve(OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException {
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startItem, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return null;
    }

    private void distribute(List<OperationalMessage> items, boolean store) throws ReatmetricException {
        if(store && archive != null) {
            archive.store(items);
        }
        for(OperationalMessageSubscriptionManager s : subscribers) {
            try {
                s.notifyItems(items);
            } catch (Exception e) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Exception when notifying subscriber on operational message broker", e);
                }
            }
        }
    }

    @Override
    public void unsubscribe(IOperationalMessageSubscriber subscriber) {
        OperationalMessageSubscriptionManager manager = this.subscriberIndex.remove(subscriber);
        if(manager != null) {
            this.subscribers.remove(manager);
            manager.terminate();
        }
    }

    private IUniqueId nextOperationalMessageId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    @Override
    public void publish(LogRecord record) {
        if(record.getLevel().intValue() < Level.INFO.intValue()) {
            // Simple trace, ignore at this level
            return;
        }
        IUniqueId idToAssign = nextOperationalMessageId();
        Severity messageSeverity = Severity.INFO;
        if(record.getLevel().intValue() >= Level.SEVERE.intValue()) {
            messageSeverity = Severity.ALARM;
        } else if(record.getLevel().intValue() >= Level.WARNING.intValue()) {
            messageSeverity = Severity.WARN;
        }
        OperationalMessage om = new OperationalMessage(idToAssign, record.getInstant(), REATMETRIC_ID, record.getMessage(), record.getLoggerName(), messageSeverity, null);
        try {
            distribute(Collections.singletonList(om), true);
        } catch (ReatmetricException e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Exception when distributing on operational message broker", e);
            }
        }
    }

    @Override
    public void flush() {
        // Do nothing
    }

    @Override
    public void close() throws SecurityException {
        // Do nothing
    }

    @Override
    public OperationalMessage distribute(String id, String message, String source, Severity severity, Object[] additionalFields, boolean store) throws ReatmetricException {
        IUniqueId idToAssign = nextOperationalMessageId();
        OperationalMessage om = new OperationalMessage(idToAssign, Instant.now(), id, message, source, severity, additionalFields);
        distribute(Collections.singletonList(om), store);
        return om;
    }

    private static class OperationalMessageSubscriptionManager {

        private static final Predicate<OperationalMessage> IDENTITY_FILTER = (o) -> true;

        private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
        private final BlockingQueue<OperationalMessage> queue = new ArrayBlockingQueue<>(1000);
        private final IOperationalMessageSubscriber subscriber;
        private final boolean timely;
        private volatile Predicate<OperationalMessage> filter;

        public OperationalMessageSubscriptionManager(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter, boolean timely) {
            this.subscriber = subscriber;
            this.filter = filter == null ? IDENTITY_FILTER : filter;
            this.timely = timely;
            dispatcher.submit(this::processQueue);
        }

        private void processQueue() {
            List<OperationalMessage> drainer = new ArrayList<>(1000);
            while(!dispatcher.isShutdown()) {
                // Wait to have elements in the queue
                synchronized (queue) {
                    while(queue.isEmpty() && !dispatcher.isShutdown()) {
                        try {
                            queue.wait(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    // If the queue is still empty, repeat the outer cycle
                    if(queue.isEmpty()) {
                        continue;
                    }
                    // If you are here, there are elements in the queue: so drain them
                    queue.drainTo(drainer);
                }
                // Now filter the items and then inform the subscriber
                List<OperationalMessage> toNotify = filterItems(drainer);
                if(!toNotify.isEmpty()) {
                    subscriber.dataItemsReceived(toNotify);
                }
                drainer.clear();
            }
        }

        public void notifyItems(List<OperationalMessage> messages) {
            synchronized (queue) {
                if (timely && !queue.isEmpty() && messages.size() > queue.remainingCapacity()) {
                    queue.clear();
                }
                queue.addAll(messages);
                queue.notifyAll();
            }
        }

        private List<OperationalMessage> filterItems(List<OperationalMessage> items) {
            Predicate<OperationalMessage> theFilter = filter;
            return items.stream().filter(theFilter).collect(Collectors.toList());
        }

        public void update(OperationalMessageFilter filter) {
            this.filter = filter == null ? IDENTITY_FILTER : filter;
        }

        public void terminate() {
            dispatcher.shutdown();
            synchronized (queue) {
                queue.notifyAll();
            }
            try {
                dispatcher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
