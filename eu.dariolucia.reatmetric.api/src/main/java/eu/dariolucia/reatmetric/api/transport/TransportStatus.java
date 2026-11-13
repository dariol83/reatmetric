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

import eu.dariolucia.reatmetric.api.model.AlarmState;

import java.io.Serializable;

/**
 * This immutable class is used to deliver updates in the status of {@link ITransportConnector} objects.
 */
public final class TransportStatus implements Serializable {

    /** The name of the transport connector. */
    private final String name;
    /** The connection status. */
    private final TransportConnectionStatus status;
    /** The alarm state. */
    private final AlarmState alarmState;
    /** The transmission rate in bits per second. */
    private final long txRate;
    /** The reception rate in bits per second. */
    private final long rxRate;
    /** Whether auto-reconnect is enabled. */
    private final boolean autoReconnect;

    /**
     * Constructs a new TransportStatus.
     *
     * @param name the name of the transport connector
     * @param status the connection status
     * @param txRate the transmission rate in bits per second
     * @param rxRate the reception rate in bits per second
     * @param alarmState the alarm state
     * @param autoReconnect whether auto-reconnect is enabled
     */
    public TransportStatus(String name, TransportConnectionStatus status, long txRate, long rxRate, AlarmState alarmState, boolean autoReconnect) {
        this.name = name;
        this.status = status;
        this.txRate = txRate;
        this.rxRate = rxRate;
        this.alarmState = alarmState;
        this.autoReconnect = autoReconnect;
    }

    /**
     * The name of the connector, typically immutable.
     *
     * @return the connector name, shall not be null
     */
    public String getName() {
        return name;
    }

    /**
     * The status of the connection.
     *
     * @return the connection status
     */
    public TransportConnectionStatus getStatus() {
        return status;
    }

    /**
     * The transmission bitrate.
     *
     * @return the transmission bitrate (bps)
     */
    public long getTxRate() {
        return txRate;
    }

    /**
     * The reception bitrate.
     *
     * @return the reception bitrate (bps)
     */
    public long getRxRate() {
        return rxRate;
    }

    /**
     * The alarm state of the connector, i.e. an indication of the health state of the connector as object. The state of
     * the connection handled by the connector is reported by {@link TransportStatus#getStatus()}.
     *
     * @return the alarm state of the connector
     */
    public AlarmState getAlarmState() {
        return alarmState;
    }

    /**
     * The auto-reconnection of the transport status.
     *
     * @return the auto-reconnection flag linked to this connector
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    @Override
    /**
     * Returns a string representation of this TransportStatus.
     *
     * @return the string representation
     */
    public String toString() {
        return "TransportStatus{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", txRate=" + txRate +
                ", rxRate=" + rxRate +
                ", alarmState=" + alarmState +
                ", autoReconnect=" + autoReconnect +
                '}';
    }
}
