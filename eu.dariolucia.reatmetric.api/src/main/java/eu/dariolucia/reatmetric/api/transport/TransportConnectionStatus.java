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
