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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads complete, validated JTDP v1 messages. A protocol error is fatal to the
 * connection; the reader deliberately does not scan for another magic byte.
 */
final class JtdpMessageReader {

    static final int MAX_MESSAGE_BYTES = 1_048_576;
    private static final int HEADER_LENGTH = 8;
    private static final int FRAME_HEADER_LENGTH = 13;

    private JtdpMessageReader() {
        // Utility class
    }

    /**
     * @return the next complete message, or null on EOF between messages
     * @throws IOException on malformed messages or EOF within a message
     */
    static Message read(InputStream input) throws IOException {
        int magic = input.read();
        if (magic == -1) {
            return null;
        }
        if (magic != 0xA5) {
            throw new IOException("Invalid JTDP message magic: " + magic);
        }
        DataInputStream stream = new DataInputStream(input);
        int type = stream.readUnsignedByte();
        int antennaId = stream.readUnsignedShort();
        long length = Integer.toUnsignedLong(stream.readInt());
        if (length < HEADER_LENGTH || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Invalid JTDP message length: " + length);
        }
        int payloadLength = (int) length - HEADER_LENGTH;
        if (type > 2) {
            skipFully(stream, payloadLength);
            return new Unknown(antennaId, type);
        }
        if ((type == 1 && length != 40) || (type == 2 && length != 25)) {
            throw new IOException("Invalid JTDP type " + type + " message length: " + length);
        }
        byte[] payload = new byte[payloadLength];
        stream.readFully(payload);
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        switch (type) {
            case 0:
                return readFrames(antennaId, buffer);
            case 1:
                return readMonitoring(antennaId, buffer);
            case 2:
                return readEvent(antennaId, buffer);
            default:
                throw new AssertionError("Unreachable message type: " + type);
        }
    }

    private static void skipFully(DataInputStream input, int count) throws IOException {
        byte[] scratch = new byte[Math.min(count, 4096)];
        while (count > 0) {
            int toRead = Math.min(count, scratch.length);
            input.readFully(scratch, 0, toRead);
            count -= toRead;
        }
    }

    private static FrameBatch readFrames(int antennaId, ByteBuffer buffer) throws IOException {
        List<Frame> frames = new ArrayList<>();
        while (buffer.hasRemaining()) {
            if (buffer.remaining() < FRAME_HEADER_LENGTH) {
                throw new IOException("Incomplete JTDP frame record header");
            }
            int qualityCode = Byte.toUnsignedInt(buffer.get());
            Quality quality;
            switch (qualityCode) {
                case 0:
                    quality = Quality.GOOD;
                    break;
                case 1:
                    quality = Quality.BAD;
                    break;
                case 2:
                    quality = Quality.UNKNOWN;
                    break;
                default:
                    throw new IOException("Reserved JTDP frame quality: " + qualityCode);
            }
            Instant receptionTime = toInstant(buffer.getLong());
            long frameLength = Integer.toUnsignedLong(buffer.getInt());
            if (frameLength == 0 || frameLength > buffer.remaining()) {
                throw new IOException("Invalid JTDP frame length: " + frameLength);
            }
            byte[] frame = new byte[(int) frameLength];
            buffer.get(frame);
            frames.add(new Frame(quality, receptionTime, frame));
        }
        if (frames.isEmpty()) {
            throw new IOException("Empty JTDP frame batch");
        }
        // Publish nothing until every record in the batch has been validated.
        return new FrameBatch(antennaId, List.copyOf(frames));
    }

    private static Monitoring readMonitoring(int antennaId, ByteBuffer buffer) {
        int status = Byte.toUnsignedInt(buffer.get());
        long frequency = readFrequency(buffer);
        int wireOffset = buffer.getInt();
        Long offset = wireOffset == Integer.MIN_VALUE ? null : (long) wireOffset;
        float wireSnr = buffer.getFloat();
        Double snr = (status & 3) == 1 && Float.isFinite(wireSnr) ? (double) wireSnr : null;
        return new Monitoring(antennaId, status, frequency, offset, snr,
                Integer.toUnsignedLong(buffer.getInt()), Integer.toUnsignedLong(buffer.getInt()),
                Integer.toUnsignedLong(buffer.getInt()), Integer.toUnsignedLong(buffer.getInt()));
    }

    private static Event readEvent(int antennaId, ByteBuffer buffer) {
        int code = Byte.toUnsignedInt(buffer.get());
        Instant time = toInstant(buffer.getLong());
        int status = Byte.toUnsignedInt(buffer.get());
        return new Event(antennaId, code, time, status, readFrequency(buffer));
    }

    private static long readFrequency(ByteBuffer buffer) {
        long frequency = 0;
        for (int i = 0; i < 7; ++i) {
            frequency = (frequency << 8) | Byte.toUnsignedLong(buffer.get());
        }
        return frequency;
    }

    private static Instant toInstant(long nanoseconds) {
        return Instant.ofEpochSecond(Math.floorDiv(nanoseconds, 1_000_000_000L),
                Math.floorMod(nanoseconds, 1_000_000_000L));
    }

    interface Message {
        int antennaId();
    }

    record FrameBatch(int antennaId, List<Frame> frames) implements Message {
    }

    record Frame(Quality quality, Instant receptionTime, byte[] data) {
    }

    record Monitoring(int antennaId, int status, long frequency, Long offset, Double snr,
                      long totalFrames, long errorFrames, long goodFrames, long distributedFrames) implements Message {
    }

    record Event(int antennaId, int code, Instant time, int status, long frequency) implements Message {
    }

    record Unknown(int antennaId, int type) implements Message {
    }
}
