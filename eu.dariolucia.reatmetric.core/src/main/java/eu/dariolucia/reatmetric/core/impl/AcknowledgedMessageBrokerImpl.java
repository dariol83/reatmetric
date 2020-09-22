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
import eu.dariolucia.reatmetric.api.messages.*;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AcknowledgedMessageBrokerImpl implements IAcknowledgedMessageProvisionService {

    private static final Logger LOG = Logger.getLogger(AcknowledgedMessageBrokerImpl.class.getName());

    private final IAcknowledgedMessageArchive archive;
    private final AtomicLong sequencer;

    private final List<AcknowledgedMessageSubscriptionManager> subscribers = new CopyOnWriteArrayList<>();
    private final Map<IAcknowledgedMessageSubscriber, AcknowledgedMessageSubscriptionManager> subscriberIndex = new ConcurrentHashMap<>();

    private final Map<IUniqueId, AcknowledgedMessage> unacknowledgedMessages = new LinkedHashMap<>();

    private final AcknowledgementServiceImpl acknowledgementMessageService;

    public AcknowledgedMessageBrokerImpl(IAcknowledgedMessageArchive archive) throws ArchiveException {
        this.archive = archive;
        this.sequencer = new AtomicLong();
        IUniqueId lastStoredUniqueId = archive != null ? archive.retrieveLastId() : null;
        if(lastStoredUniqueId == null) {
            this.sequencer.set(0);
        } else {
            this.sequencer.set(lastStoredUniqueId.asLong());
        }
        this.acknowledgementMessageService = new AcknowledgementServiceImpl(this);
    }

    public void initialise() throws ArchiveException {
        if(this.archive != null) {
            List<AcknowledgedMessage> messages = this.archive.retrieve(Instant.now(), 1000, RetrievalDirection.TO_PAST, new AcknowledgedMessageFilter(null, Collections.singletonList(AcknowledgementState.PENDING)));
            synchronized (this) {
                for (AcknowledgedMessage am : messages) {
                    unacknowledgedMessages.put(am.getInternalId(), am);
                }
            }
        }
    }

    @Override
    public synchronized void subscribe(IAcknowledgedMessageSubscriber subscriber, AcknowledgedMessageFilter filter) {
        AcknowledgedMessageSubscriptionManager manager = subscriberIndex.get(subscriber);
        if(manager == null) {
            manager = new AcknowledgedMessageSubscriptionManager(this, subscriber, filter, false, new ArrayList<>(unacknowledgedMessages.values()));
            subscriberIndex.put(subscriber, manager);
            subscribers.add(manager);
        } else {
            manager.update(filter);
        }
    }

    @Override
    public List<AcknowledgedMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) throws ReatmetricException{
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startTime, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<AcknowledgedMessage> retrieve(AcknowledgedMessage startItem, int numRecords, RetrievalDirection direction, AcknowledgedMessageFilter filter) throws ReatmetricException {
        // Access the archive and query it
        if(archive != null) {
            return archive.retrieve(startItem, numRecords, direction, filter);
        } else {
            return Collections.emptyList();
        }
    }

    // To be called by the object owning the instance
    void distribute(List<OperationalMessage> items) throws ReatmetricException {
        // Transform the list in messages to be acknowledged
        List<AcknowledgedMessage> toProcess = process(items);
        synchronized (this) {
            for (AcknowledgedMessage am : toProcess) {
                unacknowledgedMessages.put(am.getInternalId(), am);
            }
        }
        storeAndNotify(toProcess);
    }

    private void storeAndNotify(List<AcknowledgedMessage> toProcess) throws ArchiveException {
        if(archive != null && !toProcess.isEmpty()) {
            archive.store(toProcess);
        }
        for(AcknowledgedMessageSubscriptionManager s : subscribers) {
            try {
                s.notifyItems(toProcess);
            } catch (Exception e) {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Exception when notifying subscriber on acknowledged message broker", e);
                }
            }
        }
    }

    private List<AcknowledgedMessage> process(List<OperationalMessage> items) {
        List<AcknowledgedMessage> toReturn = new ArrayList<>(items.size());
        for(OperationalMessage om : items) {
            if(toBeAcknowledged(om)) {
                IUniqueId idToAssign = nextOperationalMessageId();
                AcknowledgedMessage toAck = new AcknowledgedMessage(idToAssign, om.getGenerationTime(), om, AcknowledgementState.PENDING, null, null, null);
                toReturn.add(toAck);
            }
        }
        return toReturn;
    }

    private boolean toBeAcknowledged(OperationalMessage om) {
        return om.getSeverity() == Severity.ALARM || om.getSeverity() == Severity.WARN || om.getSeverity() == Severity.ERROR;
    }

    @Override
    public void unsubscribe(IAcknowledgedMessageSubscriber subscriber) {
        AcknowledgedMessageSubscriptionManager manager = this.subscriberIndex.remove(subscriber);
        if(manager != null) {
            this.subscribers.remove(manager);
            manager.terminate();
        }
    }

    private IUniqueId nextOperationalMessageId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    // Internal method for the AcknowledgementServiceImpl
    void internalAcknowledgeMessage(AcknowledgedMessage message, String user) throws ReatmetricException{
        internalAcknowledgeMessages(Collections.singletonList(message), user);
    }

    // Internal method for the AcknowledgementServiceImpl
    void internalAcknowledgeMessages(Collection<AcknowledgedMessage> messages, String user) throws ReatmetricException {
        List<AcknowledgedMessage> toStore = new LinkedList<>();
        synchronized (this) {
            for (AcknowledgedMessage am : messages) {
                AcknowledgedMessage toBeAcknowledged = unacknowledgedMessages.remove(am.getInternalId());
                if (toBeAcknowledged != null) {
                    toBeAcknowledged = toBeAcknowledged.ack(user);
                    toStore.add(toBeAcknowledged);
                }
            }
        }
        // Now store and notify
        storeAndNotify(toStore);
    }

    public IAcknowledgementService getAcknowledgementService() {
        return acknowledgementMessageService;
    }

    private static class AcknowledgedMessageSubscriptionManager {

        private static final Predicate<AcknowledgedMessage> IDENTITY_FILTER = (o) -> true;

        private final ExecutorService dispatcher = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Acknowledged Message Subscription Dispatcher");
            t.setDaemon(true);
            return t;
        }); // OK to use, only 1 job, separate queue
        private final BlockingQueue<AcknowledgedMessage> queue = new LinkedBlockingQueue<>();
        private final IAcknowledgedMessageSubscriber subscriber;
        private final boolean timely;
        private volatile Predicate<AcknowledgedMessage> filter;
        private final AcknowledgedMessageBrokerImpl broker;

        public AcknowledgedMessageSubscriptionManager(AcknowledgedMessageBrokerImpl broker, IAcknowledgedMessageSubscriber subscriber, AcknowledgedMessageFilter filter, boolean timely, List<AcknowledgedMessage> initialMessages) {
            this.broker = broker;
            this.subscriber = subscriber;
            this.filter = filter == null ? IDENTITY_FILTER : filter;
            this.timely = timely;
            this.queue.addAll(initialMessages);
            this.dispatcher.submit(this::processQueue);
        }

        private void processQueue() {
            List<AcknowledgedMessage> drainer = new ArrayList<>(1000);
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
                List<AcknowledgedMessage> toNotify = filterItems(drainer);
                if(!toNotify.isEmpty()) {
                    try {
                        subscriber.dataItemsReceived(toNotify);
                    } catch (RemoteException e) {
                        LOG.log(Level.SEVERE, "Cannot notify subscriber, terminating...", e);
                        broker.unsubscribe(subscriber);
                    }
                }
                drainer.clear();
            }
        }

        public void notifyItems(List<AcknowledgedMessage> messages) {
            if(dispatcher.isShutdown()) {
                // Ignore
                return;
            }
            synchronized (queue) {
                if (timely && !queue.isEmpty() && messages.size() > queue.remainingCapacity()) {
                    queue.clear();
                }
                queue.addAll(messages);
                queue.notifyAll();
            }
        }

        private List<AcknowledgedMessage> filterItems(List<AcknowledgedMessage> items) {
            Predicate<AcknowledgedMessage> theFilter = filter;
            return items.stream().filter(theFilter).collect(Collectors.toList());
        }

        public void update(AcknowledgedMessageFilter filter) {
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
