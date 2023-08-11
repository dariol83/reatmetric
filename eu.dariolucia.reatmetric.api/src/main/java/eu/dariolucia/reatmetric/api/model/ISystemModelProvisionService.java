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
 * This interface defines the available operations for the navigation, monitoring of state changes and interactions
 * with the system elements composing the ReatMetric processing model.
 */
public interface ISystemModelProvisionService extends Remote {

    /**
     * Subscribe to the provision service, to receive live updates matching the provider filter. If the filter is null,
     * all updates will be provided to the subscriber.
     *
     * @param subscriber the callback interface, cannot be null
     * @throws RemoteException in case of remoting problem
     */
    void subscribe(ISystemModelSubscriber subscriber) throws RemoteException;

    /**
     * Unsubscribe the callback interface from the provision service.
     *
     * @param subscriber the callback interface to unsubscribe, cannot be null
     * @throws RemoteException in case of remoting problem
     */
    void unsubscribe(ISystemModelSubscriber subscriber) throws RemoteException;

    /**
     * This method returns the root of the processing model tree.
     *
     * @return the {@link SystemEntity} root of the processing model tree
     * @throws ReatmetricException in case of issues during the execution of the operation
     * @throws RemoteException in case of remoting problem
     */
    SystemEntity getRoot() throws ReatmetricException, RemoteException;

    /**
     * This method returns the list of children of a specified path in the processing model tree.
     *
     * @param path the path of the system entity
     * @return the children of the specified system entity
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    List<SystemEntity> getContainedEntities(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method returns the {@link SystemEntity} at the specified path in the processing model tree.
     *
     * @param path the path of the system entity
     * @return the specified system entity
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method returns the {@link SystemEntity} with the specified ID in the processing model tree.
     *
     * @param externalId the ID of the system entity
     * @return the specified system entity
     * @throws ReatmetricException if the ID does not exist
     * @throws RemoteException in case of remoting problem
     */
    SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException, RemoteException;

    /**
     * This method converts the specified path to the corresponding ID of the system entity.
     *
     * @param path the path of the system entity
     * @return the specified system entity ID
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    int getExternalIdOf(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method converts the specified ID to the corresponding path of the system entity.
     *
     * @param externalId the ID of the system entity
     * @return the specified system entity path
     * @throws ReatmetricException if the ID does not exist
     * @throws RemoteException in case of remoting problem
     */
    SystemEntityPath getPathOf(int externalId) throws ReatmetricException, RemoteException;

    /**
     * This method is used to enable the processing of the specified path. The processing is enabled recursively
     * on all sub-entities of the specified system entity.
     *
     * @param path the path of the system entity
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    void enable(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method is used to disable the processing of the specified path. The processing is disabled recursively
     * on all sub-entities of the specified system entity. A disabled system entity:
     * <ul>
     *     <li>If it is a parameter, injections are recorded but not processed; synthetic parameters are not re-evaluated</li>
     *     <li>If it is an event, it is not raised; conditions are not evaluated</li>
     *     <li>If it is an activity, it will not be dispatched</li>
     * </ul>
     *
     * @param path the path of the system entity
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    void disable(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method is used to ignore the processing of the specified path. The processing is ignored recursively
     * on all sub-entities of the specified system entity. An ignored system entity:
     * <ul>
     *     <li>If it is a parameter, alarm status is not computed</li>
     *     <li>If it is an event, it is raised; conditions are evaluated; log messages are not generated</li>
     *     <li>If it is an activity, it will be processed as usual</li>
     * </ul>
     *
     * @param path the path of the system entity
     * @throws ReatmetricException if the path does not exist
     * @throws RemoteException in case of remoting problem
     */
    void ignore(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * This method retrieves and returns the {@link AbstractSystemEntityDescriptor} of the system entity specified by ID.
     *
     * @param id the ID of the system entity
     * @return the {@link AbstractSystemEntityDescriptor} describing the system entity
     * @throws ReatmetricException if the ID does not exist
     * @throws RemoteException in case of remoting problem
     */
    AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ReatmetricException, RemoteException;

    /**
     * This method retrieves and returns the {@link AbstractSystemEntityDescriptor} of the system entity specified by path.
     *
     * @param path the path of the system entity
     * @return the {@link AbstractSystemEntityDescriptor} describing the system entity
     * @throws ReatmetricException if the ID does not exist
     * @throws RemoteException in case of remoting problem
     */
    AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ReatmetricException, RemoteException;

}
