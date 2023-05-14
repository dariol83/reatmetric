/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This connector established a TCP/IP connection to a host on a given port, and uses this connection to:
 * <ul>
 *     <li>Receive CADUs</li>
 *     <li>Send CLTUs</li>
 * </ul>
 */
public class CltuCaduTcpConnector extends AbstractTransportConnector implements ICltuConnector {
    private static final Logger LOG = Logger.getLogger(CltuCaduTcpConnector.class.getName());

    private volatile Instant lastSamplingTime;
    private volatile long rxBytes; // injected bytes
    private volatile long txBytes; // injected bytes
    private String driverName;
    private SpacecraftConfiguration spacecraftConfig;
    private IServiceCoreContext context;
    private String host;
    private int port;
    private int caduLength;
    private int asmLength;

    private final List<IForwardDataUnitStatusSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private volatile Socket socket;

    private final ExecutorService cltuForwarderExecutor = Executors.newSingleThreadExecutor(r -> {
       Thread t = new Thread(r, "CLTU/CADU - CLTU forward thread");
       t.setDaemon(true);
       return t;
    });

    private Thread readingTmThread = null;

    public CltuCaduTcpConnector() {
        super("CLTU/CADU Connector", "CLTU/CADU Connector");
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        Instant now = Instant.now();
        if(lastSamplingTime != null) {
            long theRxBytes = rxBytes; // Not atomic, but ... who cares
            long theTxBytes = txBytes; // Not atomic, but ... who cares
            rxBytes = 0;
            txBytes = 0;
            long msDiff = now.toEpochMilli() - lastSamplingTime.toEpochMilli();
            long rxRate = Math.round((theRxBytes * 8000.0) / (msDiff));
            long txRate = Math.round((theTxBytes * 8000.0) / (msDiff));
            lastSamplingTime = now;
            return Pair.of(txRate, rxRate);
        } else {
            lastSamplingTime = now;
            return null;
        }
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        if(socket == null) {
            try {
                // Connect, start thread to read CADUs and send RawData to broker for processing (with TM frames)
                socket = new Socket(this.host, this.port);
                final InputStream dataStream = socket.getInputStream();
                readingTmThread = new Thread(() -> {
                    readTm(dataStream);
                }, "CLTU/CADU - CADU reading thread");
                readingTmThread.setDaemon(true);
                readingTmThread.start();
            } catch (IOException e) {
                throw new TransportException(e);
            }
        }
        // Do nothing
    }

    private void readTm(InputStream inputStream) {
        while(this.socket != null) {
            try {
                byte[] cadu = inputStream.readNBytes(this.caduLength);
                // TODO process CADU
                // Remove ASM

                // if randomisation == ON, derandomise

                // get first frame-length bytes

                // build transfer frame info with configuration from spacecraft

                // distribute to raw data broker
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Reading of CADU failed: connection error on read", e);
            }
        }
    }

    @Override
    protected synchronized void doDisconnect() throws TransportException {
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
            socket = null;
        }
        if(readingTmThread != null) {
            readingTmThread = null;
        }
        // Done
    }

    @Override
    protected synchronized void doDispose() {
        try {
            disconnect();
        } catch (TransportException e) {
            // Do nothing
        }
        cltuForwarderExecutor.shutdownNow();
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation) throws RemoteException {
        this.driverName = driverName;
        this.spacecraftConfig = configuration;
        this.context = context;
        String[] infoSpl = connectorInformation.split(":", -1); // String with format: "<ip>:<port>:<CADU length>:<asm length>"
        this.host = infoSpl[0];
        this.port = Integer.parseInt(infoSpl[1]);
        this.caduLength = Integer.parseInt(infoSpl[2]);
        this.asmLength = Integer.parseInt(infoSpl[3]);
    }

    @Override
    public void register(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException {
        this.subscribers.add(subscriber);
    }

    @Override
    public void deregister(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException {
        this.subscribers.remove(subscriber);
    }

    @Override
    public synchronized void sendCltu(byte[] cltu, long externalId) throws RemoteException {
        if(this.cltuForwarderExecutor.isShutdown()) {
            LOG.severe(String.format("Transmission of CLTU with external ID %d failed: CLTU/CADU connector disposed", externalId));
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED);
            return;
        }
        this.cltuForwarderExecutor.submit(() -> {
            Socket sock = this.socket;
            if (sock == null) {
                LOG.severe(String.format("Transmission of CLTU with external ID %d failed: CLTU/CADU connector is disconnected", externalId));
                informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED);
                return;
            }
            // Try to send CLTU
            try {
                LOG.log(Level.INFO, String.format("Sending CLTU with ID %d: %s", externalId, StringUtil.toHexDump(cltu)));
                sock.getOutputStream().write(cltu);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, String.format("Transmission of CLTU with external ID %d failed: CLTU/CADU connector error on write", externalId), e);
                informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED);
                return;
            }
            // Sent
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASED);
        });
    }

    @Override
    public List<String> getSupportedRoutes() throws RemoteException {
        return Collections.singletonList("CLTU/CADU @ " + this.host + ":" + this.port);
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status) {
        informSubscribers(externalId, status, Instant.now());
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status, Instant time) {
        subscribers.forEach(o -> o.informStatusUpdate(externalId, status, time));
    }
}
