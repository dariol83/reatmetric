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

import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket.ITcPacketConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPusConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.packet.TmPacketProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This connector established a TCP/IP connection to a host on a given port, and uses this connection to:
 * <ul>
 *     <li>Receive TM space packets</li>
 *     <li>Send TC space packets</li>
 * </ul>
 */
public class SpacePacketTcpConnector extends AbstractTransportConnector implements ITcPacketConnector {
    private static final Logger LOG = Logger.getLogger(SpacePacketTcpConnector.class.getName());

    private volatile Instant lastSamplingTime;
    private final AtomicLong rxBytes = new AtomicLong(0); // received bytes
    private final AtomicLong txBytes = new AtomicLong(0); // injected bytes
    private String driverName;
    private SpacecraftConfiguration spacecraftConfig;
    private IServiceCoreContext context;
    private String host;
    private int port;

    private volatile Socket socket;

    private volatile boolean closing;

    private final ExecutorService commandForwarderExecutor = Executors.newSingleThreadExecutor(r -> {
       Thread t = new Thread(r, "Space Packet - TC packet forward thread");
       t.setDaemon(true);
       return t;
    });

    private Thread readingTmThread = null;
    private IPacketIdentifier packetIdentifier;
    private Instant epoch;
    private ITimeCorrelation timeCorrelation;
    private IServiceBroker serviceBroker;

    public SpacePacketTcpConnector() {
        super("Space Packet Connector", "Space Packet Connector");
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
                // Connect, start thread to read TM packets and send RawData to broker for processing
                socket = new Socket(this.host, this.port);
                final InputStream dataStream = socket.getInputStream();
                readingTmThread = new Thread(() -> {
                    readTm(dataStream);
                }, "Space Packet - TM packet reading thread");
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
        String route = String.valueOf(spacecraftConfig.getId()) + '.' + ".TCP." + host + '.' + port;
        while(this.socket != null) {
            try {
                byte[] header = inputStream.readNBytes(SpacePacket.SP_PRIMARY_HEADER_LENGTH);
                if(header.length == 0) {
                    throw new IOException("End of stream");
                }
                rxBytes.addAndGet(header.length);
                // Get length of packet
                int pktDataLength = Short.toUnsignedInt(ByteBuffer.wrap(header, 4, 2).getShort()) + 1;
                // Read the rest
                byte[] packet = new byte[SpacePacket.SP_PRIMARY_HEADER_LENGTH + pktDataLength];
                inputStream.readNBytes(packet, SpacePacket.SP_PRIMARY_HEADER_LENGTH, pktDataLength);
                // Copy header in place
                System.arraycopy(header, 0, packet, 0, SpacePacket.SP_PRIMARY_HEADER_LENGTH);

                Instant receptionTime = Instant.now();
                // Build TM packet with configuration from spacecraft
                // Assume good packet
                SpacePacket sp = new SpacePacket(packet, true);
                // Annotate with reception time
                sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, receptionTime);
                // Make an attempt to identify the packet
                String packetName = Constants.N_UNKNOWN_PACKET;
                String packetType = Constants.T_TM_PACKET;
                if(sp.isIdle()) {
                    packetName = Constants.N_IDLE_PACKET;
                    packetType = Constants.T_IDLE_PACKET;
                } else {
                    packetName = this.packetIdentifier.identify(packet);
                }
                // Perform time generation extraction/time correlation for packets
                Instant generationTime = extractPacketGenerationTime(receptionTime, sp);
                Quality quality = checkPacketQuality(sp);
                // No info about the spacecraft ID in the packet, read it from configuration
                String source = String.valueOf(spacecraftConfig.getId());
                // Distribute to raw data broker
                RawData rd = new RawData(context.getRawDataBroker().nextRawDataId(),
                            generationTime,
                            packetName,
                            packetType,
                            route,
                            source,
                            quality,
                        null,
                            sp.getPacket(),
                            receptionTime,
                            driverName,
                        null);

                sp.setAnnotationValue(Constants.ANNOTATION_ROUTE, rd.getRoute());
                sp.setAnnotationValue(Constants.ANNOTATION_SOURCE, rd.getSource());
                sp.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rd.getGenerationTime());
                sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rd.getReceptionTime());
                sp.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rd.getInternalId());
                rd.setData(sp);
                try {
                    context.getRawDataBroker().distribute(Collections.singletonList(rd));
                } catch (ReatmetricException e) {
                    LOG.log(Level.SEVERE, "Error when distributing TM packet from TCP connection " + host + ":" + port + ": " + e.getMessage(), e);
                    updateAlarmState(AlarmState.WARNING);
                }

            } catch (IOException e) {
                if(!closing) {
                    LOG.log(Level.SEVERE, "Reading of TM packet failed: connection error on read: " + e.getMessage(), e);
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
                    LOG.log(Level.SEVERE, "Processing of TM packet failed: unknown error: " + e.getMessage(), e);
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

    private Quality checkPacketQuality(SpacePacket spacePacket) {
        TmPusConfiguration conf = this.spacecraftConfig.getTmPacketConfiguration().getPusConfigurationFor(spacePacket.getApid());
        return TmPacketProcessor.checkPacketQuality(conf, spacePacket);
    }

    public Instant extractPacketGenerationTime(Instant packetReceptionTime, SpacePacket spacePacket) {
        TmPusConfiguration conf = this.spacecraftConfig.getTmPacketConfiguration().getPusConfigurationFor(spacePacket.getApid());
        // Extract OBT according to PUS configuration (per APID)
        if(spacePacket.isSecondaryHeaderFlag() && conf != null) {
            TmPusHeader pusHeader = TmPusHeader.decodeFrom(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(),
                    conf.getObtConfiguration() != null && conf.getObtConfiguration().isExplicitPField(),
                    epoch, conf.getTimeDescriptor());
            spacePacket.setAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER, pusHeader);
            Instant generationTime = pusHeader.getAbsoluteTime();
            // 2. apply time correlation
            if(generationTime != null) {
                return timeCorrelation.toUtc(generationTime, null, spacePacket);
            }
        }
        // In case the packet time cannot be derived, then use the packet reception time, which is the best approximation possible
        return packetReceptionTime;
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
        commandForwarderExecutor.shutdownNow();
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, IPacketIdentifier packetIdentifier, String connectorInformation) throws RemoteException {
        this.driverName = driverName;
        this.spacecraftConfig = configuration;
        this.context = context;
        String[] infoSpl = connectorInformation.split(":", -1); // String with format: "<ip>:<port>"
        this.host = infoSpl[0];
        this.port = Integer.parseInt(infoSpl[1]);
        this.packetIdentifier = packetIdentifier;
        this.epoch = configuration.getEpoch() == null ? null : Instant.ofEpochMilli(configuration.getEpoch().getTime());
        this.serviceBroker = serviceBroker;
        ITimeCorrelation timeCorrelationService = serviceBroker.locate(eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation.class);
        this.timeCorrelation = Objects.requireNonNullElse(timeCorrelationService, TmPacketProcessor.IDENTITY_TIME_CORRELATION);
    }

    @Override
    public synchronized void sendTcPacket(SpacePacket sp, TcTracker tcTracker) throws RemoteException {
        if(this.commandForwarderExecutor.isShutdown()) {
            LOG.severe("Transmission of TC packet failed: Space Packet connector disposed");
            this.serviceBroker.informTcPacket(TcPhase.FAILED, Instant.now(), tcTracker);
            return;
        }
        Instant released = Instant.now();
        this.serviceBroker.informTcPacket(TcPhase.RELEASED, released, tcTracker);
        reportActivityState(tcTracker, released, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
        reportActivityState(tcTracker, released, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ENDPOINT_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        this.commandForwarderExecutor.submit(() -> {
            Socket sock = this.socket;
            if (sock == null) {
                LOG.severe("Transmission of TC packet failed: Space Packet connector is disconnected");
                this.serviceBroker.informTcPacket(TcPhase.FAILED, Instant.now(), tcTracker);
                return;
            }
            // Try to send TC packet
            Instant sent = Instant.now();
            try {
                LOG.log(Level.INFO, String.format("Sending TC packet: %s", StringUtil.toHexDump(sp.getPacket())));
                sock.getOutputStream().write(sp.getPacket());
                sock.getOutputStream().flush();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Transmission of TC packet failed: Space Packet connector error on write", e);
                this.serviceBroker.informTcPacket(TcPhase.FAILED, Instant.now(), tcTracker);
                return;
            }
            txBytes.addAndGet(sp.getLength());
            // Sent
            this.serviceBroker.informTcPacket(TcPhase.UPLINKED, sent, tcTracker);
            reportActivityState(tcTracker, sent, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ENDPOINT_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION);
            this.serviceBroker.informTcPacket(TcPhase.AVAILABLE_ONBOARD, sent, tcTracker);
        });
    }

    private void reportActivityState(TcTracker tracker, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, null, status, nextState, null));
    }

    @Override
    public List<String> getSupportedRoutes() throws RemoteException {
        return Collections.singletonList("Space Packet @ " + this.host + ":" + this.port);
    }
}
