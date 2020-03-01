/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.transport;

import eu.dariolucia.reatmetric.api.model.AlarmState;

public final class TransportStatus {

    private final String name;
    private final String message;
    private final TransportConnectionStatus status;
    private final AlarmState alarmState;
    private final long txRate;
    private final long rxRate;

    public TransportStatus(String name, String message, TransportConnectionStatus status, long txRate, long rxRate, AlarmState alarmState) {
        this.name = name;
        this.message = message;
        this.status = status;
        this.txRate = txRate;
        this.rxRate = rxRate;
        this.alarmState = alarmState;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

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

    public AlarmState getAlarmState() {
        return alarmState;
    }

    @Override
    public String toString() {
        return "TransportStatus{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", txRate=" + txRate +
                ", rxRate=" + rxRate +
                ", alarmState=" + alarmState +
                '}';
    }
}
