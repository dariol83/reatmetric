/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AbstractSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AosSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.TmSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.builder.EncapsulationPacketBuilder;
import eu.dariolucia.ccsds.tmtc.transport.builder.SpacePacketBuilder;
import eu.dariolucia.ccsds.tmtc.transport.pdu.EncapsulationPacket;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.VirtualChannelUnit;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.OverrideLiveEnum;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmVcConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.VirtualChannelType;
import eu.dariolucia.reatmetric.driver.spacecraft.security.DataLinkSecurityManager;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TmDataLinkProcessorTest {

    private static final int SPACECRAFT_ID = 123;
    private static final int VC_ID = 3;
    private static final long PROPAGATION_DELAY = 1_234_567;
    private static final Instant RECEPTION_TIME = Instant.parse("2026-01-02T03:04:05.678901Z");
    private static final Instant GENERATION_TIME = RECEPTION_TIME.minus(PROPAGATION_DELAY, ChronoUnit.MICROS);
    private static final Instant RESOLVED_PACKET_TIME = GENERATION_TIME.plusSeconds(42);

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void spacePacketModeStillUsesItsTimeAndQualityResolvers(TransferFrameType type) {
        AbstractTransferFrame frame = legacyFrame(type, VirtualChannelType.PACKET);
        try (Fixture fixture = new Fixture(type, bytes -> "PUS_PACKET", OverrideLiveEnum.INHERIT, VirtualChannelType.PACKET)) {
            assertTrue(fixture.receive(frame, true));

            assertEquals(1, fixture.broker.published.size());
            RawData raw = fixture.broker.published.get(0);
            assertEquals(Constants.T_TM_PACKET, raw.getType());
            assertEquals("PUS_PACKET", raw.getName());
            assertEquals(RESOLVED_PACKET_TIME, raw.getGenerationTime());
            assertEquals(Quality.UNKNOWN, raw.getQuality());
            assertEquals(42, assertInstanceOf(SpacePacket.class, raw.getData()).getApid());
            assertEquals(1, fixture.generationResolutions);
            assertEquals(1, fixture.qualityChecks);
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void vcaAndRawModesRetainTheirPayloadAndWholeFrameSemantics(TransferFrameType type) {
        for (VirtualChannelType mode : List.of(VirtualChannelType.VCA, VirtualChannelType.RAW)) {
            AbstractTransferFrame frame = legacyFrame(type, mode);
            try (Fixture fixture = new Fixture(type, bytes -> "CHANNEL_DATA", OverrideLiveEnum.INHERIT, mode)) {
                assertTrue(fixture.receive(frame, false));

                assertEquals(1, fixture.broker.published.size());
                RawData raw = fixture.broker.published.get(0);
                byte[] expected = mode == VirtualChannelType.RAW ? frame.getFrame() : frame.getDataFieldCopy();
                assertEquals(mode == VirtualChannelType.RAW ? Constants.T_TM_RAW : Constants.T_TM_VCA, raw.getType());
                assertArrayEquals(expected, raw.getContents());
                assertArrayEquals(expected, assertInstanceOf(VirtualChannelUnit.class, raw.getData()).getData());
                assertEquals(Quality.GOOD, raw.getQuality());
                assertEquals(GENERATION_TIME, raw.getGenerationTime());
                assertFalse(raw.isLive());
                assertEquals(0, fixture.generationResolutions);
                assertEquals(0, fixture.qualityChecks);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void ignoredVirtualChannelsAreExcludedFromTheBrokerSubscription(TransferFrameType type) {
        try (Fixture fixture = new Fixture(type, bytes -> fail("Ignored frames must not be identified"),
                OverrideLiveEnum.INHERIT, VirtualChannelType.IGNORE)) {
            assertFalse(fixture.receive(legacyFrame(type, VirtualChannelType.IGNORE), true));
            assertTrue(fixture.broker.published.isEmpty());
            assertEquals(0, fixture.identifications);
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void reassemblesEncapsulationPacketsAndRetainsFirstFrameMetadata(TransferFrameType type) {
        EncapsulationPacket packet = packet(170);
        try (Fixture fixture = new Fixture(type, bytes -> {
            assertArrayEquals(packet.getPacket(), bytes);
            return "CFDP_DATA";
        }, OverrideLiveEnum.INHERIT)) {
            List<AbstractTransferFrame> frames = frames(type, packet);
            assertTrue(frames.size() > 1, "Exercise encapsulation across frame boundaries");

            frames.forEach(frame -> fixture.receive(frame, false));

            RawData raw = fixture.nonIdlePackets().get(0);
            assertEquals(1, fixture.nonIdlePackets().size());
            assertEquals(1, fixture.identifications);
            assertEquals("CFDP_DATA", raw.getName());
            assertEquals(Constants.T_ENC_PACKET, raw.getType());
            assertEquals(Quality.GOOD, raw.getQuality());
            assertArrayEquals(packet.getPacket(), raw.getContents());
            assertEquals(GENERATION_TIME, raw.getGenerationTime(), "Frame time already includes propagation delay");
            assertEquals(RECEPTION_TIME, raw.getReceptionTime());
            assertEquals("station/route", raw.getRoute());
            assertEquals("spacecraft/source", raw.getSource());
            assertEquals("test-driver", raw.getHandler());
            assertFalse(raw.isLive());
            EncapsulationPacket decoded = assertInstanceOf(EncapsulationPacket.class, raw.getData());
            assertTrue(decoded.isQualityIndicator());
            assertEquals(EncapsulationPacket.ProtocolIdType.PROTOCOL_ID_CFDP, decoded.getEncapsulationProtocolId());
            assertEquals(RECEPTION_TIME, decoded.getAnnotationValue(Constants.ANNOTATION_RCP_TIME));
            assertEquals(VC_ID, decoded.getAnnotationValue(Constants.ANNOTATION_VCID));
            TmFrameDescriptor descriptor = assertInstanceOf(TmFrameDescriptor.class, raw.getExtension());
            assertEquals(VC_ID, descriptor.getVirtualChannelId());
            assertEquals(17, descriptor.getVirtualChannelFrameCounter());
            assertEquals(RECEPTION_TIME, descriptor.getEarthReceptionTime());
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void missingMiddleFrameMarksReconstructedPacketBad(TransferFrameType type) {
        EncapsulationPacket packet = packet(220);
        try (Fixture fixture = new Fixture(type, bytes -> "DAMAGED_CFDP", OverrideLiveEnum.INHERIT)) {
            List<AbstractTransferFrame> frames = frames(type, packet);
            assertTrue(frames.size() >= 4);
            fixture.receive(frames.get(0), true);
            for (int i = 2; i < frames.size(); ++i) {
                fixture.receive(frames.get(i), true);
            }

            assertEquals(1, fixture.nonIdlePackets().size());
            RawData raw = fixture.nonIdlePackets().get(0);
            assertEquals(Quality.BAD, raw.getQuality());
            assertEquals("DAMAGED_CFDP", raw.getName());
            assertEquals(1, fixture.identifications, "Non-idle damaged packets are still identified");
            assertFalse(assertInstanceOf(EncapsulationPacket.class, raw.getData()).isQualityIndicator());
            assertEquals(packet.getLength(), raw.getContents().length);
            assertFalse(Arrays.equals(packet.getPacket(), raw.getContents()));
            assertEquals(GENERATION_TIME, raw.getGenerationTime());
        }
    }

    @Test
    void unknownAmbiguousAndFailingIdentificationStillDistributeEncapsulationPackets() {
        List<IPacketIdentifier> identifiers = List.of(
                bytes -> { throw new PacketNotIdentifiedException(bytes); },
                bytes -> { throw new PacketAmbiguityException("Two definitions matched"); },
                bytes -> { throw new IllegalStateException("Identifier failed"); });
        for (IPacketIdentifier identifier : identifiers) {
            try (Fixture fixture = new Fixture(TransferFrameType.TM, identifier, OverrideLiveEnum.INHERIT)) {
                frames(TransferFrameType.TM, packet(10)).forEach(frame -> fixture.receive(frame, true));

                assertEquals(1, fixture.nonIdlePackets().size());
                RawData raw = fixture.nonIdlePackets().get(0);
                assertEquals(Constants.N_UNKNOWN_ENC_PACKET, raw.getName());
                assertEquals(Constants.T_ENC_PACKET, raw.getType());
                assertEquals(Quality.GOOD, raw.getQuality(), "Identification does not determine receiver quality");
                assertEquals(1, fixture.identifications);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferFrameType.class, names = {"TM", "AOS"})
    void idleEncapsulationPacketsBypassIdentification(TransferFrameType type) {
        try (Fixture fixture = new Fixture(type, bytes -> fail("Idle packets must not be identified"), OverrideLiveEnum.INHERIT)) {
            EncapsulationPacket idle = EncapsulationPacketBuilder.create().setIdle().setData(new byte[12]).build();
            frames(type, idle).forEach(frame -> fixture.receive(frame, true));

            assertFalse(fixture.broker.published.isEmpty());
            for (RawData raw : fixture.broker.published) {
                assertEquals(Constants.N_IDLE_ENC_PACKET, raw.getName());
                assertEquals(Constants.T_IDLE_ENC_PACKET, raw.getType());
                assertEquals(Quality.GOOD, raw.getQuality());
                assertTrue(assertInstanceOf(EncapsulationPacket.class, raw.getData()).isIdle());
            }
            assertEquals(0, fixture.identifications);
        }
    }

    @Test
    void missingGenerationAnnotationUsesReceptionTimeAndPropagationDelay() {
        try (Fixture fixture = new Fixture(TransferFrameType.TM, bytes -> "CFDP", OverrideLiveEnum.INHERIT)) {
            List<AbstractTransferFrame> frames = frames(TransferFrameType.TM, packet(10));
            frames.forEach(frame -> {
                frame.clearAnnotationValue(Constants.ANNOTATION_GEN_TIME);
                fixture.receive(frame, true);
            });

            assertEquals(GENERATION_TIME, fixture.nonIdlePackets().get(0).getGenerationTime());
        }
    }

    @ParameterizedTest
    @EnumSource(OverrideLiveEnum.class)
    void appliesVirtualChannelLiveOverrides(OverrideLiveEnum override) {
        try (Fixture fixture = new Fixture(TransferFrameType.TM, bytes -> "CFDP", override)) {
            frames(TransferFrameType.TM, packet(10)).forEach(frame -> fixture.receive(frame, false));

            assertEquals(override == OverrideLiveEnum.LIVE, fixture.nonIdlePackets().get(0).isLive());
        }
    }

    @Test
    void rendersArchivedEncapsulationHeadersWithoutTransientPacketData() {
        EncapsulationPacket packet = EncapsulationPacketBuilder.create()
                .setEncapsulationProtocolId(EncapsulationPacket.ProtocolIdType.PROTOCOL_ID_EXTENSION)
                .setEncapsulationProtocolIdExtension(4).setUserDefinedField(9)
                .setCcsdsDefinedField(new byte[] {0x12, 0x34}).setData(new byte[] {1, 2, 3}).build();
        RawData raw = new RawData(new LongUniqueId(1), GENERATION_TIME, "Extended packet", Constants.T_ENC_PACKET,
                "station/route", "spacecraft/source", Quality.BAD, null, packet.getPacket(), RECEPTION_TIME,
                "test-driver", false, new TmFrameDescriptor(VC_ID, 17, RECEPTION_TIME));
        try (Fixture fixture = new Fixture(TransferFrameType.TM, bytes -> fail("Rendering must not identify packets"), OverrideLiveEnum.INHERIT)) {
            raw.setData(packet);
            var inMemory = fixture.processor.renderEncapsulationPacket(raw);
            raw.setData(null);
            var archived = fixture.processor.renderEncapsulationPacket(raw);

            assertEquals(inMemory, archived);
            assertEquals("PROTOCOL_ID_EXTENSION", archived.get("Protocol ID"));
            assertEquals("false", archived.get("Idle Packet"));
            assertEquals("11", archived.get("Length"));
            assertEquals("8", archived.get("Primary Header Length"));
            assertEquals("3", archived.get("Data Field Length"));
            assertEquals("4", archived.get("Protocol ID Extension"));
            assertEquals("9", archived.get("User Defined Field"));
            assertEquals("1234", archived.get("CCSDS Defined Field"));
            assertEquals(String.valueOf(VC_ID), archived.get("Virtual Channel ID"));
            assertEquals("17", archived.get("Virtual Channel Counter"));
            assertEquals(RECEPTION_TIME.toString(), archived.get("Earth-Reception Time"));
            assertEquals(0, fixture.identifications);
        }
    }

    private static EncapsulationPacket packet(int dataLength) {
        byte[] data = new byte[dataLength];
        Arrays.fill(data, (byte) 0x5a);
        return EncapsulationPacketBuilder.create()
                .setEncapsulationProtocolId(EncapsulationPacket.ProtocolIdType.PROTOCOL_ID_CFDP).setData(data).build();
    }

    private static List<AbstractTransferFrame> frames(TransferFrameType type, EncapsulationPacket packet) {
        AbstractSenderVirtualChannel<?> sender = sender(type, VirtualChannelAccessMode.ENCAPSULATION);
        List<AbstractTransferFrame> frames = new ArrayList<>();
        sender.register((vc, frame, bufferedBytes) -> frames.add(frame));
        sender.dispatch(packet);
        int remaining = sender.getRemainingFreeSpace();
        if (remaining < sender.getMaxUserDataLength()) {
            EncapsulationPacketBuilder padding = EncapsulationPacketBuilder.create().setIdle();
            if (remaining > 1) {
                padding.setLengthOfLength(1).setData(new byte[remaining - 2]);
            }
            sender.dispatch(padding.build());
        }
        annotateFrames(frames);
        return frames;
    }

    private static AbstractTransferFrame legacyFrame(TransferFrameType type, VirtualChannelType mode) {
        AbstractSenderVirtualChannel<?> sender = sender(type,
                mode == VirtualChannelType.PACKET || mode == VirtualChannelType.IGNORE
                        ? VirtualChannelAccessMode.PACKET : VirtualChannelAccessMode.DATA);
        List<AbstractTransferFrame> frames = new ArrayList<>();
        sender.register((vc, frame, bufferedBytes) -> frames.add(frame));
        if (sender.getMode() == VirtualChannelAccessMode.PACKET) {
            SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setApid(42)
                    .setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED);
            builder.addData(new byte[sender.getMaxUserDataLength() - SpacePacket.SP_PRIMARY_HEADER_LENGTH]);
            sender.dispatch(builder.build());
        } else {
            byte[] data = new byte[sender.getMaxUserDataLength()];
            Arrays.fill(data, (byte) 0x5a);
            sender.dispatch(data);
        }
        assertEquals(1, frames.size());
        annotateFrames(frames);
        return frames.get(0);
    }

    private static AbstractSenderVirtualChannel<?> sender(TransferFrameType type, VirtualChannelAccessMode mode) {
        AbstractSenderVirtualChannel<?> sender = type == TransferFrameType.TM
                ? new TmSenderVirtualChannel(SPACECRAFT_ID, VC_ID, mode, false, 64, () -> 0, null)
                : new AosSenderVirtualChannel(SPACECRAFT_ID, VC_ID, mode, false, 64, null);
        sender.setVirtualChannelFrameCounter(17);
        return sender;
    }

    private static void annotateFrames(List<AbstractTransferFrame> frames) {
        for (int i = 0; i < frames.size(); ++i) {
            AbstractTransferFrame frame = frames.get(i);
            frame.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, GENERATION_TIME.plusSeconds(i));
            frame.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, RECEPTION_TIME.plusSeconds(i));
            frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, "station/route");
            frame.setAnnotationValue(Constants.ANNOTATION_SOURCE, "spacecraft/source");
            frame.setAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY, Quality.GOOD.ordinal());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final RecordingBroker broker = new RecordingBroker();
        private final TmDataLinkProcessor processor;
        private final TransferFrameType type;
        private int identifications;
        private int generationResolutions;
        private int qualityChecks;

        private Fixture(TransferFrameType type, IPacketIdentifier identifier, OverrideLiveEnum liveOverride) {
            this(type, identifier, liveOverride, VirtualChannelType.ENCAPSULATION);
        }

        private Fixture(TransferFrameType type, IPacketIdentifier identifier, OverrideLiveEnum liveOverride, VirtualChannelType processType) {
            this.type = type;
            SpacecraftConfiguration configuration = new SpacecraftConfiguration();
            configuration.setId(SPACECRAFT_ID);
            configuration.setPropagationDelay(PROPAGATION_DELAY);
            configuration.getTmDataLinkConfigurations().setType(type);
            TmVcConfiguration vc = new TmVcConfiguration();
            vc.setId(VC_ID);
            vc.setProcessType(processType);
            vc.setOverrideLive(liveOverride);
            vc.setEnableGapWarning(false);
            TmVcConfiguration encapsulationVc = new TmVcConfiguration();
            encapsulationVc.setId(VC_ID + 1);
            encapsulationVc.setProcessType(VirtualChannelType.ENCAPSULATION);
            configuration.getTmDataLinkConfigurations().setTmVcConfigurations(List.of(vc, encapsulationVc));
            IServiceCoreContext context = (IServiceCoreContext) Proxy.newProxyInstance(
                    IServiceCoreContext.class.getClassLoader(), new Class<?>[] {IServiceCoreContext.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getRawDataBroker")) {
                            return broker;
                        }
                        throw new AssertionError("Unexpected context call: " + method.getName());
                    });
            IServiceBroker services = (IServiceBroker) Proxy.newProxyInstance(
                    IServiceBroker.class.getClassLoader(), new Class<?>[] {IServiceBroker.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("locate")) {
                            return null;
                        }
                        throw new AssertionError("Unexpected service call: " + method.getName());
                    });
            processor = new TmDataLinkProcessor("test-driver", configuration, context, bytes -> {
                ++identifications;
                return identifier.identify(bytes);
            }, (frame, spacePacket) -> {
                assertEquals(VirtualChannelType.PACKET, processType, "Only Space Packets use the PUS time resolver");
                ++generationResolutions;
                return RESOLVED_PACKET_TIME;
            }, (frame, spacePacket) -> {
                assertEquals(VirtualChannelType.PACKET, processType, "Only Space Packets use the PUS quality checker");
                ++qualityChecks;
                return Quality.UNKNOWN;
            },
                    new DataLinkSecurityManager(context, configuration, services));
            processor.initialise();
        }

        private boolean receive(AbstractTransferFrame frame, boolean live) {
            RawData raw = new RawData(new LongUniqueId(-1), GENERATION_TIME, "Frame",
                    type == TransferFrameType.TM ? Constants.T_TM_FRAME : Constants.T_AOS_FRAME,
                    "station/route", "spacecraft/source", Quality.GOOD, null, frame.getFrame(),
                    RECEPTION_TIME, "connector", live, null);
            raw.setData(frame);
            boolean accepted = broker.postFilter.test(raw);
            if (accepted) {
                processor.dataItemsReceived(List.of(raw));
            }
            return accepted;
        }

        private List<RawData> nonIdlePackets() {
            return broker.published.stream().filter(raw -> raw.getType().equals(Constants.T_ENC_PACKET)).toList();
        }

        @Override
        public void close() {
            processor.dispose();
        }
    }

    private static final class RecordingBroker implements IRawDataBroker {
        private final List<RawData> published = new ArrayList<>();
        private Predicate<RawData> postFilter;
        private long nextId;

        @Override
        public void distribute(List<RawData> items, boolean store) {
            assertTrue(store);
            published.addAll(items);
        }

        @Override
        public void subscribe(IRawDataSubscriber subscriber, Predicate<RawData> preFilter, RawDataFilter filter, Predicate<RawData> postFilter) {
            this.postFilter = postFilter;
        }

        @Override
        public void unsubscribe(IRawDataSubscriber subscriber) {
        }

        @Override
        public IUniqueId nextRawDataId() {
            return new LongUniqueId(++nextId);
        }
    }
}
