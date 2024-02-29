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
import eu.dariolucia.ccsds.encdec.identifier.PacketAmbiguityException;
import eu.dariolucia.ccsds.encdec.identifier.PacketNotIdentifiedException;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
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
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * This connector established a TCP/IP connection to a host on a given port, and uses this connection to:
 * <ul>
 *     <li>Receive TM space packets</li>
 *     <li>Send TC space packets</li>
 * </ul>
 */
public class SpacePacketTcpConnector extends AbstractFullDuplexTcpConnector implements ITcPacketConnector {
    private IPacketIdentifier packetIdentifier;
    private Instant epoch;
    private ITimeCorrelation timeCorrelation;
    private IServiceBroker serviceBroker;
    private String source;
    private String route;

    public SpacePacketTcpConnector() {
        super("Space Packet Connector", "Space Packet Connector");
    }


    @Override
    protected void processDataUnit(byte[] dataUnit, Instant receptionTime) {
        // Build TM packet with configuration from spacecraft
        // Assume good packet
        SpacePacket sp = new SpacePacket(dataUnit, true);
        // Annotate with reception time
        sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, receptionTime);
        // Make an attempt to identify the packet
        String packetName;
        String packetType = Constants.T_TM_PACKET;
        if(sp.isIdle()) {
            packetName = Constants.N_IDLE_PACKET;
            packetType = Constants.T_IDLE_PACKET;
        } else {
            try {
                packetName = this.packetIdentifier.identify(dataUnit);
            } catch (PacketNotIdentifiedException | PacketAmbiguityException e) {
                packetName = Constants.N_UNKNOWN_PACKET;
            }
        }
        // Perform time generation extraction/time correlation for packets
        Instant generationTime = extractPacketGenerationTime(receptionTime, sp);
        Quality quality = checkPacketQuality(sp);
        // Create the raw data and set annotations
        RawData rd = createRawData(generationTime, packetName, packetType, route, source, quality, null, sp.getPacket(), receptionTime, null);
        sp.setAnnotationValue(Constants.ANNOTATION_ROUTE, rd.getRoute());
        sp.setAnnotationValue(Constants.ANNOTATION_SOURCE, rd.getSource());
        sp.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rd.getGenerationTime());
        sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rd.getReceptionTime());
        sp.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rd.getInternalId());
        rd.setData(sp);
        // Distribute
        distributeRawData(rd);
    }

    @Override
    protected byte[] readDataUnit(InputStream inputStream) throws Exception {
        byte[] header = inputStream.readNBytes(SpacePacket.SP_PRIMARY_HEADER_LENGTH);
        if(header.length == 0) {
            throw new IOException("End of stream");
        }
        // Get length of packet
        int pktDataLength = Short.toUnsignedInt(ByteBuffer.wrap(header, 4, 2).getShort()) + 1;
        // Read the rest
        byte[] packet = new byte[SpacePacket.SP_PRIMARY_HEADER_LENGTH + pktDataLength];
        inputStream.readNBytes(packet, SpacePacket.SP_PRIMARY_HEADER_LENGTH, pktDataLength);
        // Copy header in place
        System.arraycopy(header, 0, packet, 0, SpacePacket.SP_PRIMARY_HEADER_LENGTH);
        // Return the data unit
        return packet;
    }

    private Quality checkPacketQuality(SpacePacket spacePacket) {
        TmPusConfiguration conf = getSpacecraftConfig().getTmPacketConfiguration().getPusConfigurationFor(spacePacket.getApid());
        return TmPacketProcessor.checkPacketQuality(conf, spacePacket);
    }

    public Instant extractPacketGenerationTime(Instant packetReceptionTime, SpacePacket spacePacket) {
        TmPusConfiguration conf = getSpacecraftConfig().getTmPacketConfiguration().getPusConfigurationFor(spacePacket.getApid());
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
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, IPacketIdentifier packetIdentifier, String connectorInformation) throws RemoteException {
        // Call configuration for parent class
        internalConfigure(driverName, configuration, context);
        // Parse configuration for this class
        String[] infoSpl = connectorInformation.split(":", -1); // String with format: "<ip>:<port>"
        // Set host and port in parent class
        setConnectionInformation(infoSpl[0], Integer.parseInt(infoSpl[1]));
        // Set the remaining configuration data
        this.packetIdentifier = packetIdentifier;
        this.epoch = configuration.getEpoch() == null ? null : Instant.ofEpochMilli(configuration.getEpoch().getTime());
        this.serviceBroker = serviceBroker;
        ITimeCorrelation timeCorrelationService = serviceBroker.locate(eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation.class);
        this.timeCorrelation = Objects.requireNonNullElse(timeCorrelationService, TmPacketProcessor.IDENTITY_TIME_CORRELATION);

        // No info about the spacecraft ID in the packet, read it from configuration
        this.source = String.valueOf(getSpacecraftConfig().getId());
        this.route = this.source + ".TCP." + getHost() + '.' + getPort();
    }

    @Override
    public synchronized void sendTcPacket(SpacePacket sp, TcTracker tcTracker) throws RemoteException {
        sendDataUnit(sp.getPacket(), Pair.of(sp, tcTracker), tcTracker != null ? tcTracker.getInvocation().getActivityId() : -1,
                tcTracker != null ? tcTracker.getInvocation().getActivityOccurrenceId() : null);
    }

    @Override
    protected void reportActivityRelease(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime, ActivityReportState status) {
        // Report status to service broker
        Pair<SpacePacket, TcTracker> tracker = (Pair<SpacePacket, TcTracker>) trackingInformation;
        this.serviceBroker.informTcPacket(status == ActivityReportState.OK ? TcPhase.RELEASED : TcPhase.FAILED, progressTime, tracker.getSecond());
        // Invoke super method
        super.reportActivityRelease(trackingInformation, activityId, activityOccurrenceId, progressTime, status);
    }

    @Override
    protected void reportActivityReceptionOk(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        // Report status to service broker
        Pair<SpacePacket, TcTracker> tracker = (Pair<SpacePacket, TcTracker>) trackingInformation;
        this.serviceBroker.informTcPacket(TcPhase.UPLINKED, progressTime, tracker.getSecond());
        // Invoke super method
        super.reportActivityReceptionOk(trackingInformation, activityId, activityOccurrenceId, progressTime);
        // Inform on-board availability (ignore propagation delay)
        this.serviceBroker.informTcPacket(TcPhase.AVAILABLE_ONBOARD, progressTime, tracker.getSecond());
    }

    @Override
    protected void reportActivityReceptionFailure(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        // Report status to service broker
        Pair<SpacePacket, TcTracker> tracker = (Pair<SpacePacket, TcTracker>) trackingInformation;
        this.serviceBroker.informTcPacket(TcPhase.FAILED, progressTime, tracker.getSecond());
        // Invoke super method
        super.reportActivityReceptionFailure(trackingInformation, activityId, activityOccurrenceId, progressTime);
    }

    @Override
    public List<String> getSupportedRoutes() throws RemoteException {
        return Collections.singletonList("Space Packet @ " + getHost() + ":" + getPort());
    }
}
