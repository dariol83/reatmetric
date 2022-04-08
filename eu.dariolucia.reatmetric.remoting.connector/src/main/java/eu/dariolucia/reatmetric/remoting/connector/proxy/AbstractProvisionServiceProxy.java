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

import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractProvisionServiceProxy<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>, U extends IDataItemSubscriber<T>, V extends IDataItemProvisionService<U, K, T>> implements IDataItemProvisionService<U, K, T> {

    private static final Logger LOG = Logger.getLogger(AbstractProvisionServiceProxy.class.getName());

    protected final V delegate;

    private final Map<U, Remote> subscriber2remote = new ConcurrentHashMap<>();

    private final String localAddress;

    public AbstractProvisionServiceProxy(V delegate, String localAddress) {
        this.delegate = delegate;
        this.localAddress = localAddress;
    }

    protected String getLocalAddress() {
        return localAddress;
    }

    @Override
    public void subscribe(U subscriber, K filter) throws RemoteException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Registering subscriber " + subscriber + " to proxy " + getClass().getSimpleName());
        }
        Remote activeObject = subscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = ObjectActivationCache.instance().activate(subscriber, this.localAddress,0);
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Subscriber active object " + activeObject + " for " + subscriber + " to proxy " + getClass().getSimpleName() + " activated");
            }
            subscriber2remote.put(subscriber, activeObject);
        }
        delegate.subscribe((U) activeObject, filter);
    }

    @Override
    public void unsubscribe(U subscriber) throws RemoteException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Unregistering subscriber " + subscriber + " from proxy " + getClass().getSimpleName());
        }
        Remote activeObject = subscriber2remote.remove(subscriber);
        if(activeObject == null) {
            return;
        }
        try {
            delegate.unsubscribe((U) activeObject);
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

    @Override
    public List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ReatmetricException, RemoteException {
        return delegate.retrieve(startTime, numRecords, direction, filter);
    }

    @Override
    public List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ReatmetricException, RemoteException {
        return delegate.retrieve(startItem, numRecords, direction, filter);
    }

    @Override
    public List<T> retrieve(Instant startTime, Instant endTime, K filter) throws ReatmetricException, RemoteException {
        return delegate.retrieve(startTime, endTime, filter);
    }

    public void terminate() {
        // Unsubscribe all remotes
        for(Map.Entry<U, Remote> entry : subscriber2remote.entrySet()) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Terminating subscriber " + entry.getKey() + " from proxy " + getClass().getSimpleName());
            }
            try {
                delegate.unsubscribe((U) entry.getValue());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot unsubscribe " + entry.getKey() + " in proxy " + getClass().getSimpleName(), e);
            } finally {
                try {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Terminating subscriber active object " + entry.getValue() + " for " + entry.getKey() + " in proxy " + getClass().getSimpleName());
                    }
                    ObjectActivationCache.instance().deactivate(entry.getKey(), true);
                } catch (NoSuchObjectException e) {
                    // Ignore
                }
            }
        }
        subscriber2remote.clear();
    }
}
