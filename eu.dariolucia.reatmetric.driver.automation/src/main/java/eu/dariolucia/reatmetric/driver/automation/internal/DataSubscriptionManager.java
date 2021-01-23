/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.automation.internal;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceDataFilter;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataSubscriber;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataSubscriptionManager {

    private static final Logger LOG = Logger.getLogger(DataSubscriptionManager.class.getName());

    private final Map<Integer, List<IActivityOccurrenceDataSubscriber>> currentActivitySubscribers = new HashMap<>();
    private final IActivityOccurrenceDataProvisionService activityDataService;

    private final Map<Integer, List<IEventDataSubscriber>> currentEventSubscribers = new HashMap<>();
    private final IEventDataProvisionService eventService;

    private final Map<Integer, List<IParameterDataSubscriber>> currentParameterSubscribers = new HashMap<>();
    private final IParameterDataProvisionService parameterService;

    private final IEventDataSubscriber eventDataSubscriber = this::processEvents;

    private final IActivityOccurrenceDataSubscriber activityOccurrenceDataSubscriber = this::processActivities;

    private final IParameterDataSubscriber parameterDataSubscriber = this::processParameters;

    private final ExecutorService invocationDispatcher = Executors.newFixedThreadPool(1, t -> {
       Thread thr = new Thread(t);
       thr.setName("Automation - Data Subscription Manager - Dispatcher");
       thr.setDaemon(true);
       return thr;
    });

    public DataSubscriptionManager(IActivityOccurrenceDataProvisionService activityDataService, IEventDataProvisionService eventDataService, IParameterDataProvisionService parameterService) {
        this.activityDataService = activityDataService;
        this.eventService = eventDataService;
        this.parameterService = parameterService;
    }

    synchronized void subscribe(IEventDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IEventDataSubscriber) - External ID " + externalId + ": invoked");
        boolean alreadyRegistered = true;
        List<IEventDataSubscriber> subs = currentEventSubscribers.get(externalId);
        if(subs == null) {
            subs = new CopyOnWriteArrayList<>();
            currentEventSubscribers.put(externalId, subs);
            alreadyRegistered = false;
        }
        subs.add(s);
        if(!alreadyRegistered) {
            try {
                eventService.subscribe(eventDataSubscriber, new EventDataFilter(null, null, null, null, null, null, new ArrayList<>(currentEventSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe to event provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IEventDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    synchronized void subscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IActivityOccurrenceDataSubscriber) - External ID " + externalId + ": invoked");
        boolean alreadyRegistered = true;
        List<IActivityOccurrenceDataSubscriber> subs = currentActivitySubscribers.get(externalId);
        if(subs == null) {
            subs = new CopyOnWriteArrayList<>();
            currentActivitySubscribers.put(externalId, subs);
            alreadyRegistered = false;
        }
        subs.add(s);
        if(!alreadyRegistered) {
            try {
                activityDataService.subscribe(activityOccurrenceDataSubscriber, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, new ArrayList<>(currentActivitySubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe to activity occurrence provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IActivityOccurrenceDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    synchronized void subscribe(IParameterDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IParameterDataSubscriber) - External ID " + externalId + ": invoked");
        boolean alreadyRegistered = true;
        List<IParameterDataSubscriber> subs = currentParameterSubscribers.get(externalId);
        if(subs == null) {
            subs = new CopyOnWriteArrayList<>();
            currentParameterSubscribers.put(externalId, subs);
            alreadyRegistered = false;
        }
        subs.add(s);
        if(!alreadyRegistered) {
            try {
                parameterService.subscribe(parameterDataSubscriber, new ParameterDataFilter(null, null, null, null, null, new ArrayList<>(currentParameterSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe to activity occurrence provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "subscribe(IParameterDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    synchronized void unsubscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IActivityOccurrenceDataSubscriber) - External ID " + externalId + ": invoked");
        boolean externalIdRemoved = false;
        List<IActivityOccurrenceDataSubscriber> subs = currentActivitySubscribers.get(externalId);
        if(subs != null) {
            subs.remove(s);
            if(subs.isEmpty()) {
                currentActivitySubscribers.remove(externalId);
                externalIdRemoved = true;
            }
        }
        if(currentActivitySubscribers.isEmpty()) {
            try {
                activityDataService.unsubscribe(activityOccurrenceDataSubscriber);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot unsubscribe to activity occurrence provision service: " + e.getMessage(), e);
            }
        } else if(externalIdRemoved) {
            try {
                activityDataService.subscribe(activityOccurrenceDataSubscriber, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, new ArrayList<>(currentActivitySubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe (after unsubscription) to activity occurrence provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IActivityOccurrenceDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    synchronized void unsubscribe(IEventDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IEventDataSubscriber) - External ID " + externalId + ": invoked");
        boolean externalIdRemoved = false;
        List<IEventDataSubscriber> subs = currentEventSubscribers.get(externalId);
        if(subs != null) {
            subs.remove(s);
            if(subs.isEmpty()) {
                currentEventSubscribers.remove(externalId);
                externalIdRemoved = true;
            }
        }
        if(currentEventSubscribers.isEmpty()) {
            try {
                eventService.unsubscribe(eventDataSubscriber);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot unsubscribe to event provision service: " + e.getMessage(), e);
            }
        } else if(externalIdRemoved) {
            try {
                eventService.subscribe(eventDataSubscriber, new EventDataFilter(null, null, null, null, null, null, new ArrayList<>(currentEventSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe (after unsubscription) to event provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IEventDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    synchronized void unsubscribe(IParameterDataSubscriber s, int externalId) {
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IParameterDataSubscriber) - External ID " + externalId + ": invoked");
        boolean externalIdRemoved = false;
        List<IParameterDataSubscriber> subs = currentParameterSubscribers.get(externalId);
        if(subs != null) {
            subs.remove(s);
            if(subs.isEmpty()) {
                currentParameterSubscribers.remove(externalId);
                externalIdRemoved = true;
            }
        }
        if(currentParameterSubscribers.isEmpty()) {
            try {
                parameterService.unsubscribe(parameterDataSubscriber);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot unsubscribe to event provision service: " + e.getMessage(), e);
            }
        } else if(externalIdRemoved) {
            try {
                parameterService.subscribe(parameterDataSubscriber, new ParameterDataFilter(null, null, null, null, null, new ArrayList<>(currentParameterSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe (after unsubscription) to event provision service: " + e.getMessage(), e);
            }
        }
        LOG.fine(Thread.currentThread().getName() + " || " + "unsubscribe(IParameterDataSubscriber) - External ID " + externalId + ": invocation end");
    }

    public void processParameters(List<ParameterData> dataItems) {
        invocationDispatcher.execute(() -> {
            LOG.fine(Thread.currentThread().getName() + " || " + "processParameters() - dataItems.size() " + dataItems.size() + ": invoked");
            synchronized (DataSubscriptionManager.this) {
                for (ParameterData item : dataItems) {
                    List<ParameterData> singleItem = Collections.singletonList(item);
                    List<IParameterDataSubscriber> subs = currentParameterSubscribers.get(item.getExternalId());
                    if (subs != null) {
                        subs.forEach(o -> {
                            try {
                                o.dataItemsReceived(singleItem);
                            } catch (RemoteException e) {
                                LOG.log(Level.WARNING, "Remote exception when notifying automation procedure subscriber for parameters: " + e.getMessage(), e);
                            }
                        });
                    }
                }
            }
            LOG.fine(Thread.currentThread().getName() + " || " + "processParameters() - dataItems.size() " + dataItems.size() + ": invocation end");
        });
    }

    public void processEvents(List<EventData> dataItems) {
        invocationDispatcher.execute(() -> {
            LOG.fine(Thread.currentThread().getName() + " || " + "processEvents() - dataItems.size() " + dataItems.size() + ": invoked");
            synchronized (DataSubscriptionManager.this) {
                for (EventData item : dataItems) {
                    List<EventData> singleItem = Collections.singletonList(item);
                    List<IEventDataSubscriber> subs = currentEventSubscribers.get(item.getExternalId());
                    if (subs != null) {
                        subs.forEach(o -> {
                            try {
                                o.dataItemsReceived(singleItem);
                            } catch (RemoteException e) {
                                LOG.log(Level.WARNING, "Remote exception when notifying automation procedure subscriber for events: " + e.getMessage(), e);
                            }
                        });
                    }
                }
            }
            LOG.fine(Thread.currentThread().getName() + " || " + "processEvents() - dataItems.size() " + dataItems.size() + ": invocation end");
        });
    }

    public synchronized void processActivities(List<ActivityOccurrenceData> dataItems) {
        invocationDispatcher.execute(() -> {
            LOG.fine(Thread.currentThread().getName() + " || " + "processActivities() - dataItems.size() " + dataItems.size() + ": invoked");
            synchronized (DataSubscriptionManager.this) {
                for (ActivityOccurrenceData item : dataItems) {
                    List<ActivityOccurrenceData> singleItem = Collections.singletonList(item);
                    List<IActivityOccurrenceDataSubscriber> subs = currentActivitySubscribers.get(item.getExternalId());
                    if (subs != null) {
                        subs.forEach(o -> {
                            try {
                                o.dataItemsReceived(singleItem);
                            } catch (RemoteException e) {
                                LOG.log(Level.WARNING, "Remote exception when notifying automation procedure subscriber for activities: " + e.getMessage(), e);
                            }
                        });
                    }
                }
            }
            LOG.fine(Thread.currentThread().getName() + " || " + "processActivities() - dataItems.size() " + dataItems.size() + ": invocation end");
        });
    }
}
