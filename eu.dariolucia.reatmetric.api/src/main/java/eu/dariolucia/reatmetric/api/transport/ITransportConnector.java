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

package eu.dariolucia.reatmetric.api.transport;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * A transport connector is in charge to manage the connection between a ReatMetric system and an external system. This interface
 * has all the methods required to monitor and control such connection, but it does not enter in the details of the protocol
 * and means that an implementation shall handle. As such, this interface does not foresee entrypoints for raw data distribution
 * or for activity implementation handling, i.e. it is not a direct extension of an {@link eu.dariolucia.reatmetric.api.processing.IActivityHandler}.
 *
 * The lifecycle of an {@link ITransportConnector} implementation foresees its creation, the invocation of the method
 * prepare() (only once, after the construction of the connector implementation), and the invocation of the method
 * initialise(...). After these steps, the connector is ready for being connected.
 */
public interface ITransportConnector extends Remote {

    /**
     * Return the name of the connector.
     *
     * @return the name of the connector
     * @throws RemoteException in case of issues on the remote side
     */
    String getName() throws RemoteException;

    /**
     * Return the description of the connector.
     *
     * @return the description of the connector
     * @throws RemoteException in case of issues on the remote side
     */
    String getDescription() throws RemoteException;

    /**
     * Return the current connection status.
     *
     * @return the current connection status
     * @throws RemoteException in case of issues on the remote side
     */
    TransportConnectionStatus getConnectionStatus() throws RemoteException;

    /**
     * Return the last generated transport connector status.
     * @return the last generated connector status
     * @throws RemoteException in case of issues on the remote side
     */
    TransportStatus getLastTransportStatus() throws RemoteException;

    /**
     * Ask the connector to finalise its construction. This method shall be called before calling any of the other
     * {@link ITransportConnector} methods. As such, it is typically invoked by the driver responsible for the creation
     * of the connector. Failing to invoke this method may result in undefined behaviour.
     *
     * Not supposed to be remoted.
     * @throws RemoteException in case of issues on the remote side
     */
    void prepare() throws RemoteException;

    /**
     * Return the initialisation status.
     *
     * @return true if the connector is initialised, otherwise false
     * @throws RemoteException in case of issues on the remote side
     */
    boolean isInitialised() throws RemoteException;

    /**
     * Initialise the connector with the provided property map.
     *
     * @param properties the properties that this connector shall use
     * @throws TransportException in case of problems
     * @throws RemoteException in case of issues on the remote side
     */
    void initialise(Map<String, Object> properties) throws TransportException, RemoteException;

    /**
     * Ask the connector to perform the connection to the external system.
     *
     * @throws TransportException in case of problems
     * @throws RemoteException in case of issues on the remote side
     */
    void connect() throws TransportException, RemoteException;

    /**
     * Ask the connector to disconnect from the external system.
     *
     * @throws TransportException in case of problems
     * @throws RemoteException in case of issues on the remote side
     */
    void disconnect() throws TransportException, RemoteException;

    /**
     * Ask the connector to abort the connection to the external system.
     *
     * @throws TransportException in case of problems
     * @throws RemoteException in case of issues on the remote side
     */
    void abort() throws TransportException, RemoteException;

    /**
     * Dispose the connector. This is the last call that the implementation instance shall accept. Further calls to the
     * implementation after calling this method may result in undefined behaviour.
     *
     * Not supposed to be remoted.
     * @throws RemoteException in case of issues on the remote side
     */
    void dispose() throws RemoteException;

    /**
     * Return the map of properties that this connector support, for dynamic initialisation. The map contains, as key,
     * the key of the property. As value, a pair containing the property description (in human readable terms) and the
     * required value type.
     *
     * @return the initialisation map
     * @throws RemoteException in case of issues on the remote side
     */
    Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() throws RemoteException;

    /**
     * Return the current initialisation property values.
     *
     * @return the current initialisation property values
     * @throws RemoteException in case of issues on the remote side
     */
    Map<String, Object> getCurrentProperties() throws RemoteException;

    /**
     * Register a subscriber to this transport connector.
     *
     * @param listener the subscriber to register
     * @throws RemoteException in case of issues on the remote side
     */
    void register(ITransportSubscriber listener) throws RemoteException;

    /**
     * Deregister a subscriber from this transport connector.
     *
     * @param listener the subscriber to deregister
     * @throws RemoteException in case of issues on the remote side
     */
    void deregister(ITransportSubscriber listener) throws RemoteException;

    /**
     * Mark this transport connector to enable auto-reconnection when the connection is lost.
     * @param reconnect true to enable reconnection, otherwise false
     * @throws RemoteException in case of issues on the remote side
     */
    void setReconnect(boolean reconnect) throws RemoteException;

    /**
     * Give back the value of the reconnect flag.
     *
     * @return the reconnect flag value
     * @throws RemoteException in case of issues on the remote side
     */
    boolean isReconnect() throws RemoteException;
}
