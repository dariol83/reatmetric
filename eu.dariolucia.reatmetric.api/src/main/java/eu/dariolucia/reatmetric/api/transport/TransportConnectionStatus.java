/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.transport;

import java.util.Map;

/**
 * This enumeration is used to represent the possible connection status that a {@link ITransportConnector} object can have.
 */
public enum TransportConnectionStatus {
    /**
     * The connector is initialised and ready to start a connection via {@link ITransportConnector#connect()}.
     */
    IDLE,
    /**
     * The connector is attempting a connection to the external system.
     */
    CONNECTING,
    /**
     * The connector is currently connected to the external system and the connection is open for data flow.
     */
    OPEN,
    /**
     * The connector is disconnecting from the external system.
     */
    DISCONNECTING,
    /**
     * The connector is not connected to the external system due to a previous abort.
     */
    ABORTED,
    /**
     * The connector is not initialised, call to {@link ITransportConnector#initialise(Map)} is expected.
     */
    NOT_INIT,
    /**
     * The connector experienced an internal error, preventing him to connect or keep the connection open to the external system.
     */
    ERROR
}
