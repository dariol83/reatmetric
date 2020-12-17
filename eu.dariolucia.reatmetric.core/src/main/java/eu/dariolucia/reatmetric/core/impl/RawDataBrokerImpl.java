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

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.*;
import eu.dariolucia.reatmetric.core.ReatmetricSystemImpl;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RawDataBrokerImpl implements IRawDataBroker, IRawDataProvisionService {

    private static final Logger LOG = Logger.getLogger(RawDataBrokerImpl.class.getName());

    private final ReatmetricSystemImpl core;
    private final IRawDataArchive archive;
    private final AtomicLong sequencer;

    private final List<RawDataSubscriptionManager> subscribers = new CopyOnWriteArrayList<>();
    private final Map<IRawDataSubscriber, RawDataSubscriptionManager> subscriberIndex = new ConcurrentHashMap<>();

    public RawDataBrokerImpl(ReatmetricSystemImpl core, IRawDataArchive archive) throws ArchiveException {
        this.core = core;
        this.archive = archive;
        this.sequencer = new AtomicLong();
        IUniqueId lastStoredUniqueId = archive != null ? archive.retrieveLastId() : null;
        if(lastStoredUniqueId == null) {
            this.sequencer.set(0);
        } else {
            this.sequencer.set(lastStoredUniqueId.asLong());
        }
    }

    @Override
    public RawData getRawDataContents(IUniqueId uniqueId) throws ReatmetricException {
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(uniqueId);
        } else {
            return null;
        }
    }

    @Override
    public LinkedHashMap<String, String> getRenderedInformation(RawData rawData) {
        return core.getRenderedInformation(rawData);
    }

    @Override
    public void subscribe(IRawDataSubscriber subscriber, RawDataFilter filter) {
        subscribe(subscriber, null, filter, null);
    }

    @Override
    public List<RawData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException{
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startTime, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<RawData> retrieve(RawData startItem, int numRecords, RetrievalDirection direction, RawDataFilter filter) throws ReatmetricException {
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startItem, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void distribute(List<RawData> items, boolean store) throws ReatmetricException {
        if(store && archive != null) {
            archive.store(items);
        }

        for(RawDataSubscriptionManager s : subscribers) {
            try {
                s.notifyItems(items);
            } catch (Exception e) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Exception when notifying subscriber on raw data broker", e);
                }
            }
        }
    }

    @Override
    public void subscribe(IRawDataSubscriber subscriber, Predicate<RawData> preFilter, RawDataFilter filter, Predicate<RawData> postFilter) {
        RawDataSubscriptionManager manager = subscriberIndex.get(subscriber);
        if(manager == null) {
            manager = new RawDataSubscriptionManager(this, subscriber, preFilter, filter, postFilter);
            subscriberIndex.put(subscriber, manager);
            subscribers.add(manager);
        } else {
            manager.update(preFilter, filter, postFilter);
        }
    }

    @Override
    public void unsubscribe(IRawDataSubscriber subscriber) {
        RawDataSubscriptionManager manager = this.subscriberIndex.remove(subscriber);
        if(manager != null) {
            this.subscribers.remove(manager);
            manager.terminate();
        }
    }

    @Override
    public IUniqueId nextRawDataId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    private static class RawDataSubscriptionManager {

        private static final int MAX_QUEUE = 1000;
        private static final Predicate<RawData> IDENTITY_FILTER = (o) -> true;

        private final Thread dispatcher;
        private final BlockingQueue<RawData> items;

        private final IRawDataSubscriber subscriber;
        private final RawDataBrokerImpl broker;
        private Predicate<RawData> preFilter;
        private Predicate<RawData> filter;
        private Predicate<RawData> postFilter;
        private volatile boolean running = true;

        public RawDataSubscriptionManager(RawDataBrokerImpl broker, IRawDataSubscriber subscriber, Predicate<RawData> preFilter, RawDataFilter filter, Predicate<RawData> postFilter) {
            this.broker = broker;
            this.subscriber = subscriber;
            this.preFilter = preFilter;
            this.filter = filter;
            this.postFilter = postFilter;
            sanitizeFilters();
            this.items = new LinkedBlockingQueue<>();
            this.dispatcher = new Thread(this::run);
            this.dispatcher.setName("Raw Data Dispatcher Thread - " + subscriber.toString());
            this.dispatcher.setDaemon(true);
            this.dispatcher.start();
        }

        private void run() {
            List<RawData> toProcess;
            while(running) {
                toProcess = new ArrayList<>(MAX_QUEUE);
                // Wait for item
                synchronized (this) {
                    while(running && items.isEmpty()) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            // Repeat the cycle
                        }
                    }
                    if(!running) {
                        items.clear();
                        this.notifyAll();
                        return;
                    }
                    // Drain
                    items.drainTo(toProcess);
                    this.notifyAll();
                }
                if(!toProcess.isEmpty()) {
                    try {
                        subscriber.dataItemsReceived(toProcess);
                    } catch (RemoteException e) {
                        LOG.log(Level.SEVERE, "Cannot notify subscriber, terminating...", e);
                        broker.unsubscribe(subscriber);
                    }
                }
            }
        }

        public void notifyItems(List<RawData> toDistribute) {
            if(!running) {
                return;
            }
            List<RawData> toNotify = filterItems(toDistribute);
            if(!toNotify.isEmpty()) {
                // Wait for space
                synchronized (this) {
                    while(running && items.size() >= MAX_QUEUE && !Thread.currentThread().equals(dispatcher)) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            // Repeat the cycle
                        }
                    }
                    if(!running) {
                        items.clear();
                        this.notifyAll();
                        return;
                    }
                    // Add
                    items.addAll(toNotify);
                    this.notifyAll();
                }
            }
        }

        private synchronized List<RawData> filterItems(List<RawData> items) {
            return items.stream().filter(preFilter).filter(filter).filter(postFilter).collect(Collectors.toList());
        }

        public synchronized void update(Predicate<RawData> preFilter, RawDataFilter filter, Predicate<RawData> postFilter) {
            this.preFilter = preFilter;
            this.filter = filter;
            this.postFilter = postFilter;
            sanitizeFilters();
        }

        private void sanitizeFilters() {
            if(preFilter == null) {
                preFilter = IDENTITY_FILTER;
            }
            if(filter == null) {
                filter = IDENTITY_FILTER;
            }
            if(postFilter == null) {
                postFilter = IDENTITY_FILTER;
            }
        }

        public synchronized void terminate() {
            running = false;
            notifyAll();
        }
    }
}
