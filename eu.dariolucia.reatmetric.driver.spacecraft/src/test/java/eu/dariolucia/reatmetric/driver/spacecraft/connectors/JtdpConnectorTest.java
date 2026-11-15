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

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class JtdpConnectorTest {

    private static final String PARENT = "GROUND.JTDP";
    private static final String ROUTE = "Antenna North";
    private static final int ANTENNA_ID = 65535;
    private static final long TIME_NS = 1_700_000_000_123_456_789L;
    private static final Instant FRAME_TIME = Instant.ofEpochSecond(1_700_000_000L, 123_456_789);
    private static final List<String> PARAMETERS = List.of("CARRIER_LOCK", "SYMBOL_LOCK", "FRAME_LOCK",
            "SNR_AVAILABLE", "CENTER_FREQUENCY", "CARRIER_OFFSET", "SNR", "TOTAL_FRAMES", "ERROR_FRAMES",
            "GOOD_FRAMES", "DISTRIBUTED_FRAMES");
    private static final List<String> EVENTS = List.of("DECODER_STARTED", "DECODER_STOPPED", "CARRIER_STATE_CHANGED",
            "SYMBOL_STATE_CHANGED", "FRAME_STATE_CHANGED", "DECODER_ERROR", "DISTRIBUTION_OVERRUN", "DATA_DISCARDED");

    @Test
    void publishesFramesAllMonitoringFieldsAndEventsUsingNumericModelPaths() throws Exception {
        try (ServerSocket server = server(); Harness harness = new Harness(server.getLocalPort())) {
            harness.connector.connect();
            try (Socket peer = server.accept()) {
                ByteArrayOutputStream messages = new ByteArrayOutputStream();
                messages.writeBytes(message(0, join(frame(0, TIME_NS, new byte[]{1, 2, 3}),
                        frame(1, TIME_NS + 1, new byte[]{4, 5}))));
                messages.writeBytes(monitoring(0x65, 0x00FF_FFFF_FFFF_FFFFL, -1234, 12.5F));
                messages.writeBytes(monitoring(0xFF, 1, Integer.MIN_VALUE, Float.NaN));
                messages.writeBytes(message(0xF0, new byte[]{(byte) 0xA5, 0, 1}));
                messages.writeBytes(event(200, TIME_NS, 0x65, 1));
                for (int code = 0; code < EVENTS.size(); ++code) {
                    messages.writeBytes(event(code, TIME_NS + code, 0x65, 437_100_000));
                }
                peer.getOutputStream().write(messages.toByteArray());
                peer.getOutputStream().flush();

                RawData first = take(harness.frames);
                RawData second = take(harness.frames);
                assertArrayEquals(new byte[]{1, 2, 3}, first.getContents());
                assertArrayEquals(new byte[]{4, 5}, second.getContents());
                assertEquals(Quality.GOOD, first.getQuality());
                assertEquals(Quality.BAD, second.getQuality());
                assertEquals(FRAME_TIME, first.getReceptionTime());
                assertEquals(FRAME_TIME.minus(10, ChronoUnit.MICROS), first.getGenerationTime());
                assertEquals(FRAME_TIME.plusNanos(1), second.getReceptionTime());
                assertEquals(ROUTE, first.getRoute());
                assertEquals("321", first.getSource());
                assertEquals(Constants.T_RAW_FRAME, first.getType());
                assertEquals("jtdp-test-driver", first.getHandler());
                assertTrue(first.isLive());

                List<ParameterSample> observation = take(harness.parameters);
                Map<String, Object> expected = Map.ofEntries(
                        Map.entry("CARRIER_LOCK", 1L), Map.entry("SYMBOL_LOCK", 2L), Map.entry("FRAME_LOCK", 1L),
                        Map.entry("SNR_AVAILABLE", true), Map.entry("CENTER_FREQUENCY", 0x00FF_FFFF_FFFF_FFFFL),
                        Map.entry("CARRIER_OFFSET", -1234L), Map.entry("SNR", 12.5),
                        Map.entry("TOTAL_FRAMES", 0xFFFF_FFFFL), Map.entry("ERROR_FRAMES", 0x8000_0000L),
                        Map.entry("GOOD_FRAMES", 0x7FFF_FFFFL), Map.entry("DISTRIBUTED_FRAMES", 0L));
                assertEquals(expected, harness.values(observation));
                observation.forEach(sample -> {
                    assertEquals(ROUTE, sample.getRoute());
                    assertEquals(sample.getGenerationTime(), sample.getReceptionTime());
                });
                Map<String, Object> unavailable = harness.values(take(harness.parameters));
                assertEquals(11, unavailable.size());
                assertEquals(false, unavailable.get("SNR_AVAILABLE"));
                assertNull(unavailable.get("CARRIER_OFFSET"));
                assertNull(unavailable.get("SNR"));
                assertEquals(3L, unavailable.get("FRAME_LOCK"));

                for (int code = 0; code < EVENTS.size(); ++code) {
                    EventOccurrence event = take(harness.events);
                    assertEquals(harness.descriptors.get(path(EVENTS.get(code))).getExternalId(), event.getId());
                    assertEquals(FRAME_TIME.plusNanos(code), event.getGenerationTime());
                    assertEquals(ROUTE, event.getRoute());
                    assertEquals(Integer.toString(ANTENNA_ID), event.getSource());
                    Map<?, ?> report = assertInstanceOf(Map.class, event.getReport());
                    assertEquals(0x65L, report.get("STATUS"));
                    assertEquals(437_100_000L, report.get("CENTER_FREQUENCY"));
                    List<ParameterSample> eventSnapshot = take(harness.parameters);
                    assertEquals(5, eventSnapshot.size());
                    int eventCode = code;
                    eventSnapshot.forEach(sample -> assertEquals(FRAME_TIME.plusNanos(eventCode), sample.getGenerationTime()));
                }
                assertTrue(harness.parameters.isEmpty(), "Reserved message and event codes must not inject status");
                assertTrue(harness.events.isEmpty());
                assertTrue(harness.frames.isEmpty());
                assertEquals(TransportConnectionStatus.OPEN, harness.connector.getConnectionStatus());
            }
        }
    }

    @Test
    void rejectsMalformedAndTruncatedBatchesWithoutPublishingTheirValidPrefix() throws Exception {
        byte[] malformed = message(0, join(frame(0, TIME_NS, new byte[]{1, 2, 3}),
                frame(3, TIME_NS, new byte[]{4})));
        byte[] complete = message(0, join(frame(0, TIME_NS, new byte[]{1, 2, 3}),
                frame(0, TIME_NS, new byte[]{4})));
        byte[] truncated = Arrays.copyOf(complete, complete.length - 1);
        for (byte[] wire : new byte[][]{malformed, truncated}) {
            try (ServerSocket server = server(); Harness harness = new Harness(server.getLocalPort())) {
                harness.connector.connect();
                try (Socket peer = server.accept()) {
                    peer.setSoTimeout(5000);
                    peer.getOutputStream().write(wire);
                    peer.getOutputStream().flush();
                    peer.shutdownOutput();
                    awaitStatus(harness.connector, TransportConnectionStatus.ERROR);
                    assertEquals(-1, peer.getInputStream().read(), "Protocol failure must close the client socket");
                    assertTrue(harness.frames.isEmpty(), "A malformed batch must not publish its valid prefix");
                    assertTrue(harness.parameters.isEmpty());
                    assertTrue(harness.events.isEmpty());
                }
            }
        }
    }

    @Test
    void disconnectsBlockedReaderReconnectsAndReportsCleanServerEof() throws Exception {
        try (ServerSocket server = server(); Harness harness = new Harness(server.getLocalPort())) {
            harness.connector.connect();
            try (Socket firstSession = server.accept()) {
                firstSession.setSoTimeout(5000);
                assertTimeoutPreemptively(Duration.ofSeconds(3), harness.connector::disconnect);
                assertEquals(TransportConnectionStatus.IDLE, harness.connector.getConnectionStatus());
                assertEquals(-1, firstSession.getInputStream().read());
            }
            harness.connector.connect();
            try (Socket nextSession = server.accept()) {
                nextSession.setSoTimeout(5000);
                nextSession.getOutputStream().write(message(0, frame(2, TIME_NS, new byte[]{6, 7})));
                nextSession.getOutputStream().flush();
                assertEquals(Quality.UNKNOWN, take(harness.frames).getQuality());
                assertEquals(TransportConnectionStatus.OPEN, harness.connector.getConnectionStatus());
                nextSession.shutdownOutput();
                awaitStatus(harness.connector, TransportConnectionStatus.ERROR);
                assertEquals(-1, nextSession.getInputStream().read());
            }
        }
    }

    private static void awaitStatus(JtdpConnector connector, TransportConnectionStatus expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (connector.getConnectionStatus() != expected && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(expected, connector.getConnectionStatus());
    }

    private static <T> T take(BlockingQueue<T> queue) throws InterruptedException {
        T value = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(value, "Timed out waiting for connector publication");
        return value;
    }

    private static ServerSocket server() throws IOException {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        server.setSoTimeout(5000);
        return server;
    }

    private static String path(String name) {
        return PARENT + "." + ANTENNA_ID + "." + name;
    }

    private static byte[] frame(int quality, long time, byte[] bytes) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(payload);
        output.writeByte(quality);
        output.writeLong(time);
        output.writeInt(bytes.length);
        output.write(bytes);
        return payload.toByteArray();
    }

    private static byte[] monitoring(int status, long frequency, int offset, float snr) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(payload);
        output.writeByte(status);
        frequency(output, frequency);
        output.writeInt(offset);
        output.writeFloat(snr);
        output.writeInt(-1);
        output.writeInt(Integer.MIN_VALUE);
        output.writeInt(Integer.MAX_VALUE);
        output.writeInt(0);
        return message(1, payload.toByteArray());
    }

    private static byte[] event(int code, long time, int status, long frequency) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(payload);
        output.writeByte(code);
        output.writeLong(time);
        output.writeByte(status);
        frequency(output, frequency);
        return message(2, payload.toByteArray());
    }

    private static void frequency(DataOutputStream output, long frequency) throws IOException {
        for (int shift = 48; shift >= 0; shift -= 8) {
            output.writeByte((int) (frequency >>> shift));
        }
    }

    private static byte[] message(int type, byte[] payload) throws IOException {
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(wire);
        output.writeByte(0xA5);
        output.writeByte(type);
        output.writeShort(ANTENNA_ID);
        output.writeInt(8 + payload.length);
        output.write(payload);
        return wire.toByteArray();
    }

    private static byte[] join(byte[]... arrays) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] array : arrays) {
            output.writeBytes(array);
        }
        return output.toByteArray();
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static final class Harness implements AutoCloseable {
        private final JtdpConnector connector = new JtdpConnector();
        private final BlockingQueue<RawData> frames = new LinkedBlockingQueue<>();
        private final BlockingQueue<List<ParameterSample>> parameters = new LinkedBlockingQueue<>();
        private final BlockingQueue<EventOccurrence> events = new LinkedBlockingQueue<>();
        private final Map<String, AbstractSystemEntityDescriptor> descriptors = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        private Harness(int port) {
            for (int i = 0; i < PARAMETERS.size(); ++i) {
                String name = PARAMETERS.get(i);
                ValueTypeEnum type = name.equals("SNR_AVAILABLE") ? ValueTypeEnum.BOOLEAN
                        : name.equals("SNR") ? ValueTypeEnum.REAL : ValueTypeEnum.SIGNED_INTEGER;
                descriptors.put(path(name), new ParameterDescriptor(SystemEntityPath.fromString(path(name)), 100 + i,
                        name, type, type, null, false, false, false, null, null, null, null));
            }
            for (int i = 0; i < EVENTS.size(); ++i) {
                String name = EVENTS.get(i);
                descriptors.put(path(name), new EventDescriptor(SystemEntityPath.fromString(path(name)), 200 + i,
                        name, Severity.INFO, "JTDP", false));
            }
            IProcessingModel model = proxy(IProcessingModel.class, (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "getDescriptorOf":
                        AbstractSystemEntityDescriptor descriptor = descriptors.get(arguments[0].toString());
                        assertNotNull(descriptor, "Unexpected model path: " + arguments[0]);
                        return descriptor;
                    case "injectParameters":
                        parameters.add(List.copyOf((List<ParameterSample>) arguments[0]));
                        return null;
                    case "raiseEvent":
                        events.add((EventOccurrence) arguments[0]);
                        return null;
                    default:
                        throw new AssertionError("Unexpected processing model call: " + method.getName());
                }
            });
            AtomicLong rawId = new AtomicLong();
            IRawDataBroker broker = proxy(IRawDataBroker.class, (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "nextRawDataId":
                        return new LongUniqueId(rawId.incrementAndGet());
                    case "distribute":
                        frames.addAll((List<RawData>) arguments[0]);
                        return null;
                    default:
                        throw new AssertionError("Unexpected broker call: " + method.getName());
                }
            });
            IServiceCoreContext context = proxy(IServiceCoreContext.class, (proxy, method, arguments) -> {
                switch (method.getName()) {
                    case "getProcessingModel":
                        return model;
                    case "getRawDataBroker":
                        return broker;
                    default:
                        throw new AssertionError("Unexpected context call: " + method.getName());
                }
            });
            SpacecraftConfiguration configuration = new SpacecraftConfiguration();
            configuration.setId(321);
            configuration.setPropagationDelay(10);
            connector.configure("jtdp-test-driver", configuration, context, null,
                    "dataUrl=tcp://127.0.0.1:" + port + ";dataType=RAW;statusParentPath=" + PARENT +
                            ";antenna." + ANTENNA_ID + "=" + ROUTE, true);
            connector.prepare();
            connector.initialise(connector.getCurrentProperties());
        }

        private Map<String, Object> values(List<ParameterSample> samples) {
            Map<String, Object> values = new HashMap<>();
            for (ParameterSample sample : samples) {
                String name = PARAMETERS.get(sample.getId() - 100);
                values.put(name, sample.getValue());
            }
            return values;
        }

        @Override
        public void close() {
            connector.dispose();
        }
    }
}
