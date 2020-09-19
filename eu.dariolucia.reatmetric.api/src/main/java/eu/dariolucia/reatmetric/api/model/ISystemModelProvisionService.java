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


package eu.dariolucia.reatmetric.api.model;

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 * @author dario
 */
public interface ISystemModelProvisionService extends Remote {

    void subscribe(ISystemModelSubscriber subscriber) throws RemoteException;
    
    void unsubscribe(ISystemModelSubscriber subscriber) throws RemoteException;
    
    SystemEntity getRoot() throws ReatmetricException, RemoteException;
    
    List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ReatmetricException, RemoteException;
    
    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException, RemoteException;

    SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException, RemoteException;

    int getExternalIdOf(SystemEntityPath path) throws ReatmetricException, RemoteException;

    SystemEntityPath getPathOf(int externalId) throws ReatmetricException, RemoteException;

    void enable(SystemEntityPath path) throws ReatmetricException, RemoteException;

    void disable(SystemEntityPath path) throws ReatmetricException, RemoteException;

    AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ReatmetricException, RemoteException;

    AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ReatmetricException, RemoteException;

}
