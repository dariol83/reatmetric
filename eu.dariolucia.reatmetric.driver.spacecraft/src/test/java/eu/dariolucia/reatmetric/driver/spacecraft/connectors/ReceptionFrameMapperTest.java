/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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
 */

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.ccsds.tmtc.datalink.channel.VirtualChannelAccessMode;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AbstractSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.AosSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.channel.sender.TmSenderVirtualChannel;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.builder.SpacePacketBuilder;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.RawTransferFrame;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionFrameMapperTest {

    private static final Instant RECEPTION_TIME = Instant.parse("2026-01-02T03:04:05.678901Z");
    private static final long PROPAGATION_DELAY = 1_234_567;

    @ParameterizedTest
    @EnumSource(value = ReceptionFrameMapper.DataType.class, names = {"TM_FRAME", "AOS_FRAME"})
    void mapsStandardFramesWithReceptionMetadata(ReceptionFrameMapper.DataType type) {
        ReceptionFrameMapper mapper = mapper(false);
        byte[] payload = frame(type, false);
        RawData raw = mapper.map(type, payload, RECEPTION_TIME, Quality.UNKNOWN, "antenna.route", Map.of(), "unused");

        assertEquals("123", raw.getSource(), "Use the spacecraft ID in the received frame");
        assertEquals(type == ReceptionFrameMapper.DataType.TM_FRAME ? Constants.T_TM_FRAME : Constants.T_AOS_FRAME, raw.getType());
        assertEquals(Constants.N_TM_TRANSFER_FRAME, raw.getName());
        assertEquals(Quality.UNKNOWN, raw.getQuality());
        assertArrayEquals(payload, raw.getContents());
        assertNull(raw.getExtension());
        assertPublicationMetadata(raw);
        assertEquals(3, assertInstanceOf(AbstractTransferFrame.class, raw.getData()).getVirtualChannelId());

        RawData overridden = mapper.map(type, payload, RECEPTION_TIME, Quality.BAD, "antenna.route", Map.of("spacecraft_id", 99), "unused");
        assertEquals("99", overridden.getSource(), "Preserve the SatDecode metadata override");
        assertEquals(Constants.T_BAD_TM, overridden.getType());
        assertEquals(Quality.BAD.ordinal(), ((AbstractTransferFrame) overridden.getData()).getAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY));
    }

    @ParameterizedTest
    @EnumSource(value = ReceptionFrameMapper.DataType.class, names = {"TM_FRAME", "AOS_FRAME"})
    void invalidFecfOverridesReportedQuality(ReceptionFrameMapper.DataType type) {
        ReceptionFrameMapper mapper = mapper(true);
        byte[] payload = frame(type, true);
        assertEquals(Quality.GOOD, mapper.map(type, payload, RECEPTION_TIME, Quality.GOOD, "antenna.route", Map.of(), "unused").getQuality());
        payload[10] ^= 1;

        RawData raw = mapper.map(type, payload, RECEPTION_TIME, Quality.GOOD, "antenna.route", Map.of(), "unused");
        assertEquals(Quality.BAD, raw.getQuality());
        assertEquals(Constants.T_BAD_TM, raw.getType());
        assertEquals(Quality.BAD.ordinal(), ((AbstractTransferFrame) raw.getData()).getAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY));
    }

    @Test
    void mapsOpaqueDataWithSatDecodeOverridesAndJtdpFallbacks() {
        ReceptionFrameMapper mapper = mapper(false);
        byte[] payload = {1, 2, 3};
        RawData raw = mapper.map(ReceptionFrameMapper.DataType.RAW, payload, RECEPTION_TIME, Quality.UNKNOWN, "antenna.route",
                Map.of("spacecraft_id", 99, "virtual_channel_id", 7, "name", "RAW_NAME", "frame_type", "metadata-type"), "envelope-type");

        assertEquals("99", raw.getSource());
        assertEquals("RAW_NAME", raw.getName());
        assertEquals(Constants.T_RAW_FRAME, raw.getType());
        assertEquals("metadata-type", raw.getExtension());
        assertArrayEquals(payload, raw.getContents());
        RawTransferFrame frame = assertInstanceOf(RawTransferFrame.class, raw.getData());
        assertTrue(frame.isValid());
        assertEquals("envelope-type", frame.getExternalTmType());
        assertEquals(7, frame.getAnnotationValue(Constants.ANNOTATION_VCID));
        assertPublicationMetadata(raw);

        RawData fallback = mapper.map(ReceptionFrameMapper.DataType.RAW, payload, RECEPTION_TIME, Quality.BAD, "antenna.route", Map.of(), Constants.N_UNKNOWN_FRAME);
        assertEquals("321", fallback.getSource());
        assertEquals(Constants.N_UNKNOWN_FRAME, fallback.getName());
        assertEquals(Constants.N_UNKNOWN_FRAME, fallback.getExtension());
        assertFalse(((RawTransferFrame) fallback.getData()).isValid());
    }

    private static void assertPublicationMetadata(RawData raw) {
        assertEquals(RECEPTION_TIME.minus(PROPAGATION_DELAY, ChronoUnit.MICROS), raw.getGenerationTime());
        assertEquals(RECEPTION_TIME, raw.getReceptionTime());
        assertEquals("antenna.route", raw.getRoute());
        assertEquals("test-driver", raw.getHandler());
        assertFalse(raw.isLive());
        AbstractTransferFrame frame = assertInstanceOf(AbstractTransferFrame.class, raw.getData());
        assertEquals(raw.getRoute(), frame.getAnnotationValue(Constants.ANNOTATION_ROUTE));
        assertEquals(raw.getSource(), frame.getAnnotationValue(Constants.ANNOTATION_SOURCE));
        assertEquals(raw.getGenerationTime(), frame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME));
        assertEquals(raw.getReceptionTime(), frame.getAnnotationValue(Constants.ANNOTATION_RCP_TIME));
        assertEquals(raw.getInternalId(), frame.getAnnotationValue(Constants.ANNOTATION_UNIQUE_ID));
        assertEquals(raw.getQuality().ordinal(), frame.getAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY));
    }

    private static ReceptionFrameMapper mapper(boolean fecfPresent) {
        SpacecraftConfiguration configuration = new SpacecraftConfiguration();
        configuration.setId(321);
        configuration.setPropagationDelay(PROPAGATION_DELAY);
        configuration.getTmDataLinkConfigurations().setFecfPresent(fecfPresent);
        configuration.getTmDataLinkConfigurations().setOcfPresent(false);
        IRawDataBroker broker = (IRawDataBroker) Proxy.newProxyInstance(IRawDataBroker.class.getClassLoader(), new Class<?>[] {IRawDataBroker.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("nextRawDataId")) {
                        return new LongUniqueId(42);
                    }
                    throw new AssertionError("Unexpected broker call: " + method.getName());
                });
        return new ReceptionFrameMapper(configuration, broker, "test-driver", false);
    }

    private static byte[] frame(ReceptionFrameMapper.DataType type, boolean fecfPresent) {
        AbstractSenderVirtualChannel<?> sender = type == ReceptionFrameMapper.DataType.TM_FRAME
                ? new TmSenderVirtualChannel(123, 3, VirtualChannelAccessMode.PACKET, fecfPresent, 64, () -> 0, null)
                : new AosSenderVirtualChannel(123, 3, VirtualChannelAccessMode.PACKET, fecfPresent, 64, null);
        List<AbstractTransferFrame> frames = new ArrayList<>();
        sender.register((vc, frame, bufferedBytes) -> frames.add(frame));
        SpacePacketBuilder builder = SpacePacketBuilder.create().setTelemetryPacket().setApid(42)
                .setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED);
        builder.addData(new byte[sender.getMaxUserDataLength() - SpacePacket.SP_PRIMARY_HEADER_LENGTH]);
        sender.dispatch(builder.build());
        assertEquals(1, frames.size());
        return frames.get(0).getFrame();
    }
}
