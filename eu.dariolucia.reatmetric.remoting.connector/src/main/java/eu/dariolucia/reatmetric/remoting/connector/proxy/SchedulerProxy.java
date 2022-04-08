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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchedulerProxy extends AbstractStateProvisionServiceProxy<ScheduledActivityData, ScheduledActivityDataFilter, IScheduledActivityDataSubscriber, IScheduler> implements IScheduler {

    private static final Logger LOG = Logger.getLogger(SchedulerProxy.class.getName());

    private final Map<ISchedulerSubscriber, Remote> scheduleSubscriber2remote = new ConcurrentHashMap<>();

    public SchedulerProxy(IScheduler delegate) {
        super(delegate);
    }

    @Override
    public void initialise() {
        // Not from remote
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    public void subscribe(ISchedulerSubscriber subscriber) throws RemoteException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Registering subscriber " + subscriber + " to proxy " + getClass().getSimpleName());
        }
        Remote activeObject = scheduleSubscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = ObjectActivationCache.instance().activate(subscriber, 0);
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Subscriber active object " + activeObject + " for " + subscriber + " to proxy " + getClass().getSimpleName() + " activated");
            }
            scheduleSubscriber2remote.put(subscriber, activeObject);
        }
        delegate.subscribe((ISchedulerSubscriber) activeObject);
    }

    public void unsubscribe(ISchedulerSubscriber subscriber) throws RemoteException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Unregistering subscriber " + subscriber + " from proxy " + getClass().getSimpleName());
        }
        Remote activeObject = scheduleSubscriber2remote.remove(subscriber);
        if(activeObject == null) {
            return;
        }
        try {
            delegate.unsubscribe((ISchedulerSubscriber) activeObject);
        } finally {
            try {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Deactivating subscriber active object " + activeObject + " for " + subscriber + " in proxy " + getClass().getSimpleName());
                }
                ObjectActivationCache.instance().deactivate(activeObject, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
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

    @Override
    public void enableBot(String name) throws SchedulingException, RemoteException {
        delegate.enableBot(name);
    }

    @Override
    public void disableBot(String name) throws SchedulingException, RemoteException {
        delegate.disableBot(name);
    }

    public void dispose() {
        // Not from remote
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    @Override
    public void terminate() {
        // Unsubscribe all remotes
        for(Map.Entry<ISchedulerSubscriber, Remote> entry : scheduleSubscriber2remote.entrySet()) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Terminating subscriber active object " + entry.getKey() + " from proxy " + getClass().getSimpleName());
            }
            try {
                delegate.unsubscribe((ISchedulerSubscriber) entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Terminating subscriber active object " + entry.getKey() + " in proxy " + getClass().getSimpleName());
                    }
                    ObjectActivationCache.instance().deactivate(entry.getKey(), true);
                } catch (NoSuchObjectException e) {
                    // Ignore
                }
            }
        }
        scheduleSubscriber2remote.clear();

        super.terminate();
    }
}
