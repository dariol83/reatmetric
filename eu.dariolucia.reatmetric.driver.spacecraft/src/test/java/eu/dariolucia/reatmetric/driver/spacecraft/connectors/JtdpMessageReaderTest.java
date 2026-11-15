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

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.reatmetric.api.rawdata.Quality;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JtdpMessageReaderTest {

    @Test
    void readsBatchedFramesAndCoalescedMessagesAcrossSingleByteReads() throws IOException {
        byte[] batch = message(0, 65535, join(
                frame(0, -1, new byte[]{0x12, (byte) 0xA5}),
                frame(1, 1_234_567_890L, new byte[]{0x34}),
                frame(2, Long.MIN_VALUE, new byte[]{0x56})));
        byte[] monitoring = monitoring(1, 0x65, 0x00FF_FFFF_FFFF_FFFFL, -1234, 12.5F);
        byte[] event = message(2, 42, ByteBuffer.allocate(17).put((byte) 6)
                .putLong(1_234_567_890L).put((byte) 0xAB)
                .put(new byte[]{1, 2, 3, 4, 5, 6, 7}).array());
        InputStream input = new ByteArrayInputStream(join(batch, monitoring, event)) {
            @Override
            public synchronized int read(byte[] bytes, int offset, int length) {
                return super.read(bytes, offset, Math.min(length, 1));
            }
        };

        JtdpMessageReader.FrameBatch frames = assertInstanceOf(JtdpMessageReader.FrameBatch.class,
                JtdpMessageReader.read(input));
        assertEquals(65535, frames.antennaId());
        assertEquals(3, frames.frames().size());
        assertEquals(Quality.GOOD, frames.frames().get(0).quality());
        assertEquals(Instant.ofEpochSecond(-1, 999_999_999), frames.frames().get(0).receptionTime());
        assertArrayEquals(new byte[]{0x12, (byte) 0xA5}, frames.frames().get(0).data());
        assertEquals(Quality.BAD, frames.frames().get(1).quality());
        assertEquals(Instant.ofEpochSecond(1, 234_567_890), frames.frames().get(1).receptionTime());
        assertEquals(Quality.UNKNOWN, frames.frames().get(2).quality());
        assertEquals(Instant.ofEpochSecond(-9_223_372_037L, 145_224_192), frames.frames().get(2).receptionTime());

        JtdpMessageReader.Monitoring observation = assertInstanceOf(JtdpMessageReader.Monitoring.class,
                JtdpMessageReader.read(input));
        assertEquals(1, observation.antennaId());
        assertEquals(0x65, observation.status());
        assertEquals(0x00FF_FFFF_FFFF_FFFFL, observation.frequency());
        assertEquals(-1234L, observation.offset());
        assertEquals(12.5, observation.snr());
        assertEquals(0xFFFF_FFFFL, observation.totalFrames());
        assertEquals(0x8000_0000L, observation.errorFrames());
        assertEquals(0x7FFF_FFFFL, observation.goodFrames());
        assertEquals(0L, observation.distributedFrames());

        JtdpMessageReader.Event notification = assertInstanceOf(JtdpMessageReader.Event.class,
                JtdpMessageReader.read(input));
        assertEquals(42, notification.antennaId());
        assertEquals(6, notification.code());
        assertEquals(Instant.ofEpochSecond(1, 234_567_890), notification.time());
        assertEquals(0xAB, notification.status());
        assertEquals(0x0102_0304_0506_07L, notification.frequency());
        assertNull(JtdpMessageReader.read(input));
    }

    @Test
    void handlesUnavailableOffsetAndSnrEncodings() throws IOException {
        for (int availability : new int[]{0, 1, 2, 3}) {
            for (float snr : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 4.5F}) {
                JtdpMessageReader.Monitoring observation = assertInstanceOf(JtdpMessageReader.Monitoring.class,
                        read(monitoring(0, availability, 0, Integer.MIN_VALUE, snr)));
                assertNull(observation.offset());
                if (availability == 1 && Float.isFinite(snr)) {
                    assertEquals(4.5, observation.snr());
                } else {
                    assertNull(observation.snr());
                }
            }
        }
    }

    @Test
    void skipsUnknownTypesWithoutScanningTheirPayload() throws IOException {
        for (int type : new int[]{3, 0xEF, 0xF0, 0xFF}) {
            byte[] wire = join(message(type, 32768, new byte[]{(byte) 0xA5, 0, 0, 1}),
                    message(2, 1, ByteBuffer.allocate(17).put((byte) 255).array()));
            InputStream input = new ByteArrayInputStream(wire);
            JtdpMessageReader.Unknown unknown = assertInstanceOf(JtdpMessageReader.Unknown.class,
                    JtdpMessageReader.read(input));
            assertEquals(32768, unknown.antennaId());
            assertEquals(type, unknown.type());
            JtdpMessageReader.Event event = assertInstanceOf(JtdpMessageReader.Event.class,
                    JtdpMessageReader.read(input));
            assertEquals(255, event.code());
            assertNull(JtdpMessageReader.read(input));
        }
    }

    @Test
    void validatesUnsignedOuterLengthsAndInclusiveMaximum() throws IOException {
        for (long length : new long[]{0, 7, 1_048_577, 0x8000_0000L, 0xFFFF_FFFFL}) {
            assertThrows(IOException.class, () -> read(header(3, 0, length)));
        }
        assertInstanceOf(JtdpMessageReader.Unknown.class,
                read(message(3, 0, new byte[JtdpMessageReader.MAX_MESSAGE_BYTES - 8])));
        assertInstanceOf(JtdpMessageReader.Unknown.class, read(message(3, 0, new byte[0])));
        assertThrows(IOException.class, () -> read(new byte[]{0}));
    }

    @Test
    void rejectsFixedLayoutsWithExtraOrMissingBytes() {
        for (int type : new int[]{1, 2}) {
            int payloadLength = type == 1 ? 32 : 17;
            assertThrows(IOException.class, () -> read(message(type, 0, new byte[payloadLength - 1])));
            assertThrows(IOException.class, () -> read(message(type, 0, new byte[payloadLength + 1])));
        }
    }

    @Test
    void rejectsMalformedBatchIncludingInvalidFinalRecord() {
        assertThrows(IOException.class, () -> read(message(0, 0, new byte[0])));
        assertThrows(IOException.class, () -> read(message(0, 0, frame(3, 0, new byte[]{1}))));
        assertThrows(IOException.class, () -> read(message(0, 0, frame(0, 0, new byte[0]))));
        assertThrows(IOException.class, () -> read(message(0, 0,
                ByteBuffer.allocate(13).put((byte) 0).putLong(0).putInt(-1).array())));
        assertThrows(IOException.class, () -> read(message(0, 0,
                ByteBuffer.allocate(14).put((byte) 0).putLong(0).putInt(2).put((byte) 1).array())));
        assertThrows(IOException.class, () -> read(message(0, 0,
                join(frame(0, 0, new byte[]{1}), new byte[]{0}))));
    }

    @Test
    void distinguishesCleanEofFromTruncatedHeadersAndPayloads() throws IOException {
        assertNull(read(new byte[0]));
        byte[][] messages = {
                message(0, 1, frame(0, 0, new byte[]{1, 2, 3})),
                monitoring(1, 0, 0, 0, Float.NaN),
                message(2, 1, new byte[17]),
                message(255, 1, new byte[]{1, 2, 3})
        };
        for (byte[] message : messages) {
            for (int length = 1; length < message.length; ++length) {
                byte[] truncated = Arrays.copyOf(message, length);
                assertThrows(IOException.class, () -> read(truncated), "Prefix length " + length);
            }
        }
    }

    private static JtdpMessageReader.Message read(byte[] bytes) throws IOException {
        return JtdpMessageReader.read(new ByteArrayInputStream(bytes));
    }

    private static byte[] monitoring(int antennaId, int status, long frequency, int offset, float snr) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.putLong(frequency | ((long) status << 56));
        buffer.putInt(offset).putFloat(snr).putInt(-1).putInt(Integer.MIN_VALUE)
                .putInt(Integer.MAX_VALUE).putInt(0);
        return message(1, antennaId, buffer.array());
    }

    private static byte[] frame(int quality, long time, byte[] data) {
        return ByteBuffer.allocate(13 + data.length).put((byte) quality).putLong(time)
                .putInt(data.length).put(data).array();
    }

    private static byte[] message(int type, int antennaId, byte[] payload) {
        return join(header(type, antennaId, 8L + payload.length), payload);
    }

    private static byte[] header(int type, int antennaId, long length) {
        return ByteBuffer.allocate(8).put((byte) 0xA5).put((byte) type)
                .putShort((short) antennaId).putInt((int) length).array();
    }

    private static byte[] join(byte[]... arrays) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            output.writeBytes(array);
        }
        return output.toByteArray();
    }
}
