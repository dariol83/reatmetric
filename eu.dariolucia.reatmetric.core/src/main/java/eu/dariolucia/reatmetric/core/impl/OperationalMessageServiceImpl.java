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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OperationalMessageServiceImpl implements IOperationalMessageProvisionService {

    private final IOperationalMessageArchive archive;
    private final AtomicLong sequencer;

    private final List<OperationalMessageSubscriptionManager> subscribers = new CopyOnWriteArrayList<>();
    private final Map<IOperationalMessageSubscriber, OperationalMessageSubscriptionManager> subscriberIndex = new ConcurrentHashMap<>();

    public OperationalMessageServiceImpl(IOperationalMessageArchive archive) throws ArchiveException {
        this.archive = archive;
        this.sequencer = new AtomicLong();
        IUniqueId lastStoredUniqueId = archive.retrieveLastId();
        if(lastStoredUniqueId == null) {
            this.sequencer.set(0);
        } else {
            this.sequencer.set(lastStoredUniqueId.asLong());
        }
        // TODO: register to java.util.logging as handler
    }

    @Override
    public void subscribe(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
        OperationalMessageSubscriptionManager manager = subscriberIndex.get(subscriber);
        if(manager == null) {
            manager = new OperationalMessageSubscriptionManager(subscriber, filter);
            subscriberIndex.put(subscriber, manager);
            subscribers.add(manager);
        } else {
            manager.update(filter);
        }
    }

    @Override
    public List<OperationalMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException{
        // Access the archive and query it
        return archive.retrieve(startTime, numRecords, direction, filter);
    }

    @Override
    public List<OperationalMessage> retrieve(OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ReatmetricException {
        // Access the archive and query it
        return archive.retrieve(startItem, numRecords, direction, filter);
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return null;
    }

    private void distribute(List<OperationalMessage> items, boolean store) throws ReatmetricException {
        if(store) {
            archive.store(items);
        }
        for(OperationalMessageSubscriptionManager s : subscribers) {
            s.notifyItems(items);
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

    private static class OperationalMessageSubscriptionManager {

        private static final Predicate<OperationalMessage> IDENTITY_FILTER = (o) -> true;

        private final ExecutorService dispatcher = Executors.newSingleThreadExecutor();
        private final IOperationalMessageSubscriber subscriber;
        private Predicate<OperationalMessage> filter;

        public OperationalMessageSubscriptionManager(IOperationalMessageSubscriber subscriber, OperationalMessageFilter filter) {
            this.subscriber = subscriber;
            this.filter = filter;
            sanitizeFilters();
        }

        public void notifyItems(List<OperationalMessage> items) {
            // TODO: implement timely and blocking policy
            dispatcher.submit(() -> {
                List<OperationalMessage> toNotify = filterItems(items);
                if(!toNotify.isEmpty()) {
                    subscriber.dataItemsReceived(toNotify);
                }
            });
        }

        private synchronized List<OperationalMessage> filterItems(List<OperationalMessage> items) {
            return items.stream().filter(filter).collect(Collectors.toList());
        }

        public synchronized void update(OperationalMessageFilter filter) {
            this.filter = filter;
            sanitizeFilters();
        }

        private void sanitizeFilters() {
            if(filter == null) {
                filter = IDENTITY_FILTER;
            }
        }

        public synchronized void terminate() {
            dispatcher.shutdownNow();
        }
    }
}
