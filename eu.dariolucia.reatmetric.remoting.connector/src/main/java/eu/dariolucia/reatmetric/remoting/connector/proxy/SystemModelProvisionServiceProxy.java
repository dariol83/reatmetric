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

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelSubscriber;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemModelProvisionServiceProxy implements ISystemModelProvisionService {

    protected final ISystemModelProvisionService delegate;

    private final Map<ISystemModelSubscriber, Remote> subscriber2remote = new ConcurrentHashMap<>();

    public SystemModelProvisionServiceProxy(ISystemModelProvisionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(ISystemModelSubscriber subscriber) throws RemoteException {
        Remote activeObject = subscriber2remote.get(subscriber);
        if(activeObject == null) {
            activeObject = UnicastRemoteObject.exportObject(subscriber, 0);
            subscriber2remote.put(subscriber, activeObject);
        }
        delegate.subscribe((ISystemModelSubscriber) activeObject);
    }

    @Override
    public void unsubscribe(ISystemModelSubscriber subscriber) throws RemoteException {
        Remote activeObject = subscriber2remote.get(subscriber);
        if(activeObject == null) {
            return;
        }
        try {
            delegate.unsubscribe((ISystemModelSubscriber) activeObject);
        } finally {
            try {
                UnicastRemoteObject.unexportObject(subscriber, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
    }

    @Override
    public SystemEntity getRoot() throws ReatmetricException, RemoteException {
        return delegate.getRoot();
    }

    @Override
    public List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ReatmetricException, RemoteException {
        return delegate.getContainedEntities(se);
    }

    @Override
    public SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException, RemoteException {
        return delegate.getSystemEntityAt(path);
    }

    @Override
    public SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException, RemoteException {
        return delegate.getSystemEntityOf(externalId);
    }

    @Override
    public int getExternalIdOf(SystemEntityPath path) throws ReatmetricException, RemoteException {
        return delegate.getExternalIdOf(path);
    }

    @Override
    public SystemEntityPath getPathOf(int externalId) throws ReatmetricException, RemoteException {
        return delegate.getPathOf(externalId);
    }

    @Override
    public void enable(SystemEntityPath path) throws ReatmetricException, RemoteException {
        delegate.enable(path);
    }

    @Override
    public void disable(SystemEntityPath path) throws ReatmetricException, RemoteException {
        delegate.disable(path);
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ReatmetricException, RemoteException {
        return delegate.getDescriptorOf(id);
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ReatmetricException, RemoteException {
        return delegate.getDescriptorOf(path);
    }

    public void terminate() {
        // Unsubscribe all remotes
        for(Map.Entry<ISystemModelSubscriber, Remote> entry : subscriber2remote.entrySet()) {
            try {
                delegate.unsubscribe((ISystemModelSubscriber) entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    UnicastRemoteObject.unexportObject(entry.getKey(), true);
                } catch (NoSuchObjectException e) {
                    // Ignore
                }
            }
        }
        subscriber2remote.clear();
    }
}
