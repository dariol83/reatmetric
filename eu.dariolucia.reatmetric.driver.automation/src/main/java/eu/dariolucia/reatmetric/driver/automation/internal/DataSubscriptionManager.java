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

    public DataSubscriptionManager(IActivityOccurrenceDataProvisionService activityDataService, IEventDataProvisionService eventDataService, IParameterDataProvisionService parameterService) {
        this.activityDataService = activityDataService;
        this.eventService = eventDataService;
        this.parameterService = parameterService;
    }

    synchronized void subscribe(IEventDataSubscriber s, int externalId) {
        boolean alreadyRegistered = true;
        List<IEventDataSubscriber> subs = currentEventSubscribers.get(externalId);
        if(subs == null) {
            subs = new ArrayList<>();
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
    }

    synchronized void subscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
        boolean alreadyRegistered = true;
        List<IActivityOccurrenceDataSubscriber> subs = currentActivitySubscribers.get(externalId);
        if(subs == null) {
            subs = new ArrayList<>();
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
    }

    synchronized void subscribe(IParameterDataSubscriber s, int externalId) {
        boolean alreadyRegistered = true;
        List<IParameterDataSubscriber> subs = currentParameterSubscribers.get(externalId);
        if(subs == null) {
            subs = new ArrayList<>();
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
    }

    synchronized void unsubscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
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
    }

    synchronized void unsubscribe(IEventDataSubscriber s, int externalId) {
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
    }

    synchronized void unsubscribe(IParameterDataSubscriber s, int externalId) {
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
    }

    public synchronized void processParameters(List<ParameterData> dataItems) {
        for(ParameterData item : dataItems) {
            List<ParameterData> singleItem = Collections.singletonList(item);
            List<IParameterDataSubscriber> subs = currentParameterSubscribers.get(item.getExternalId());
            if(subs != null) {
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

    public synchronized void processEvents(List<EventData> dataItems) {
        for(EventData item : dataItems) {
            List<EventData> singleItem = Collections.singletonList(item);
            List<IEventDataSubscriber> subs = currentEventSubscribers.get(item.getExternalId());
            if(subs != null) {
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

    public synchronized void processActivities(List<ActivityOccurrenceData> dataItems) {
        for(ActivityOccurrenceData item : dataItems) {
            List<ActivityOccurrenceData> singleItem = Collections.singletonList(item);
            List<IActivityOccurrenceDataSubscriber> subs = currentActivitySubscribers.get(item.getExternalId());
            if(subs != null) {
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
}
