/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class ParameterDataAccessSubscriber {

    private final IParameterDataSubscriber subscriber;
    private final BlockingQueue<ParameterData> queue;
    private final Thread managerThread;
    private final IProcessingModel model;
    private volatile ParameterDataFilter filter;

    private volatile boolean running;

    public ParameterDataAccessSubscriber(IParameterDataSubscriber subscriber, ParameterDataFilter filter, IProcessingModel model) {
        this.subscriber = subscriber;
        this.filter = filter;
        this.model = model;
        this.queue = new LinkedBlockingQueue<>();
        running = true;
        managerThread = new Thread(this::runDistribution);
        managerThread.setName("Parameter Data Access Subscription Thread");
        managerThread.setDaemon(true);
        managerThread.start();
    }

    private void runDistribution() {
        boolean initialiseFromModel = true;
        boolean firstInitialisation = false;
        Map<Integer, IUniqueId> lastDelivered = new HashMap<>();
        List<ParameterData> toDistribute = new LinkedList<>();
        while(running) {
            ParameterDataFilter theFilter = filter;
            // And here we start first by getting the current values subject to filter, by retrieving them from the model and
            // distributing them
            if(initialiseFromModel) {
                lastDelivered.clear();
                List<ParameterData> initialItems = (List<ParameterData>) (List) model.get(theFilter);
                // Remember what you are sending
                for(ParameterData pd : initialItems) {
                    lastDelivered.put(pd.getExternalId(), pd.getInternalId());
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
                    // TODO: to be evaluated if a reset of the subscription (i.e. with retrieval from model) is needed
                    initialiseFromModel = true;
                    continue;
                }
            }

            if(firstInitialisation) {
                for(Iterator<ParameterData> it = toDistribute.iterator(); it.hasNext();) {
                    ParameterData pd = it.next();
                    IUniqueId lastDeliveredId = lastDelivered.get(pd.getExternalId());
                    if(lastDeliveredId != null && lastDeliveredId.asLong() >= pd.getInternalId().asLong()) {
                        it.remove();
                    }
                }
                firstInitialisation = false;
            }
            // Distribute the elements
            subscriber.dataItemsReceived(toDistribute);

            // TODO: to be evaluated if a reset of the subscription (i.e. with retrieval from model) is needed
            if(theFilter != filter) {
                initialiseFromModel = true;
            }
        }
    }

    public void notifyItems(List<ParameterData> toDistribute) {
        ParameterDataFilter theFilter = filter;
        if(theFilter != null && !theFilter.isClear()) {
            toDistribute = toDistribute.stream().filter(theFilter).collect(Collectors.toList());
        }
        synchronized (queue) {
            queue.addAll(toDistribute);
            queue.notifyAll();
        }
    }

    public void update(ParameterDataFilter filter) {
        this.filter = filter;
    }

    public void terminate() {
        this.running = false;
        synchronized (queue) {
            queue.notifyAll();
        }
    }
}
