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

package eu.dariolucia.reatmetric.remoting.stubs;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.rmi.RemoteException;
import java.util.Map;

public class TransportConnectorDelegate implements ITransportConnector {

    private final ITransportConnector delegate;

    public TransportConnectorDelegate(ITransportConnector delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() throws RemoteException {
        return delegate.getName();
    }

    @Override
    public String getDescription() throws RemoteException {
        return delegate.getDescription();
    }

    @Override
    public TransportConnectionStatus getConnectionStatus() throws RemoteException {
        return delegate.getConnectionStatus();
    }

    @Override
    public void prepare() throws RemoteException {
        delegate.prepare();
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
    public void dispose() throws RemoteException {
        delegate.dispose();
    }

    @Override
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() throws RemoteException {
        return delegate.getSupportedProperties();
    }

    @Override
    public Map<String, Object> getCurrentProperties() throws RemoteException {
        return delegate.getCurrentProperties();
    }

    @Override
    public void register(ITransportSubscriber listener) throws RemoteException {
        delegate.register(listener);
    }

    @Override
    public void deregister(ITransportSubscriber listener) throws RemoteException {
        delegate.deregister(listener);
    }
}
