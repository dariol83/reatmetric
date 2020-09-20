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

public abstract class AbstractProvisionServiceProxy<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>, U extends IDataItemSubscriber<T>, V extends IDataItemProvisionService<U, K, T>> implements IDataItemProvisionService<U, K, T> {

    protected final V delegate;

    private final Map<U, Remote> subscriber2remote = new ConcurrentHashMap<>();

    public AbstractProvisionServiceProxy(V delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(U subscriber, K filter) throws RemoteException {
        Remote activeObject = subscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = UnicastRemoteObject.exportObject(subscriber, 0);
            subscriber2remote.put(subscriber, activeObject);
        }
        delegate.subscribe((U) activeObject, filter);
    }

    @Override
    public void unsubscribe(U subscriber) throws RemoteException {
        Remote activeObject = subscriber2remote.remove(subscriber);
        if(activeObject == null) {
            return;
        }
        delegate.unsubscribe((U) activeObject);
        try {
            UnicastRemoteObject.unexportObject(activeObject, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
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

    public void terminate() {
        // Unsubscribe all remotes
        for(Remote r : subscriber2remote.values()) {
            try {
                delegate.unsubscribe((U) r);
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
