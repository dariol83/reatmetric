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

package eu.dariolucia.reatmetric.remoting.connector.proxy;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulerProxy extends AbstractStateProvisionServiceProxy<ScheduledActivityData, ScheduledActivityDataFilter, IScheduledActivityDataSubscriber, IScheduler> implements IScheduler {

    private final Map<ISchedulerSubscriber, Remote> scheduleSubscriber2remote = new ConcurrentHashMap<>();

    public SchedulerProxy(IScheduler delegate) {
        super(delegate);
    }

    public void initialise(boolean schedulerEnabled) {
        // Not from remote
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    public void subscribe(ISchedulerSubscriber subscriber) throws RemoteException {
        Remote activeObject = scheduleSubscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = UnicastRemoteObject.exportObject(subscriber, 0);
            scheduleSubscriber2remote.put(subscriber, activeObject);
        }
        delegate.subscribe((ISchedulerSubscriber) activeObject);
    }

    public void unsubscribe(ISchedulerSubscriber subscriber) throws RemoteException {
        Remote activeObject = scheduleSubscriber2remote.remove(subscriber);
        if(activeObject == null) {
            return;
        }
        delegate.unsubscribe((ISchedulerSubscriber) activeObject);
        try {
            UnicastRemoteObject.unexportObject(activeObject, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    public void enable() throws SchedulingException, RemoteException {
        delegate.enable();
    }

    public void disable() throws SchedulingException, RemoteException {
        delegate.disable();
    }

    public boolean isEnabled() throws SchedulingException, RemoteException {
        return delegate.isEnabled();
    }

    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException {
        return delegate.schedule(request, conflictStrategy);
    }

    public List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException {
        return delegate.schedule(request, conflictStrategy);
    }

    public List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException, RemoteException {
        return delegate.getCurrentScheduledActivities();
    }

    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException {
        return delegate.update(originalId, newRequest, conflictStrategy);
    }

    public void remove(IUniqueId scheduledId) throws SchedulingException, RemoteException {
        delegate.remove(scheduledId);
    }

    public void remove(ScheduledActivityDataFilter filter) throws SchedulingException, RemoteException {
        delegate.remove(filter);
    }

    public List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException {
        return delegate.load(startTime, endTime, requests, source, conflictStrategy);
    }

    public void dispose() {
        // Not from remote
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    @Override
    public void terminate() {
        // Unsubscribe all remotes
        for(Remote r : scheduleSubscriber2remote.values()) {
            try {
                delegate.unsubscribe((ISchedulerSubscriber) r);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                UnicastRemoteObject.unexportObject(r, true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
            }
        }
        super.terminate();
    }
}
