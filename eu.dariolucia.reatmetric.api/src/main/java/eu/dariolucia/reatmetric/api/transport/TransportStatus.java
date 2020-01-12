/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.transport;

public final class TransportStatus {

    private final String name;
    private final String message;
    private final TransportConnectionStatus status;
    private final long txRate;
    private final long rxRate;

    public TransportStatus(String name, String message, TransportConnectionStatus status, long txRate, long rxRate) {
        this.name = name;
        this.message = message;
        this.status = status;
        this.txRate = txRate;
        this.rxRate = rxRate;
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

    public long getTxRate() {
        return txRate;
    }

    public long getRxRate() {
        return rxRate;
    }

    @Override
    public String toString() {
        return "TransportStatus{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", txRate=" + txRate +
                ", rxRate=" + rxRate +
                '}';
    }
}
