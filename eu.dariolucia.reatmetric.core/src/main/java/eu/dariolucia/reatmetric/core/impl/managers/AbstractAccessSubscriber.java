/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractAccessSubscriber<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>, J extends IDataItemSubscriber<T>> {

    private static final Logger LOG = Logger.getLogger(AbstractAccessSubscriber.class.getName());

    private final J subscriber;
    private final BlockingQueue<T> queue;
    private final Thread managerThread;
    private final IProcessingModel model;
    private volatile K filter;

    private volatile boolean running;

    public AbstractAccessSubscriber(J subscriber, K filter, IProcessingModel model) {
        this.subscriber = subscriber;
        this.filter = filter;
        this.model = model;
        this.queue = new LinkedBlockingQueue<>();
        running = true;
        managerThread = new Thread(this::runDistribution);
        managerThread.setName(getName() + " - " + subscriber + " subscription thread");
        managerThread.setDaemon(true);
        managerThread.start();
    }


    private void runDistribution() {
        boolean initialiseFromModel = true;
        boolean firstInitialisation = false;
        Map<Pair<Integer, Long>, IUniqueId> lastDelivered = new HashMap<>();

        while(running) {
            K theFilter = filter;
            // And here we start first by getting the current values subject to filter, by retrieving them from the model and
            // distributing them
            if(initialiseFromModel) {
                lastDelivered.clear();
                List<T> initialItems = (List<T>) model.get(theFilter);
                // Remember what you are sending
                for(T pd : initialItems) {
                    lastDelivered.put(computeId(pd), computeUniqueCounter(pd));
                }
                // Deliver
                subscriber.dataItemsReceived(initialItems);
                initialiseFromModel = false;
                firstInitialisation = true;
            }
            // Once the initial distribution is done, all the items currently in the queue must be verified:
            // for each element in the queue that we distribute, we check if we have to deliver the data item (i.e. if the unique id of the item
            // is greater than the one in the lastDelivered map). If no delivery is needed, then the item is discarded,
            // if delivery is needed, the map is updated and the item is not discarded. This algorithm is used only immediately after
            // the first initialisation.

            // Wait to have elements in the queue
            List<T> toDistribute = new LinkedList<>();
            synchronized (queue) {
                while(queue.isEmpty() && running && theFilter == filter) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                // If the queue is still empty, repeat the outer cycle
                if(queue.isEmpty()) {
                    continue;
                } else if(theFilter == filter) {
                    // If you are here, there are elements in the queue: so drain them
                    queue.drainTo(toDistribute);
                } else {
                    // Filter updated in the meantime, repeat the outer cycle
                    // XXX: to be evaluated if a reset of the subscription (i.e. with retrieval from model) is needed. So far this is enabled.
                    initialiseFromModel = true;
                    continue;
                }
            }

            if(firstInitialisation) {
                for(Iterator<T> it = toDistribute.iterator(); it.hasNext();) {
                    T pd = it.next();
                    IUniqueId lastDeliveredId = lastDelivered.get(computeId(pd));
                    if(lastDeliveredId != null && lastDeliveredId.asLong() >= pd.getInternalId().asLong()) {
                        it.remove();
                    }
                }
                firstInitialisation = false;
            }
            // Distribute the elements
            subscriber.dataItemsReceived(toDistribute);

            // XXX: to be evaluated if a reset of the subscription (i.e. with retrieval from model) is needed. So far this is enabled.
            if(theFilter != filter) {
                initialiseFromModel = true;
            }
        }
    }

    protected IUniqueId computeUniqueCounter(T pd) {
        return pd.getInternalId();
    }

    public void notifyItems(List<T> toDistribute) {
        if(!running) {
            return;
        }
        K theFilter = filter;
        if(theFilter != null && !theFilter.isClear()) {
            toDistribute = toDistribute.stream().filter(theFilter).collect(Collectors.toList());
        }
        if(!toDistribute.isEmpty()) {
            synchronized (queue) {
                queue.addAll(toDistribute);
                queue.notifyAll();
            }
        }
    }

    public void update(K filter) {
        this.filter = filter;
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public void terminate() {
        this.running = false;
        synchronized (queue) {
            queue.notifyAll();
        }
        try {
            this.managerThread.join();
        } catch (InterruptedException e) {
            LOG.log(Level.FINE, "Interrupted while waiting for termination of access subscriber " + getName() + " for inner subscriber " + subscriber);
        }
    }

    protected abstract Pair<Integer, Long> computeId(T item);

    protected abstract String getName();
}
