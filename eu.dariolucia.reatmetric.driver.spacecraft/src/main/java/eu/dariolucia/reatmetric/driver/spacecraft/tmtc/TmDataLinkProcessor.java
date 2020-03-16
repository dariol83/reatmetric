/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.ccsds.encdec.identifier.IPacketIdentifier;
import eu.dariolucia.ccsds.encdec.identifier.PacketAmbiguityException;
import eu.dariolucia.ccsds.encdec.identifier.PacketNotIdentifiedException;
import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.AbstractReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.AosReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.IVirtualChannelReceiverOutput;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.TmReceiverVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.receiver.demux.VirtualChannelReceiverDemux;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmDataLinkConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TmDataLinkProcessor implements IVirtualChannelReceiverOutput, IRawDataSubscriber {

    private static final Logger LOG = Logger.getLogger(TmDataLinkProcessor.class.getName());

    private final int spacecraftId;
    private final boolean[] processedVCs;
    private final IPacketIdentifier packetIdentifier;
    private final IRawDataBroker broker;
    private final TmDataLinkConfiguration configuration;
    private final BiFunction<AbstractTransferFrame, SpacePacket, Instant> generationTimeResolver;
    private final BiFunction<AbstractTransferFrame, SpacePacket, Quality> packetQualityChecker;
    private VirtualChannelReceiverDemux demultiplexer;

    public TmDataLinkProcessor(int spacecraftId, IPacketIdentifier packetIdentifier, IRawDataBroker rawDataBroker, TmDataLinkConfiguration tmDataLinkConfigurations, BiFunction<AbstractTransferFrame, SpacePacket, Instant> generationTimeResolver, BiFunction<AbstractTransferFrame, SpacePacket, Quality> packetQualityChecker) {
        this.spacecraftId = spacecraftId;
        this.packetIdentifier = packetIdentifier;
        this.broker = rawDataBroker;
        this.configuration = tmDataLinkConfigurations;
        this.processedVCs = new boolean[64];
        this.generationTimeResolver = generationTimeResolver;
        this.packetQualityChecker = packetQualityChecker;
    }

    public void initialise() {
        List<AbstractReceiverVirtualChannel<?>> virtualChannels = new ArrayList<>();
        // Build the VCs to process
        List<Integer> vcToBuild = new ArrayList<>(64);
        if(configuration.getType() == TransferFrameType.TM) {
            if(configuration.getProcessVcs() != null) {
                vcToBuild.addAll(configuration.getProcessVcs());
            } else {
                vcToBuild.addAll(IntStream.rangeClosed(0, 8).boxed().collect(Collectors.toList()));
            }
            for(Integer i : vcToBuild) {
                TmReceiverVirtualChannel vc = new TmReceiverVirtualChannel(i, VirtualChannelAccessMode.PACKET, false);
                virtualChannels.add(vc);
            }
        } else if(configuration.getType() == TransferFrameType.AOS) {
            if(configuration.getProcessVcs() != null) {
                vcToBuild.addAll(configuration.getProcessVcs());
            } else {
                vcToBuild.addAll(IntStream.rangeClosed(0, 64).boxed().collect(Collectors.toList()));
            }
            for(Integer i : vcToBuild) {
                AosReceiverVirtualChannel vc = new AosReceiverVirtualChannel(i, VirtualChannelAccessMode.PACKET, false);
                virtualChannels.add(vc);
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
    public void spacePacketExtracted(AbstractReceiverVirtualChannel vc, AbstractTransferFrame firstFrame, byte[] packet, boolean qualityIndicator) {
        // Read route from the frame annotated map
        Instant receptionTime = Instant.now();
        SpacePacket sp = new SpacePacket(packet, qualityIndicator);
        String route = (String) firstFrame.getAnnotationValue(Constants.ANNOTATION_ROUTE);
        // If the packet is a bad packet, we do not even try to identify it
        if (!qualityIndicator) {
            LOG.warning("Quality indicator of space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", route " + route + " is negative, space packet marked as bad packet");
            distributeBadPacket(firstFrame, route, sp);
        } else {
            // Make an attempt to identify the packet
            String packetName = Constants.N_UNKNOWN_PACKET;
            String packetType = Constants.T_UNKNOWN_PACKET;
            if(sp.isIdle()) {
                packetType = Constants.T_IDLE_PACKET;
            } else {
                try {
                    packetName = packetIdentifier.identify(packet);
                    packetType = Constants.T_TM_PACKET;
                } catch (PacketNotIdentifiedException e) {
                    LOG.log(Level.WARNING, "Space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + " not identified: " + e.getMessage(), e);
                } catch (PacketAmbiguityException e) {
                    LOG.log(Level.WARNING, "Space packet from spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ", length " + packet.length + " ambiguous: " + e.getMessage(), e);
                }
            }
            // Perform time generation extraction/time correlation
            Instant generationTime = generationTimeResolver.apply(firstFrame, sp);
            Quality quality = packetQualityChecker.apply(firstFrame, sp);
            String source = (String) firstFrame.getAnnotationValue(Constants.ANNOTATION_SOURCE);
            // Now we distribute it and store it as well
            // TODO: provide also the frame, in order to add annotations such as related ID and annotations (VCID, VCC) -> needed for time correlation ... they could even go in the extension
            distributeSpacePacket(sp, packetName, generationTime, receptionTime, route, source, packetType, quality);
        }
    }

    private void distributeSpacePacket(SpacePacket sp, String packetName, Instant generationTime, Instant receptionTime, String route, String source, String type, Quality quality) {
        RawData rd = new RawData(broker.nextRawDataId(), generationTime, packetName, type, route, source, quality, null, sp.getPacket(), receptionTime, null);
        rd.setData(sp);
        try {
            broker.distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error while distributing packet " + packetName + " from route " + rd.getRoute(), e);
        }
    }

    private void distributeBadPacket(AbstractTransferFrame firstFrame, String route, SpacePacket sp) {
        Instant genTime = (Instant) firstFrame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME);
        Instant rcpTime = (Instant) firstFrame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME);
        if(genTime == null) {
            genTime = Instant.now();
        }
        if(rcpTime == null) {
            rcpTime = Instant.now();
        }
        RawData rd = new RawData(broker.nextRawDataId(), genTime, Constants.N_UNKNOWN_PACKET, Constants.T_BAD_PACKET, route, String.valueOf(firstFrame.getSpacecraftId()), Quality.BAD, null, sp.getPacket(), rcpTime, null);
        rd.setData(sp);
        try {
            broker.distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error while distributing bad packet from route " + rd.getRoute(), e);
        }
    }

    @Override
    public void gapDetected(AbstractReceiverVirtualChannel vc, int expectedVc, int receivedVc, int missingFrames) {
        LOG.warning("Telemetry gap detected for spacecraft ID " + spacecraftId + ", VC " + vc.getVirtualChannelId() + ": expected VCC " + expectedVc + ", actual VCC " + receivedVc + ", missing frames: " + missingFrames);
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        for(RawData rd : messages) {
            AbstractTransferFrame atf = (AbstractTransferFrame) rd.getData();
            demultiplexer.accept(atf);
        }
    }
}
