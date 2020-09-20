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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransportConnectorProxy implements ITransportConnector {

    private final ITransportConnector delegate;

    private final Map<ITransportSubscriber, Remote> subscriber2remote = new ConcurrentHashMap<>();

    private volatile String cachedName;
    private volatile String cachedDescription;
    private volatile Map<String, Pair<String, ValueTypeEnum>> cachedProperties;

    public TransportConnectorProxy(ITransportConnector delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() throws RemoteException {
        if(cachedName == null) {
            cachedName = delegate.getName();
        }
        return cachedName;
    }

    @Override
    public String getDescription() throws RemoteException {
        if(cachedDescription == null) {
            cachedDescription = delegate.getDescription();
        }
        return cachedDescription;
    }

    @Override
    public TransportConnectionStatus getConnectionStatus() throws RemoteException {
        return delegate.getConnectionStatus();
    }

    @Override
    public void prepare() {
        // Not to be called
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    @Override
    public boolean isInitialised() throws RemoteException {
        return delegate.isInitialised();
    }

    @Override
    public void initialise(Map<String, Object> properties) throws TransportException, RemoteException {
        delegate.initialise(properties);
    }

    @Override
    public void connect() throws TransportException, RemoteException {
        delegate.connect();
    }

    @Override
    public void disconnect() throws TransportException, RemoteException {
        delegate.disconnect();
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        delegate.abort();
    }

    @Override
    public void dispose() {
        // Not to be called
        throw new UnsupportedOperationException("Not to be called from remote");
    }

    @Override
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() throws RemoteException {
        if(cachedProperties == null) {
            cachedProperties = delegate.getSupportedProperties();
        }
        return cachedProperties;
    }

    @Override
    public Map<String, Object> getCurrentProperties() throws RemoteException {
        return delegate.getCurrentProperties();
    }

    @Override
    public void register(ITransportSubscriber subscriber) throws RemoteException {
        Remote activeObject = subscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = UnicastRemoteObject.exportObject(subscriber, 0);
            subscriber2remote.put(subscriber, activeObject);
        }
        delegate.register((ITransportSubscriber) activeObject);
    }

    @Override
    public void deregister(ITransportSubscriber subscriber) throws RemoteException {
        Remote activeObject = subscriber2remote.remove(subscriber);
        if(activeObject == null) {
            return;
        }
        delegate.deregister((ITransportSubscriber) activeObject);
        try {
            UnicastRemoteObject.unexportObject(activeObject, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    public void terminate() {
        // Unsubscribe all remotes
        for(Remote r : subscriber2remote.values()) {
            try {
                delegate.deregister((ITransportSubscriber) r);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                UnicastRemoteObject.unexportObject(r, true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
            }
        }
    }
}