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

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.PacketAmbiguityException;
import eu.dariolucia.ccsds.encdec.identifier.PacketNotIdentifiedException;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.*;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.demux.VirtualChannelReceiverDemux;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.ccsds.tmtc.ocf.pdu.Clcw;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IDebugInfoProvider;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.VirtualChannelUnit;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmDataLinkConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmVcConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;
import eu.dariolucia.reatmetric.driver.spacecraft.security.DataLinkSecurityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TmDataLinkProcessor implements IVirtualChannelReceiverOutput, IRawDataSubscriber, IDebugInfoProvider {

    private static final Logger LOG = Logger.getLogger(TmDataLinkProcessor.class.getName());

    private final int spacecraftId;
    private final boolean[] processedVCs;
    private final IPacketIdentifier packetIdentifier;
    private final IRawDataBroker broker;
    private final TmDataLinkConfiguration configuration;
    private final BiFunction<AbstractTransferFrame, SpacePacket, Instant> generationTimeResolver;
    private final BiFunction<AbstractTransferFrame, SpacePacket, Quality> packetQualityChecker;
    private final String driverName;
    private final DataLinkSecurityManager securityManager;
    private final long propagationDelay;
    private VirtualChannelReceiverDemux demultiplexer;

    private final Timer performanceSampler = new Timer("TM Data Link Processor - Sampler", true);
    private final AtomicReference<List<DebugInformation>> lastStats = new AtomicReference<>(Arrays.asList(
            DebugInformation.of("TM Data Link Processor", "Transfer frames", 0, null, "frames/second"),
            DebugInformation.of("TM Data Link Processor", "Space packets/VCA units", 0, null, "packets/second")
    ));
    private Instant lastSampleGenerationTime;
    private long frameInput = 0;
    private long packetOutput = 0;

    public TmDataLinkProcessor(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, IPacketIdentifier packetIdentifier, BiFunction<AbstractTransferFrame, SpacePacket, Instant> generationTimeResolver, BiFunction<AbstractTransferFrame, SpacePacket, Quality> packetQualityChecker,
                               DataLinkSecurityManager securityManager) {
        this.driverName = driverName;
        this.spacecraftId = configuration.getId();
        this.propagationDelay = configuration.getPropagationDelay();
        this.packetIdentifier = packetIdentifier;
        this.broker = context.getRawDataBroker();
        this.configuration = configuration.getTmDataLinkConfigurations();
        this.processedVCs = new boolean[64];
        this.generationTimeResolver = generationTimeResolver;
        this.packetQualityChecker = packetQualityChecker;
        this.securityManager = securityManager;
        // Create performance samples
        performanceSampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
    }

    private void sample() {
        synchronized (performanceSampler) {
            Instant genTime = Instant.now();
            if (lastSampleGenerationTime == null) {
                lastSampleGenerationTime = genTime;
                frameInput = 0;
                packetOutput = 0;
            } else {
                long frameInputCurr = frameInput;
                frameInput = 0;
                long packetOutputCurr = packetOutput;
                packetOutput = 0;
                int millis = (int) (genTime.toEpochMilli() - lastSampleGenerationTime.toEpochMilli());
                lastSampleGenerationTime = genTime;
                double framesPerSecond = (frameInputCurr / (millis/1000.0));
                double packetsPerSecond = (packetOutputCurr / (millis/1000.0));
                List<DebugInformation> toSet = Arrays.asList(
                        DebugInformation.of("TM Data Link Processor", "Transfer frames", (int) framesPerSecond, null, "frames/second"),
                        DebugInformation.of("TM Data Link Processor", "Space packets", (int) packetsPerSecond, null, "packets/second")
                );
                lastStats.set(toSet);
            }
        }
    }

    public void initialise() {
        List<AbstractReceiverVirtualChannel<?>> virtualChannels = new ArrayList<>();
        // Build the VCs to process
        List<Integer> vcToBuild = new ArrayList<>(64);
        if(configuration.getType() == TransferFrameType.TM) {
            if(configuration.getTmVcConfigurations() == null) {
                vcToBuild.addAll(IntStream.rangeClosed(0, 8).boxed().collect(Collectors.toList()));
                vcToBuild.stream().map(i -> new TmReceiverVirtualChannel(i, VirtualChannelAccessMode.PACKET, false)).forEach(virtualChannels::add);
            } else {
                for(TmVcConfiguration vcConf : configuration.getTmVcConfigurations()) {
                    switch (vcConf.getProcessType()) {
                        case PACKET: {
                            vcToBuild.add(vcConf.getId());
                            TmReceiverVirtualChannel vc = new TmReceiverVirtualChannel(vcConf.getId(), VirtualChannelAccessMode.PACKET, false);
                            virtualChannels.add(vc);
                        }
                        break;
                        case VCA: {
                            vcToBuild.add(vcConf.getId());
                            TmReceiverVirtualChannel vc = new TmReceiverVirtualChannel(vcConf.getId(), VirtualChannelAccessMode.DATA, false);
                            virtualChannels.add(vc);
                        }
                        break;
                    }
                }
            }
        } else if(configuration.getType() == TransferFrameType.AOS) {
            if(configuration.getTmVcConfigurations() == null) {
                vcToBuild.addAll(IntStream.rangeClosed(0, 64).boxed().collect(Collectors.toList()));
                vcToBuild.stream().map(i -> new AosReceiverVirtualChannel(i, VirtualChannelAccessMode.PACKET, false)).forEach(virtualChannels::add);
            } else {
                for(TmVcConfiguration vcConf : configuration.getTmVcConfigurations()) {
                    switch (vcConf.getProcessType()) {
                        case PACKET: {
                            vcToBuild.add(vcConf.getId());
                            AosReceiverVirtualChannel vc = new AosReceiverVirtualChannel(vcConf.getId(), VirtualChannelAccessMode.PACKET, false);
                            virtualChannels.add(vc);
                        }
                        break;
                        case VCA: {
                            vcToBuild.add(vcConf.getId());
                            AosReceiverVirtualChannel vc = new AosReceiverVirtualChannel(vcConf.getId(), VirtualChannelAccessMode.DATA, false);
                            virtualChannels.add(vc);
                        }
                        break;
                    }
                }
            }
        }
        // Remember VCs to process
        for(Integer i : vcToBuild) {
            processedVCs[i] = true;
        }
        // Register this object as listener on all VCs
        virtualChannels.forEach(o -> o.register(this));
        // Build the demultiplexer
        this.demultiplexer = new VirtualChannelReceiverDemux(this::missingHandler, virtualChannels.toArray(new AbstractReceiverVirtualChannel<?>[0]));
        // Subscribe to the broker
        subscribeToBroker();
    }

    private void subscribeToBroker() {
        // We want to receive only good frames of the configured type, from the configured spacecraft, from the processed VCs, not idle
        String typeName = null;
        switch(configuration.getType()) {
            case TM:
                typeName = Constants.T_TM_FRAME;
                break;
            case AOS:
                typeName = Constants.T_AOS_FRAME;
                break;
        }
        RawDataFilter filter = new RawDataFilter(true, null, null, Collections.singletonList(typeName), null, Collections.singletonList(Quality.GOOD));
        Predicate<RawData> postFilter = o -> {
            AbstractTransferFrame atf = (AbstractTransferFrame) o.getData();
            return atf != null && !atf.isIdleFrame() && atf.getSpacecraftId() == this.spacecraftId && this.processedVCs[atf.getVirtualChannelId()];
        };
        broker.subscribe(this, null, filter, postFilter);
    }

    private void missingHandler(AbstractTransferFrame abstractTransferFrame) {
        LOG.log(Level.WARNING, "Skipping TM frame from spacecraft " + abstractTransferFrame.getSpacecraftId() + " on ignored VC: " + abstractTransferFrame.getVirtualChannelId());
    }

    @Override
    public void spacePacketExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame firstFrame, byte[] packet, boolean qualityIndicator, List<PacketGap> gaps) {
        // Add performance indicator
        synchronized (performanceSampler) {
            ++packetOutput;
        }
        Instant genTime = (Instant) firstFrame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME);
        if(genTime == null) {
            genTime = Instant.now();
        }
        // Read route from the frame annotated map
        Instant receptionTime = (Instant) firstFrame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME);
        if(receptionTime == null) {
            receptionTime = Instant.now();
        }
        SpacePacket sp = new SpacePacket(packet, qualityIndicator);
        // Annotate with reception time
        sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, receptionTime);
        // Annotate with the VC ID
        sp.setAnnotationValue(Constants.ANNOTATION_VCID, (int) firstFrame.getVirtualChannelId());
        String route = (String) firstFrame.getAnnotationValue(Constants.ANNOTATION_ROUTE);
        // If the packet is a bad packet, we do not even try to identify it
        if (!qualityIndicator) {
            if (gaps.isEmpty()) {
                LOG.warning(String.format("Quality indicator of space packet from spacecraft ID %d, VC %d, route %s is negative, space packet marked as bad packet", spacecraftId, vc.getVirtualChannelId(), route));
            } else {
                LOG.warning(String.format("Quality indicator of space packet from spacecraft ID %d, VC %d, route %s is negative, gaps detected, space packet marked as bad packet", spacecraftId, vc.getVirtualChannelId(), route));
            }
        }
        // Make an attempt to identify the packet
        String packetName = Constants.N_UNKNOWN_PACKET;
        String packetType = Constants.T_TM_PACKET;
        if(sp.isIdle()) {
            packetName = Constants.N_IDLE_PACKET;
            packetType = Constants.T_IDLE_PACKET;
        } else {
            packetName = identifyPacket(vc, packet, sp, packetName);
        }
        // Perform time generation extraction/time correlation for good packets
        Instant generationTime = genTime;
        if (qualityIndicator) {
            generationTime = generationTimeResolver.apply(firstFrame, sp);
        }
        Quality quality = !qualityIndicator ? Quality.BAD : packetQualityChecker.apply(firstFrame, sp);
        String source = (String) firstFrame.getAnnotationValue(Constants.ANNOTATION_SOURCE);
        // Now we distribute it and store it as well
        // Provide also the frame information, needed for time correlation ... they go as extension
        distributeSpacePacket(sp, packetName, generationTime, receptionTime, route, source, packetType, quality, new TmFrameDescriptor(firstFrame.getVirtualChannelId(), firstFrame.getVirtualChannelFrameCount(), (Instant) firstFrame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME)));
    }

    private String identifyPacket(AbstractReceiverVirtualChannel vc, byte[] packet, SpacePacket sp, String packetName) {
        try {
            packetName = packetIdentifier.identify(packet);
        } catch (PacketNotIdentifiedException e) {
            LOG.log(Level.WARNING, "Space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + ", APID " + sp.getApid() + " not identified: " + e.getMessage(), e);
        } catch (PacketAmbiguityException e) {
            LOG.log(Level.WARNING, "Space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + ", APID " + sp.getApid() + " ambiguous: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + ", APID " + sp.getApid() + " error during identification: " + e.getMessage(), e);
        }
        return packetName;
    }

    private void distributeSpacePacket(SpacePacket sp, String packetName, Instant generationTime, Instant receptionTime, String route, String source, String type, Quality quality, TmFrameDescriptor frameDescriptor) {
        RawData rd = new RawData(broker.nextRawDataId(), generationTime, packetName, type, route, source, quality, null, sp.getPacket(), receptionTime, driverName, frameDescriptor);
        rd.setData(sp);
        try {
            broker.distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error while distributing packet " + packetName + " from route " + rd.getRoute(), e);
        }
    }

    @Override
    public void dataExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame frame, byte[] data, int missingBytes) {
        // Add performance indicator
        synchronized (performanceSampler) {
            ++packetOutput;
        }
        // Read route from the frame annotated map
        Instant receptionTime = (Instant) frame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME);
        if(receptionTime == null) {
            receptionTime = Instant.now();
        }
        // Generation time is reception time minus propagation delay
        Instant generationTime = receptionTime.minus(this.propagationDelay, ChronoUnit.MICROS);
        VirtualChannelUnit sp = new VirtualChannelUnit(data);
        // Annotate with reception time
        sp.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, receptionTime);
        // Annotate with the VC ID
        sp.setAnnotationValue(Constants.ANNOTATION_VCID, (int) frame.getVirtualChannelId());
        String route = (String) frame.getAnnotationValue(Constants.ANNOTATION_ROUTE);
        // Make an attempt to identify the virtual channel unit
        String vcaName = Constants.N_UNKNOWN_VCA;
        String rawDataType = Constants.T_TM_VCA;
        vcaName = identifyVca(vc, data, vcaName);

        String source = (String) frame.getAnnotationValue(Constants.ANNOTATION_SOURCE);
        // Now we distribute it and store it as well
        // Provide also the frame information, needed for time correlation ... they go as extension
        distributeVca(sp, vcaName, generationTime, receptionTime, route, source, rawDataType, Quality.GOOD, new TmFrameDescriptor(frame.getVirtualChannelId(), frame.getVirtualChannelFrameCount(), (Instant) frame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME)));
    }

    private String identifyVca(AbstractReceiverVirtualChannel vc, byte[] packet, String vcaName) {
        try {
            vcaName = packetIdentifier.identify(packet);
        } catch (PacketNotIdentifiedException e) {
            LOG.log(Level.WARNING, "VCA from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + " not identified: " + e.getMessage(), e);
        } catch (PacketAmbiguityException e) {
            LOG.log(Level.WARNING, "VCA from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + " ambiguous: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "VCA from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + " error during identification: " + e.getMessage(), e);
        }
        return vcaName;
    }

    private void distributeVca(VirtualChannelUnit vcaUnit, String vcaName, Instant generationTime, Instant receptionTime, String route, String source, String type, Quality quality, TmFrameDescriptor frameDescriptor) {
        RawData rd = new RawData(broker.nextRawDataId(), generationTime, vcaName, type, route, source, quality, null, vcaUnit.getData(), receptionTime, driverName, frameDescriptor);
        rd.setData(vcaUnit);
        try {
            broker.distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error while distributing VCA unit " + vcaName + " from route " + rd.getRoute(), e);
        }
    }

    @Override
    public void gapDetected(AbstractReceiverVirtualChannel vc, int expectedVc, int receivedVc, int missingFrames) {
        LOG.warning("Telemetry gap detected for spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ": expected VCC " + expectedVc + ", actual VCC " + receivedVc + ", missing frames: " + missingFrames);
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        // Add performance indicator
        synchronized (performanceSampler) {
            frameInput += messages.size();
        }
        for(RawData rd : messages) {
            AbstractTransferFrame atf = (AbstractTransferFrame) rd.getData();
            // If FECF is present and wrong, discard
            if(atf.isFecfPresent() && !atf.isValid()) {
                LOG.log(Level.SEVERE, "Invalid transfer frame (FECF) received for spacecraft " + atf.getSpacecraftId() + ", virtual channel " + atf.getVirtualChannelId());
                continue;
            }
            // Decrypt
            try {
                atf = securityManager.decrypt(atf);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot decrypt transfer frame: " + e.getMessage(), e);
                continue;
            }
            // Send to the demultiplexer
            demultiplexer.accept(atf);
        }
    }

    public void dispose() {
        performanceSampler.cancel();
        broker.unsubscribe(this);
    }

    public LinkedHashMap<String, String> renderTmFrame(RawData rawData) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        TmTransferFrame frame = (TmTransferFrame) rawData.getData();
        if(frame == null) {
            frame = new TmTransferFrame(rawData.getContents(), configuration.isFecfPresent());
        }
        toReturn.put("Frame Information", null);
        toReturn.put("Spacecraft ID", String.valueOf(frame.getSpacecraftId()));
        toReturn.put("Virtual Channel ID", String.valueOf(frame.getVirtualChannelId()));
        toReturn.put("Master Channel Counter", String.valueOf(frame.getMasterChannelFrameCount()));
        toReturn.put("Virtual Channel Counter", String.valueOf(frame.getVirtualChannelFrameCount()));
        toReturn.put("Segment Length Identifier", String.valueOf(frame.getSegmentLengthIdentifier()));
        toReturn.put("Packet Order Flag", String.valueOf(frame.isPacketOrderFlag()));
        toReturn.put("Synchronisation Flag", String.valueOf(frame.isSynchronisationFlag()));
        toReturn.put("Secondary Header Present", String.valueOf(frame.isSecondaryHeaderPresent()));
        toReturn.put("First Header Pointer", String.valueOf(frame.getFirstHeaderPointer()));
        toReturn.put("Idle Frame", String.valueOf(frame.isIdleFrame()));
        if(frame.isOcfPresent()) {
            Clcw clcw = new Clcw(frame.getOcfCopy());
            toReturn.put("CLCW", null);
            toReturn.put("CLCW TC VC ID", String.valueOf(clcw.getVirtualChannelId()));
            toReturn.put("CLCW Status Field", String.valueOf(clcw.getStatusField()));
            toReturn.put("CLCW COP in Effect", String.valueOf(clcw.getCopInEffect()));
            toReturn.put("CLCW Lockout Flag", String.valueOf(clcw.isLockoutFlag()));
            toReturn.put("CLCW No Bitlock Flag", String.valueOf(clcw.isNoBitlockFlag()));
            toReturn.put("CLCW No RF Available Flag", String.valueOf(clcw.isNoRfAvailableFlag()));
            toReturn.put("CLCW Retransmit Flag", String.valueOf(clcw.isRetransmitFlag()));
            toReturn.put("CLCW Wait Flag", String.valueOf(clcw.isWaitFlag()));
            toReturn.put("CLCW FARM-B Counter", String.valueOf(clcw.getFarmBCounter()));
            toReturn.put("CLCW Report Value", String.valueOf(clcw.getReportValue()));
        }
        if(frame.isFecfPresent()) {
            toReturn.put("FECF", null);
            toReturn.put("Validity FECF", String.valueOf(frame.isValid()));
        }
        return toReturn;
    }

    public LinkedHashMap<String, String> renderBadTm(RawData rawData) {
        return null;
    }

    public LinkedHashMap<String, String> renderAosFrame(RawData rawData) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        AosTransferFrame frame = (AosTransferFrame) rawData.getData();
        if(frame == null) {
            frame = new AosTransferFrame(rawData.getContents(), configuration.isAosFrameHeaderErrorControlPresent(), configuration.getAosTransferFrameInsertZoneLength(), AosTransferFrame.UserDataType.M_PDU, configuration.isOcfPresent(), configuration.isFecfPresent());
        }
        toReturn.put("Frame Information", null);
        toReturn.put("Spacecraft ID", String.valueOf(frame.getSpacecraftId()));
        toReturn.put("Virtual Channel ID", String.valueOf(frame.getVirtualChannelId()));
        toReturn.put("Virtual Channel Counter", String.valueOf(frame.getVirtualChannelFrameCount()));
        toReturn.put("Insert Zone Length", String.valueOf(frame.getInsertZoneLength()));
        if(frame.getInsertZoneLength() > 0) {
            toReturn.put("Insert Zone Value", StringUtil.toHexDump(frame.getInsertZoneCopy()));
        }
        toReturn.put("AOS Type", frame.getUserDataType().name());
        toReturn.put("First Header Pointer", String.valueOf(frame.getFirstHeaderPointer()));
        toReturn.put("Idle Frame", String.valueOf(frame.isIdleFrame()));
        if(frame.isOcfPresent()) {
            Clcw clcw = new Clcw(frame.getOcfCopy());
            toReturn.put("CLCW", null);
            toReturn.put("CLCW TC VC ID", String.valueOf(clcw.getVirtualChannelId()));
            toReturn.put("CLCW Status Field", String.valueOf(clcw.getStatusField()));
            toReturn.put("CLCW COP in Effect", String.valueOf(clcw.getCopInEffect()));
            toReturn.put("CLCW Lockout Flag", String.valueOf(clcw.isLockoutFlag()));
            toReturn.put("CLCW No Bitlock Flag", String.valueOf(clcw.isNoBitlockFlag()));
            toReturn.put("CLCW No RF Available Flag", String.valueOf(clcw.isNoRfAvailableFlag()));
            toReturn.put("CLCW Retransmit Flag", String.valueOf(clcw.isRetransmitFlag()));
            toReturn.put("CLCW Wait Flag", String.valueOf(clcw.isWaitFlag()));
            toReturn.put("CLCW FARM-B Counter", String.valueOf(clcw.getFarmBCounter()));
            toReturn.put("CLCW Report Value", String.valueOf(clcw.getReportValue()));
        }
        if(frame.isFecfPresent()) {
            toReturn.put("FECF", null);
            toReturn.put("Validity FECF", String.valueOf(frame.isValid()));
        }
        return toReturn;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return lastStats.get();
    }

    @Override
    public String toString() {
        return "TM Data Link Processor";
    }
}
