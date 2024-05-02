/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.example;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicLong;

public class ExampleConnector extends AbstractTransportConnector {

    private final ExampleDriver driver;
    private Thread countingThread;
    private volatile boolean started = false;
    private final AtomicLong counter = new AtomicLong(0);

    public ExampleConnector(String name, String description, ExampleDriver driver) {
        super(name, description);
        this.driver = driver;
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        return null; // No TX,RX data rate computed
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        // If the counting thread is not started, start the thread
        if(this.countingThread == null) {
            updateAlarmState(AlarmState.NOT_APPLICABLE);
            updateConnectionStatus(TransportConnectionStatus.CONNECTING);
            this.started = true;
            this.countingThread = new Thread(this::countingLoop);
            this.countingThread.setDaemon(true);
            this.countingThread.start();
        }
    }

    private void countingLoop() {
        updateConnectionStatus(TransportConnectionStatus.OPEN);
        while(started) {
            long toDistribute = this.counter.getAndIncrement();
            this.driver.newValue(toDistribute);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // No action needed here
            }
        }
        updateConnectionStatus(TransportConnectionStatus.IDLE);
    }

    @Override
    protected synchronized void doDisconnect() throws TransportException {
        // If the counting thread is started, stop the thread
        if(this.countingThread != null) {
            updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
            this.started = false;
            this.countingThread.interrupt();
            try {
                this.countingThread.join();
            } catch (InterruptedException e) {
                // Nothing to be done here
            }
            this.countingThread = null;
        }
    }

    @Override
    protected void doDispose() {
        // Nothing to be done here
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    public boolean resetCounter() {
        if(getConnectionStatus() != TransportConnectionStatus.OPEN) {
            return false;
        }
        this.counter.set(0);
        return true;
    }
}
