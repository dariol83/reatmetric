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

public interface ITransportConnector {

    String getName();

    String getDescription();

    TransportConnectionStatus getConnectionStatus();

    boolean isInitialised();

    void initialise(Map<String, Object> properties) throws TransportException;

    void connect() throws TransportException;

    void disconnect() throws TransportException;

    void abort() throws TransportException;

    void dispose();

    Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties();

    // TODO: get current initialised properties

    void register(ITransportSubscriber listener);

    void deregister(ITransportSubscriber listener);
}
