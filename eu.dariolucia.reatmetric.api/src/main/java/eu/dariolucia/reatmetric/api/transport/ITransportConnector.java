/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.transport;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

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
public interface ITransportConnector {

    /**
     * Return the name of the connector.
     *
     * @return the name of the connector
     */
    String getName();

    /**
     * Return the description of the connector.
     *
     * @return the description of the connector
     */
    String getDescription();

    /**
     * Return the current connection status.
     *
     * @return the current connection status
     */
    TransportConnectionStatus getConnectionStatus();

    /**
     * Ask the connector to finalise its construction. This method shall be called before calling any of the other
     * {@link ITransportConnector} methods. Failing in doing so may result in undefined behaviour.
     */
    void prepare();

    /**
     * Return the initialisation status.
     *
     * @return true if the connector is initialised, otherwise false
     */
    boolean isInitialised();

    /**
     * Initialise the connector with the provided property map.
     *
     * @param properties the properties that this connector shall use
     * @throws TransportException in case of problems
     */
    void initialise(Map<String, Object> properties) throws TransportException;

    /**
     * Ask the connector to perform the connection to the external system.
     *
     * @throws TransportException in case of problems
     */
    void connect() throws TransportException;

    /**
     * Ask the connector to disconnect from the external system.
     *
     * @throws TransportException in case of problems
     */
    void disconnect() throws TransportException;

    /**
     * Ask the connector to abort the connection to the external system.
     *
     * @throws TransportException in case of problems
     */
    void abort() throws TransportException;

    /**
     * Dispose the connector. This is the last call that the implementation instance shall accept. Further calls to the
     * implementation after calling this method may result in undefined behaviour.
     */
    void dispose();

    /**
     * Return the map of properties that this connector support, for dynamic initialisation. The map contains, as key,
     * the key of the property. As value, a pair containing the property description (in human readable terms) and the
     * required value type.
     *
     * @return the initialisation map
     */
    Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties();

    /**
     * Return the current initialisation property values.
     *
     * @return the current initialisation property values
     */
    Map<String, Object> getCurrentProperties();

    /**
     * Register a subscriber to this transport connector.
     *
     * @param listener the subscriber to register
     */
    void register(ITransportSubscriber listener);

    /**
     * Deregister a subscriber from this transport connector.
     *
     * @param listener the subscriber to deregister
     */
    void deregister(ITransportSubscriber listener);
}
