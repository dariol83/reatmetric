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

import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActivitySubscriptionManager implements IActivityOccurrenceDataSubscriber {

    private static final Logger LOG = Logger.getLogger(ActivitySubscriptionManager.class.getName());

    private final Map<Integer, List<IActivityOccurrenceDataSubscriber>> currentSubscribers = new HashMap<>();
    private final IActivityOccurrenceDataProvisionService service;

    public ActivitySubscriptionManager(IActivityOccurrenceDataProvisionService service) {
        this.service = service;
    }

    synchronized void subscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
        boolean alreadyRegistered = true;
        List<IActivityOccurrenceDataSubscriber> subs = currentSubscribers.get(externalId);
        if(subs == null) {
            subs = new ArrayList<>();
            currentSubscribers.put(externalId, subs);
            alreadyRegistered = false;
        }
        subs.add(s);
        if(!alreadyRegistered) {
            try {
                service.subscribe(this, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, new ArrayList<>(currentSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe to activity occurrence provision service: " + e.getMessage(), e);
            }
        }
    }

    synchronized void unsubscribe(IActivityOccurrenceDataSubscriber s, int externalId) {
        boolean externalIdRemoved = false;
        List<IActivityOccurrenceDataSubscriber> subs = currentSubscribers.get(externalId);
        if(subs != null) {
            subs.remove(s);
            if(subs.isEmpty()) {
                currentSubscribers.remove(externalId);
                externalIdRemoved = true;
            }
        }
        if(currentSubscribers.isEmpty()) {
            try {
                service.unsubscribe(this);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot unsubscribe to activity occurrence provision service: " + e.getMessage(), e);
            }
        } else if(externalIdRemoved) {
            try {
                service.subscribe(this, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, new ArrayList<>(currentSubscribers.keySet())));
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Cannot subscribe (after unsubscription) to activity occurrence provision service: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public synchronized void dataItemsReceived(List<ActivityOccurrenceData> dataItems) throws RemoteException {
        for(ActivityOccurrenceData item : dataItems) {
            List<ActivityOccurrenceData> singleItem = Collections.singletonList(item);
            List<IActivityOccurrenceDataSubscriber> subs = currentSubscribers.get(item.getExternalId());
            if(subs != null) {
                subs.forEach(o -> {
                    try {
                        o.dataItemsReceived(singleItem);
                    } catch (RemoteException e) {
                        e.printStackTrace(); // TODO
                    }
                });
            }
        }
    }
}
