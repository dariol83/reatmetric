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

import eu.dariolucia.ccsds.tmtc.coding.decoder.TmRandomizerDecoder;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicLong rxBytes = new AtomicLong(0); // received bytes
    private final AtomicLong txBytes = new AtomicLong(0); // injected bytes
    private String driverName;
    private SpacecraftConfiguration spacecraftConfig;
    private IServiceCoreContext context;
    private String host;
    private int port;
    private int caduLength;
    private int asmLength;

    private final List<IForwardDataUnitStatusSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private volatile Socket socket;

    private volatile boolean closing;

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
            long theRxBytes = rxBytes.get(); // Not atomic, but ... who cares
            long theTxBytes = txBytes.get(); // Not atomic, but ... who cares
            rxBytes.set(0);
            txBytes.set(0);
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
            closing = false;
            updateConnectionStatus(TransportConnectionStatus.CONNECTING);
            try {
                // Connect, start thread to read CADUs and send RawData to broker for processing (with TM frames)
                socket = new Socket(this.host, this.port);
                final InputStream dataStream = socket.getInputStream();
                readingTmThread = new Thread(() -> {
                    readTm(dataStream);
                }, "CLTU/CADU - CADU reading thread");
                updateConnectionStatus(TransportConnectionStatus.OPEN);
                updateAlarmState(AlarmState.NOMINAL);
                readingTmThread.setDaemon(true);
                readingTmThread.start();
            } catch (IOException e) {
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                updateAlarmState(AlarmState.ERROR);
                throw new TransportException(e);
            }
        }
        // Do nothing
    }

    private void readTm(InputStream inputStream) {
        TmRandomizerDecoder derandomizer = new TmRandomizerDecoder();
        while(this.socket != null) {
            try {
                byte[] cadu = inputStream.readNBytes(this.caduLength);
                rxBytes.addAndGet(this.caduLength);
                Instant receptionTime = Instant.now();
                // Remove ASM and correction codeblock
                cadu = Arrays.copyOfRange(cadu, asmLength, asmLength + this.spacecraftConfig.getTmDataLinkConfigurations().getFrameLength());
                // if randomisation == ON, derandomise
                if(this.spacecraftConfig.getTmDataLinkConfigurations().isDerandomize()) {
                    cadu = derandomizer.apply(cadu);
                }
                // build transfer frame info with configuration from spacecraft
                AbstractTransferFrame frame = null;
                if(spacecraftConfig.getTmDataLinkConfigurations().getType() == TransferFrameType.TM) {
                    frame = new TmTransferFrame(cadu, spacecraftConfig.getTmDataLinkConfigurations().isFecfPresent());
                } else if(spacecraftConfig.getTmDataLinkConfigurations().getType() == TransferFrameType.AOS) {
                    frame = new AosTransferFrame(cadu, spacecraftConfig.getTmDataLinkConfigurations().isFecfPresent(), spacecraftConfig.getTmDataLinkConfigurations().getAosTransferFrameInsertZoneLength(),
                            AosTransferFrame.UserDataType.M_PDU, spacecraftConfig.getTmDataLinkConfigurations().isOcfPresent(), spacecraftConfig.getTmDataLinkConfigurations().isFecfPresent());
                } else {
                    throw new IllegalArgumentException("Transfer frame type " + spacecraftConfig.getTmDataLinkConfigurations().getType() + " not supported");
                }
                // distribute to raw data broker
                if(spacecraftConfig.getTmDataLinkConfigurations().getProcessVcs() == null || spacecraftConfig.getTmDataLinkConfigurations().getProcessVcs().contains((int) frame.getVirtualChannelId())) {
                    Instant genTimeInstant = receptionTime.minusNanos(spacecraftConfig.getPropagationDelay() * 1000);
                    RawData rd = null;
                    if(frame.isValid()) {
                        String route = String.valueOf(frame.getSpacecraftId()) + '.' + frame.getVirtualChannelId() + ".TCP." + host + '.' + port;
                        rd = new RawData(context.getRawDataBroker().nextRawDataId(),
                                genTimeInstant,
                                Constants.N_TM_TRANSFER_FRAME,
                                spacecraftConfig.getTmDataLinkConfigurations().getType() == TransferFrameType.TM ? Constants.T_TM_FRAME : Constants.T_AOS_FRAME,
                                route,
                                String.valueOf(frame.getSpacecraftId()),
                                Quality.GOOD, null, frame.getFrame(), receptionTime, driverName, null);
                    } else {
                        String route = "TCP." + host + '.' + port;
                        rd = new RawData(context.getRawDataBroker().nextRawDataId(),
                                genTimeInstant,
                                Constants.N_TM_TRANSFER_FRAME,
                                Constants.T_BAD_TM,
                                route,
                                String.valueOf(frame.getSpacecraftId()),
                                Quality.BAD, null, frame.getFrame(), receptionTime, driverName, null);
                    }
                    frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, rd.getRoute());
                    frame.setAnnotationValue(Constants.ANNOTATION_SOURCE, rd.getSource());
                    frame.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rd.getGenerationTime());
                    frame.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rd.getReceptionTime());
                    frame.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rd.getInternalId());
                    rd.setData(frame);
                    try {
                        context.getRawDataBroker().distribute(Collections.singletonList(rd));
                    } catch (ReatmetricException e) {
                        LOG.log(Level.SEVERE, "Error when distributing frame from TCP connection " + host + ":" + port + ": " + e.getMessage(), e);
                        updateAlarmState(AlarmState.WARNING);
                    }
                }
            } catch (IOException e) {
                if(!closing) {
                    LOG.log(Level.SEVERE, "Reading of CADU failed: connection error on read: " + e.getMessage(), e);
                    try {
                        internalDisconnect();
                    } catch (TransportException ex) {
                        // Ignore
                    }
                    updateAlarmState(AlarmState.ERROR);
                    updateConnectionStatus(TransportConnectionStatus.ERROR);
                }
            } catch (Exception e) {
                if(!closing) {
                    LOG.log(Level.SEVERE, "Processing of CADU failed: unknown error: " + e.getMessage(), e);
                    try {
                        internalDisconnect();
                    } catch (TransportException ex) {
                        // Ignore
                    }
                    updateAlarmState(AlarmState.ERROR);
                    updateConnectionStatus(TransportConnectionStatus.ERROR);
                }
            }
        }
    }

    @Override
    protected synchronized void doDisconnect() {
        if(socket != null) {
            closing = true;
            updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
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
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOT_CHECKED);
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
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED, Instant.now(), null, null);
            return;
        }
        this.cltuForwarderExecutor.submit(() -> {
            Socket sock = this.socket;
            if (sock == null) {
                LOG.severe(String.format("Transmission of CLTU with external ID %d failed: CLTU/CADU connector is disconnected", externalId));
                informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED, Instant.now(), null, null);
                return;
            }
            // Try to send CLTU
            Instant sent = null;
            try {
                LOG.log(Level.INFO, String.format("Sending CLTU with ID %d: %s", externalId, StringUtil.toHexDump(cltu)));
                sent = Instant.now();
                sock.getOutputStream().write(cltu);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, String.format("Transmission of CLTU with external ID %d failed: CLTU/CADU connector error on write", externalId), e);
                informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED, Instant.now(), null, null);
                return;
            }
            txBytes.addAndGet(cltu.length);
            // Sent
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASED, sent, null, Constants.STAGE_ENDPOINT_RECEPTION);
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.UPLINKED, Instant.now(), Constants.STAGE_ENDPOINT_RECEPTION, null);
        });
    }

    @Override
    public List<String> getSupportedRoutes() throws RemoteException {
        return Collections.singletonList("CLTU/CADU @ " + this.host + ":" + this.port);
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status, Instant time, String currentState, String nextState) {
        subscribers.forEach(o -> o.informStatusUpdate(externalId, status, time, currentState, nextState));
    }
}
