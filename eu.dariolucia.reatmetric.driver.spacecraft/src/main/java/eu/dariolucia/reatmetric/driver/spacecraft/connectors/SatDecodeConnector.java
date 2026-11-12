/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AosTransferFrame;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.IReceptionOnlyConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.RawTransferFrame;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import org.zeromq.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reception-only connector for SatDecode v1 frame and monitoring streams.
 */
public class SatDecodeConnector extends AbstractTransportConnector implements IReceptionOnlyConnector {

    private static final Logger LOG = Logger.getLogger(SatDecodeConnector.class.getName());

    public static final String DATA_URL_KEY = "dataUrl";
    public static final String DATA_TYPE_KEY = "dataType";
    public static final String STATUS_URL_KEY = "statusUrl";
    public static final String STATUS_PARENT_PATH_KEY = "statusParentPath";
    public static final String DATA_THROTTLE_KEY = "dataThrottle";

    private static final String FRAME_TOPIC = "satdecode.v1.frame";
    private static final String STATUS_TOPIC = "satdecode.v1.monitor.status";
    private static final String EVENT_TOPIC = "satdecode.v1.monitor.event";

    private static final String MESSAGE_TYPE_TM_FRAME = "tm.frame";
    private static final String MESSAGE_TYPE_MONITOR_STATUS = "monitor.status";
    private static final String MESSAGE_TYPE_MONITOR_EVENT = "monitor.event";

    private static final String ENVELOPE_SCHEMA_VERSION_KEY = "schema_version";
    private static final String ENVELOPE_MESSAGE_TYPE_KEY = "message_type";
    private static final String ENVELOPE_RECEIVER_ID_KEY = "receiver_id";
    private static final String ENVELOPE_DECODER_KEY = "decoder";
    private static final String ENVELOPE_SEQUENCE_KEY = "sequence";
    private static final String ENVELOPE_RX_TIME_NS_KEY = "rx_time_ns";
    private static final String ENVELOPE_PUBLISH_TIME_NS_KEY = "publish_time_ns";
    private static final String ENVELOPE_TIMESTAMP_NS_KEY = "timestamp_ns";
    private static final String ENVELOPE_FRAME_TYPE_KEY = "frame_type"; // This can be CCSDS_AOS_TRANSFER_FRAME or
    private static final String ENVELOPE_FRAME_LENGTH_KEY = "frame_length";
    private static final String ENVELOPE_QUALITY_KEY = "quality";
    private static final String ENVELOPE_METADATA_KEY = "metadata";
    private static final String ENVELOPE_METRICS_KEY = "metrics";

    private static final String METRIC_FRAME_LOCK_KEY = "frame.lock";
    private static final String METRIC_FRAME_LOCK_LOCKED_VALUE = "LOCKED";

    private static final String METADATA_SPACECRAFT_ID_KEY = "spacecraft_id";
    private static final String METADATA_VCID_KEY = "vcid";
    private static final String METADATA_VIRTUAL_CHANNEL_ID_KEY = "virtual_channel_id";
    private static final String METADATA_NAME_KEY = "name";
    private static final String METADATA_VCA_NAME_KEY = "vca_name";
    private static final String METADATA_PACKET_NAME_KEY = "packet_name";

    private static final String FALLBACK_RECEIVER_ID = "unknown-receiver";
    private static final String FALLBACK_DECODER = "unknown-decoder";

    private static final int SCHEMA_VERSION = 1;
    private static final byte[] FRAME_TOPIC_BYTES = FRAME_TOPIC.getBytes(StandardCharsets.US_ASCII);
    private static final int PUSHBACK_BUFFER_SIZE = 1024 * 1024;

    private static final CBORFactory CBOR_FACTORY = new CBORFactory();
    private static final ObjectMapper CBOR = new ObjectMapper(CBOR_FACTORY);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    static {
        CBOR_FACTORY.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    private final Map<String, Object> configurationDefaults = new TreeMap<>();
    private final AtomicLong rxBytes = new AtomicLong(0);
    private final Map<String, ParameterDescriptor> statusParameterCache = new ConcurrentHashMap<>();
    private final Set<String> missingStatusParametersLogged = Collections.synchronizedSet(new HashSet<>());

    private volatile Instant lastSamplingTime;
    private volatile SpacecraftConfiguration spacecraftConfiguration;
    private volatile IServiceCoreContext context;
    private volatile IRawDataBroker broker;
    private volatile String driverName;
    private volatile boolean live = true;
    private volatile boolean running;
    private volatile Thread dataThread;
    private volatile Thread statusThread;
    private volatile Thread monitorThread;
    private volatile ZContext zmqContext;
    private volatile ZMQ.Socket dataSocket;
    private volatile ZMQ.Socket statusSocket;
    private volatile ZMQ.Socket monitorSocket;
    private volatile boolean zmqProviderAvailable = true;

    public SatDecodeConnector() {
        super("SatDecode Connector", "SatDecode telemetry connector");
    }

    @Override
    protected void addToInitialisationMap(Map<String, Object> initialisationMap, Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap) {
        initialisationDescriptionMap.put(DATA_URL_KEY, Pair.of("SatDecode data URL: zmq://<host>:<port> or file://<file path>", ValueTypeEnum.CHARACTER_STRING));
        initialisationDescriptionMap.put(DATA_TYPE_KEY, Pair.of("SatDecode data type: TM_FRAME, AOS_FRAME or VCA", ValueTypeEnum.CHARACTER_STRING));
        initialisationDescriptionMap.put(STATUS_URL_KEY, Pair.of("SatDecode status URL: zmq://<host>:<port> or empty", ValueTypeEnum.CHARACTER_STRING));
        initialisationDescriptionMap.put(STATUS_PARENT_PATH_KEY, Pair.of("Processing model parent path for SatDecode status metrics", ValueTypeEnum.CHARACTER_STRING));
        initialisationDescriptionMap.put(DATA_THROTTLE_KEY, Pair.of("File replay throttle in data units per second, 0 disabled", ValueTypeEnum.UNSIGNED_INTEGER));

        initialisationMap.put(DATA_URL_KEY, configurationDefaults.getOrDefault(DATA_URL_KEY, ""));
        initialisationMap.put(DATA_TYPE_KEY, configurationDefaults.getOrDefault(DATA_TYPE_KEY, SatDecodeDataType.TM_FRAME.name()));
        initialisationMap.put(STATUS_URL_KEY, configurationDefaults.getOrDefault(STATUS_URL_KEY, ""));
        initialisationMap.put(STATUS_PARENT_PATH_KEY, configurationDefaults.getOrDefault(STATUS_PARENT_PATH_KEY, ""));
        initialisationMap.put(DATA_THROTTLE_KEY, configurationDefaults.getOrDefault(DATA_THROTTLE_KEY, 0L));
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        Instant now = Instant.now();
        if (lastSamplingTime != null) {
            long theBytes = rxBytes.getAndSet(0);
            long msDiff = now.toEpochMilli() - lastSamplingTime.toEpochMilli();
            lastSamplingTime = now;
            if (msDiff <= 0) {
                return Pair.of(0L, 0L);
            }
            long rxRate = Math.round((theBytes * 8000.0) / msDiff);
            return Pair.of(0L, rxRate);
        } else {
            lastSamplingTime = now;
            return null;
        }
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        if (running) {
            throw new TransportException("SatDecode connector already running");
        }
        SatDecodeRuntimeConfiguration configuration = currentConfiguration();
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        updateAlarmState(AlarmState.NOT_CHECKED);
        running = true;
        if (configuration.fileBased()) {
            connectFile(configuration);
        } else {
            connectZmq(configuration);
        }
    }

    private void connectFile(SatDecodeRuntimeConfiguration configuration) throws TransportException {
        try {
            Path file = toFilePath(configuration.dataUrl());
            if (!Files.isReadable(file)) {
                throw new IOException("File does not exist or cannot be read: " + file);
            }
            dataThread = new Thread(() -> readFile(configuration, file), getName() + " file reader");
            dataThread.setDaemon(true);
            updateConnectionStatus(TransportConnectionStatus.OPEN);
            updateAlarmState(AlarmState.NOMINAL);
            dataThread.start();
        } catch (IOException e) {
            running = false;
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            updateAlarmState(AlarmState.ALARM);
            throw new TransportException(e);
        }
    }

    private void connectZmq(SatDecodeRuntimeConfiguration configuration) throws TransportException {
        try {
            zmqProviderAvailable = true;
            zmqContext = new ZContext(1);
            dataSocket = zmqContext.createSocket(SocketType.SUB);
            dataSocket.subscribe(FRAME_TOPIC);
            dataSocket.setReceiveTimeOut(1000);
            dataSocket.setLinger(0);
            String monitorEndpoint = "inproc://satdecode-data-monitor-" + System.identityHashCode(this);
            dataSocket.monitor(monitorEndpoint, ZMQ.EVENT_CONNECTED | ZMQ.EVENT_CONNECT_DELAYED |
                    ZMQ.EVENT_CONNECT_RETRIED | ZMQ.EVENT_DISCONNECTED);
            monitorSocket = zmqContext.createSocket(SocketType.PAIR);
            monitorSocket.setReceiveTimeOut(1000);
            monitorSocket.setLinger(0);
            monitorSocket.connect(monitorEndpoint);
            monitorThread = new Thread(() -> monitorZmqDataSocket(configuration), getName() + " data socket monitor");
            monitorThread.setDaemon(true);
            if (!dataSocket.connect(configuration.dataUrl())) {
                throw new IOException("Cannot connect data socket to " + configuration.dataUrl());
            }
            dataThread = new Thread(() -> readZmqData(configuration), getName() + " data reader");
            dataThread.setDaemon(true);

            if (configuration.statusUsed()) {
                statusSocket = zmqContext.createSocket(SocketType.SUB);
                statusSocket.subscribe(STATUS_TOPIC);
                statusSocket.setReceiveTimeOut(1000);
                statusSocket.setLinger(0);
                if (!statusSocket.connect(configuration.statusUrl())) {
                    throw new IOException("Cannot connect status socket to " + configuration.statusUrl());
                }
                statusThread = new Thread(() -> readZmqStatus(configuration), getName() + " status reader");
                statusThread.setDaemon(true);
            }

            monitorThread.start();
            dataThread.start();
            if (statusThread != null) {
                statusThread.start();
            }
        } catch (IOException | RuntimeException e) {
            running = false;
            closeZmq();
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            updateAlarmState(AlarmState.ALARM);
            throw new TransportException(e);
        }
    }

    private void monitorZmqDataSocket(SatDecodeRuntimeConfiguration configuration) {
        while (running) {
            ZMQ.Socket socket = monitorSocket;
            if (socket == null) {
                break;
            }
            ZMQ.Event event = ZMQ.Event.recv(socket, 0);
            if (event == null) {
                continue;
            }
            switch (event.getEvent()) {
                case ZMQ.EVENT_CONNECTED:
                    zmqProviderAvailable = true;
                    LOG.info(getName() + ": SatDecode ZeroMQ provider " + configuration.dataUrl() + " connection established");
                    updateConnectionStatus(TransportConnectionStatus.OPEN);
                    if (!configuration.statusUsed()) {
                        updateAlarmState(AlarmState.NOMINAL);
                    }
                    break;
                case ZMQ.EVENT_CONNECT_DELAYED:
                case ZMQ.EVENT_CONNECT_RETRIED:
                case ZMQ.EVENT_DISCONNECTED:
                    if (zmqProviderAvailable) {
                        LOG.warning(getName() + ": SatDecode ZeroMQ provider " + configuration.dataUrl() + " is not currently available");
                    }
                    zmqProviderAvailable = false;
                    updateAlarmState(AlarmState.ERROR);
                    updateConnectionStatus(TransportConnectionStatus.CONNECTING);
                    break;
                default:
                    break;
            }
        }
    }

    private void readFile(SatDecodeRuntimeConfiguration configuration, Path file) {
        boolean errorDetected = false;
        try (PushbackInputStream inputStream = new PushbackInputStream(new BufferedInputStream(new FileInputStream(file.toFile())), PUSHBACK_BUFFER_SIZE)) {
            while (running) {
                SatDecodeFrameMessage message = readFileMessage(inputStream);
                if (message == null) {
                    break;
                }
                processFrameMessage(configuration, message, Instant.now());
                throttle(configuration.dataThrottle());
            }
        } catch (Exception e) {
            if (running) {
                LOG.log(Level.SEVERE, getName() + ": SatDecode file reader failed: " + e.getMessage(), e);
                updateAlarmState(AlarmState.ALARM);
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                errorDetected = true;
            }
        } finally {
            running = false;
            if (!errorDetected) {
                updateConnectionStatus(TransportConnectionStatus.IDLE);
                updateAlarmState(AlarmState.NOT_APPLICABLE);
            }
        }
    }

    private SatDecodeFrameMessage readFileMessage(PushbackInputStream inputStream) throws IOException {
        byte[] topic = readTopic(inputStream);
        if (topic == null) {
            return null;
        }
        String topicString = new String(topic, StandardCharsets.US_ASCII);
        if (!FRAME_TOPIC.equals(topicString)) {
            throw new IOException("Unsupported SatDecode file topic: " + topicString);
        }
        JsonParser parser = CBOR_FACTORY.createParser(inputStream);
        Map<String, Object> envelope = CBOR.readValue(parser, MAP_TYPE);
        ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream();
        parser.releaseBuffered(bufferedOutput);
        parser.close();

        validateFrameEnvelope(topicString, envelope);
        int frameLength = toRequiredNumber(envelope, ENVELOPE_FRAME_LENGTH_KEY).intValue();
        byte[] payload = new byte[frameLength];
        byte[] buffered = bufferedOutput.toByteArray();
        int copied = Math.min(frameLength, buffered.length);
        if (copied > 0) {
            System.arraycopy(buffered, 0, payload, 0, copied);
        }
        if (buffered.length > copied) {
            inputStream.unread(buffered, copied, buffered.length - copied);
        }
        readFully(inputStream, payload, copied, frameLength - copied);
        return new SatDecodeFrameMessage(topicString, envelope, payload);
    }

    private byte[] readTopic(PushbackInputStream inputStream) throws IOException {
        int first = inputStream.read();
        if (first == -1) {
            return null;
        }
        byte[] topic = new byte[FRAME_TOPIC_BYTES.length];
        topic[0] = (byte) first;
        readFully(inputStream, topic, 1, topic.length - 1);
        return topic;
    }

    private void readZmqData(SatDecodeRuntimeConfiguration configuration) {
        try {
            while (running) {
                ZMQ.Socket socket = dataSocket;
                if (socket == null) {
                    break;
                }
                ZMsg message = ZMsg.recvMsg(socket);
                if (message == null) {
                    continue;
                }
                try {
                    SatDecodeFrameMessage frameMessage = decodeFrameMessage(message);
                    processFrameMessage(configuration, frameMessage, Instant.now());
                } finally {
                    message.destroy();
                }
            }
        } catch (Exception e) {
            if (running) {
                LOG.log(Level.SEVERE, getName() + ": SatDecode ZMQ data reader failed: " + e.getMessage(), e);
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                updateAlarmState(AlarmState.ALARM);
            }
        }
    }

    private void readZmqStatus(SatDecodeRuntimeConfiguration configuration) {
        try {
            while (running) {
                ZMQ.Socket socket = statusSocket;
                if (socket == null) {
                    break;
                }
                ZMsg message = ZMsg.recvMsg(socket);
                if (message == null) {
                    continue;
                }
                try {
                    SatDecodeMonitorMessage monitorMessage = decodeMonitorMessage(message);
                    if (STATUS_TOPIC.equals(monitorMessage.topic())) {
                        processStatusMessage(configuration, monitorMessage.envelope());
                    }
                } finally {
                    message.destroy();
                }
            }
        } catch (Exception e) {
            if (running) {
                LOG.log(Level.SEVERE, getName() + ": SatDecode ZMQ status reader failed: " + e.getMessage(), e);
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                updateAlarmState(AlarmState.ALARM);
            }
        }
    }

    private SatDecodeFrameMessage decodeFrameMessage(ZMsg message) throws IOException {
        ZFrame topicFrame = requireFrame(message.pop(), "topic");
        ZFrame envelopeFrame = requireFrame(message.pop(), "CBOR envelope");
        ZFrame payloadFrame = requireFrame(message.pop(), "frame payload");
        if (message.size() != 0) {
            throw new IOException("Frame message contains unexpected extra parts");
        }
        String topic = new String(topicFrame.getData(), StandardCharsets.US_ASCII);
        Map<String, Object> envelope = CBOR.readValue(envelopeFrame.getData(), MAP_TYPE);
        byte[] payload = payloadFrame.getData().clone();
        validateFrameEnvelope(topic, envelope);
        int declaredLength = toRequiredNumber(envelope, ENVELOPE_FRAME_LENGTH_KEY).intValue();
        if (declaredLength != payload.length) {
            throw new IOException("frame_length does not match payload length");
        }
        return new SatDecodeFrameMessage(topic, envelope, payload);
    }

    private SatDecodeMonitorMessage decodeMonitorMessage(ZMsg message) throws IOException {
        ZFrame topicFrame = requireFrame(message.pop(), "topic");
        ZFrame envelopeFrame = requireFrame(message.pop(), "CBOR envelope");
        if (message.size() != 0) {
            throw new IOException("Monitor message contains unexpected extra parts");
        }
        String topic = new String(topicFrame.getData(), StandardCharsets.US_ASCII);
        Map<String, Object> envelope = CBOR.readValue(envelopeFrame.getData(), MAP_TYPE);
        validateMonitorEnvelope(topic, envelope);
        return new SatDecodeMonitorMessage(topic, envelope);
    }

    private void processFrameMessage(SatDecodeRuntimeConfiguration configuration, SatDecodeFrameMessage message, Instant localReceptionTime) {
        try {
            RawData rawData = mapToRawData(configuration, message, localReceptionTime);
            if (rawData != null) {
                broker.distribute(Collections.singletonList(rawData));
                rxBytes.addAndGet(message.payload().length);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, getName() + ": cannot process SatDecode frame: " + e.getMessage(), e);
            updateAlarmState(AlarmState.WARNING);
        }
    }

    private RawData mapToRawData(SatDecodeRuntimeConfiguration configuration, SatDecodeFrameMessage message, Instant localReceptionTime) {
        Map<String, Object> envelope = message.envelope();
        Map<String, Object> metadata = metadata(envelope);
        Instant receptionTime = instantFromNs(envelope.get(ENVELOPE_RX_TIME_NS_KEY), localReceptionTime);
        Instant generationTime = receptionTime.minus(spacecraftConfiguration.getPropagationDelay(), ChronoUnit.MICROS);
        Quality quality = mapQuality(envelope.get(ENVELOPE_QUALITY_KEY));
        String receiver = stringOrDefault(envelope.get(ENVELOPE_RECEIVER_ID_KEY), FALLBACK_RECEIVER_ID);
        String decoder = stringOrDefault(envelope.get(ENVELOPE_DECODER_KEY), FALLBACK_DECODER);

        switch (configuration.dataType()) {
            case TM_FRAME:
                return mapTmFrame(message.payload(), metadata, generationTime, receptionTime, quality, receiver, decoder);
            case AOS_FRAME:
                return mapAosFrame(message.payload(), metadata, generationTime, receptionTime, quality, receiver, decoder);
            case RAW:
                return mapRaw(envelope, message.payload(), metadata, generationTime, receptionTime, quality, receiver, decoder);
            default:
                throw new IllegalStateException("Unsupported data type " + configuration.dataType());
        }
    }

    private RawData mapTmFrame(byte[] payload, Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                               Quality quality, String receiver, String decoder) {
        TmTransferFrame frame = new TmTransferFrame(payload, spacecraftConfiguration.getTmDataLinkConfigurations().isFecfPresent());
        if (frame.isFecfPresent() && !frame.isValid()) {
            quality = Quality.BAD;
        }
        String source = String.valueOf(numberFromMetadata(metadata, METADATA_SPACECRAFT_ID_KEY, frame.getSpacecraftId()).intValue());
        String route = buildRoute(source, frame.getVirtualChannelId(), receiver, decoder);
        String type = quality == Quality.BAD ? Constants.T_BAD_TM : Constants.T_TM_FRAME;
        RawData rawData = newRawData(generationTime, Constants.N_TM_TRANSFER_FRAME, type, route, source, quality, payload, receptionTime, null);
        annotateFrame(frame, rawData, quality);
        rawData.setData(frame);
        return rawData;
    }

    private RawData mapAosFrame(byte[] payload, Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                                Quality quality, String receiver, String decoder) {
        AosTransferFrame frame = new AosTransferFrame(payload,
                spacecraftConfiguration.getTmDataLinkConfigurations().isAosFrameHeaderErrorControlPresent(),
                spacecraftConfiguration.getTmDataLinkConfigurations().getAosTransferFrameInsertZoneLength(),
                AosTransferFrame.UserDataType.M_PDU,
                spacecraftConfiguration.getTmDataLinkConfigurations().isOcfPresent(),
                spacecraftConfiguration.getTmDataLinkConfigurations().isFecfPresent());
        if (frame.isFecfPresent() && !frame.isValid()) {
            quality = Quality.BAD;
        }
        String source = String.valueOf(numberFromMetadata(metadata, METADATA_SPACECRAFT_ID_KEY, frame.getSpacecraftId()).intValue());
        String route = buildRoute(source, frame.getVirtualChannelId(), receiver, decoder);
        String type = quality == Quality.BAD ? Constants.T_BAD_TM : Constants.T_AOS_FRAME;
        RawData rawData = newRawData(generationTime, Constants.N_TM_TRANSFER_FRAME, type, route, source, quality, payload, receptionTime, null);
        annotateFrame(frame, rawData, quality);
        rawData.setData(frame);
        return rawData;
    }

    private RawData mapRaw(Map<String, Object> envelope, byte[] payload, Map<String, Object> metadata, Instant generationTime, Instant receptionTime,
                           Quality quality, String receiver, String decoder) {
        int spacecraftId = numberFromMetadata(metadata, METADATA_SPACECRAFT_ID_KEY, spacecraftConfiguration.getId()).intValue();
        Number vcid = numberFromMetadata(metadata, METADATA_VCID_KEY, null);
        if (vcid == null) {
            vcid = numberFromMetadata(metadata, METADATA_VIRTUAL_CHANNEL_ID_KEY, null);
        }
        String source = String.valueOf(spacecraftId);
        String route = vcid != null ? buildRoute(source, vcid.intValue(), receiver, decoder) : buildRoute(source, null, receiver, decoder);
        String name = firstString(metadata, Constants.N_UNKNOWN_VCA, METADATA_NAME_KEY, METADATA_VCA_NAME_KEY, METADATA_PACKET_NAME_KEY);
        // A raw transfer frame is assumed valid if the reported quality is GOOD or UNKNOWN, as we have no information in relation to the protocol
        RawTransferFrame unit = new RawTransferFrame((short) spacecraftId, envelope.getOrDefault(ENVELOPE_FRAME_TYPE_KEY, Constants.N_UNKNOWN_VCA).toString(), payload, false, quality != Quality.BAD);
        if (vcid != null) {
            unit.setAnnotationValue(Constants.ANNOTATION_VCID, vcid.intValue());
        }
        RawData rawData = newRawData(generationTime, name, Constants.T_RAW_FRAME, route, source, quality, payload, receptionTime, metadata.getOrDefault(ENVELOPE_FRAME_TYPE_KEY, Constants.N_UNKNOWN_VCA).toString());
        annotateFrame(unit, rawData, quality);
        rawData.setData(unit);
        return rawData;
    }

    private void annotateFrame(AbstractTransferFrame frame, RawData rawData, Quality frameQuality) {
        frame.setAnnotationValue(Constants.ANNOTATION_ROUTE, rawData.getRoute());
        frame.setAnnotationValue(Constants.ANNOTATION_SOURCE, rawData.getSource());
        frame.setAnnotationValue(Constants.ANNOTATION_GEN_TIME, rawData.getGenerationTime());
        frame.setAnnotationValue(Constants.ANNOTATION_RCP_TIME, rawData.getReceptionTime());
        frame.setAnnotationValue(Constants.ANNOTATION_UNIQUE_ID, rawData.getInternalId());
        // Remember the original frame quality as reported by the decoder or derived/overridden from FECF presence (if any)
        frame.setAnnotationValue(Constants.ANNOTATION_FRAME_QUALITY, frameQuality.ordinal());
    }

    private RawData newRawData(Instant generationTime, String name, String type, String route, String source,
                               Quality quality, byte[] contents, Instant receptionTime, Object extension) {
        return new RawData(broker.nextRawDataId(), generationTime, name, type, route, source, quality, null,
                contents, receptionTime, driverName, live, extension);
    }

    private void processStatusMessage(SatDecodeRuntimeConfiguration configuration, Map<String, Object> envelope) {
        Object metricsObject = envelope.get(ENVELOPE_METRICS_KEY);
        if (!(metricsObject instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> metricsMap = (Map<?, ?>) metricsObject;
        Instant generationTime = instantFromNs(envelope.get(ENVELOPE_TIMESTAMP_NS_KEY), Instant.now());
        Instant receptionTime = Instant.now();
        String receiver = stringOrDefault(envelope.get(ENVELOPE_RECEIVER_ID_KEY), FALLBACK_RECEIVER_ID);
        String decoder = stringOrDefault(envelope.get(ENVELOPE_DECODER_KEY), FALLBACK_DECODER);
        String route = receiver + "." + decoder;
        List<ParameterSample> samples = new ArrayList<>();
        for (Map.Entry<?, ?> entry : metricsMap.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                continue;
            }
            String key = (String) entry.getKey();
            ParameterDescriptor descriptor = resolveStatusParameter(configuration.statusParentPath(), key);
            if (descriptor == null) {
                continue;
            }
            try {
                Object value = ValueUtil.convert(normaliseValue(entry.getValue()), descriptor.getRawDataType());
                samples.add(ParameterSample.of(descriptor.getExternalId(), generationTime, receptionTime, null, value, route, null));
            } catch (ValueException e) {
                LOG.log(Level.FINE, getName() + ": cannot convert SatDecode status metric " + key + " to parameter " + descriptor.getPath() + ": " + e.getMessage(), e);
            }
        }
        if (!samples.isEmpty()) {
            context.getProcessingModel().injectParameters(samples);
        }
        Object frameLock = metricsMap.get(METRIC_FRAME_LOCK_KEY);
        AlarmState newAlarmState = Objects.equals(METRIC_FRAME_LOCK_LOCKED_VALUE, frameLock) ? AlarmState.NOMINAL : AlarmState.WARNING;
        if (newAlarmState == AlarmState.WARNING && getLastTransportStatus().getAlarmState() != AlarmState.WARNING) {
            LOG.warning(getName() + ": SatDecode frame lock lost for receiver " + receiver + ", decoder " + decoder);
        }
        updateAlarmState(newAlarmState);
    }

    private ParameterDescriptor resolveStatusParameter(String parentPath, String key) {
        String path = parentPath == null || parentPath.isBlank() ? key : parentPath + "." + key.toUpperCase().replace('.', '_');
        if (statusParameterCache.containsKey(path)) {
            return statusParameterCache.get(path);
        }
        if (missingStatusParametersLogged.contains(path)) {
            return null;
        }
        try {
            AbstractSystemEntityDescriptor descriptor = context.getProcessingModel().getDescriptorOf(SystemEntityPath.fromString(path));
            if (descriptor instanceof ParameterDescriptor) {
                ParameterDescriptor parameterDescriptor = (ParameterDescriptor) descriptor;
                statusParameterCache.put(path, parameterDescriptor);
                return parameterDescriptor;
            }
        } catch (ReatmetricException | IllegalArgumentException e) {
            // Handled below with a single debug line.
        }
        if (missingStatusParametersLogged.add(path)) {
            LOG.fine(getName() + ": SatDecode status parameter " + path + " not present in the processing model, skipping updates");
        }
        return null;
    }

    private Object normaliseValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Short) {
            return ((Short) value).longValue();
        }
        if (value instanceof Byte) {
            return ((Byte) value).longValue();
        }
        if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }
        return value;
    }

    private SatDecodeRuntimeConfiguration currentConfiguration() throws TransportException {
        String dataUrl = requiredString(getInitialisationMap().get(DATA_URL_KEY), DATA_URL_KEY);
        String dataType = requiredString(getInitialisationMap().get(DATA_TYPE_KEY), DATA_TYPE_KEY);
        String statusUrl = optionalString(getInitialisationMap().get(STATUS_URL_KEY));
        String statusParentPath = optionalString(getInitialisationMap().get(STATUS_PARENT_PATH_KEY));
        long dataThrottle = optionalLong(getInitialisationMap().get(DATA_THROTTLE_KEY), 0L);
        if (dataThrottle < 0) {
            throw new TransportException(DATA_THROTTLE_KEY + " cannot be negative");
        }
        SatDecodeDataType decodedDataType;
        try {
            decodedDataType = SatDecodeDataType.valueOf(dataType);
        } catch (IllegalArgumentException e) {
            throw new TransportException("Unsupported " + DATA_TYPE_KEY + ": " + dataType);
        }
        boolean fileBased = dataUrl.startsWith("file://");
        boolean zmqBased = dataUrl.startsWith("tcp://");
        if (!fileBased && !zmqBased) {
            throw new TransportException(DATA_URL_KEY + " must start with tcp:// or file://");
        }
        if (zmqBased && !statusUrl.isEmpty() && !statusUrl.startsWith("tcp://")) {
            throw new TransportException(STATUS_URL_KEY + " must start with tcp:// when specified");
        }
        return new SatDecodeRuntimeConfiguration(dataUrl, decodedDataType, statusUrl, statusParentPath, dataThrottle, fileBased);
    }

    @Override
    protected synchronized void doDisconnect() {
        running = false;
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        closeZmq();
        joinThread(dataThread);
        joinThread(statusThread);
        joinThread(monitorThread);
        dataThread = null;
        statusThread = null;
        monitorThread = null;
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOT_CHECKED);
    }

    @Override
    protected synchronized void doDispose() {
        running = false;
        closeZmq();
        joinThread(dataThread);
        joinThread(statusThread);
        joinThread(monitorThread);
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, String connectorInformation, boolean live) {
        this.driverName = driverName;
        this.spacecraftConfiguration = configuration;
        this.context = context;
        this.broker = context.getRawDataBroker();
        this.live = live;
        this.configurationDefaults.clear();
        this.configurationDefaults.putAll(parseConnectorInformation(connectorInformation));
    }

    private Map<String, Object> parseConnectorInformation(String connectorInformation) {
        Map<String, Object> result = new TreeMap<>();
        if (connectorInformation == null || connectorInformation.isBlank()) {
            return result;
        }
        String[] tokens = connectorInformation.split(";", -1);
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int idx = token.indexOf('=');
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid SatDecode connector property: " + token);
            }
            String key = token.substring(0, idx).trim();
            String value = token.substring(idx + 1).trim();
            if (DATA_THROTTLE_KEY.equals(key)) {
                result.put(key, Long.parseLong(value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private void closeZmq() {
        ZMQ.Socket localDataSocket = dataSocket;
        dataSocket = null;
        if (localDataSocket != null) {
            localDataSocket.close();
        }
        ZMQ.Socket localStatusSocket = statusSocket;
        statusSocket = null;
        if (localStatusSocket != null) {
            localStatusSocket.close();
        }
        ZMQ.Socket localMonitorSocket = monitorSocket;
        monitorSocket = null;
        if (localMonitorSocket != null) {
            localMonitorSocket.close();
        }
        ZContext localContext = zmqContext;
        zmqContext = null;
        if (localContext != null) {
            localContext.close();
        }
    }

    private void joinThread(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void throttle(long dataThrottle) throws InterruptedException {
        if (dataThrottle > 0) {
            Thread.sleep(Math.max(1, Math.round(1000.0 / dataThrottle)));
        }
    }

    private void validateFrameEnvelope(String topic, Map<String, Object> envelope) throws IOException {
        validateCommon(topic, envelope, MESSAGE_TYPE_TM_FRAME);
        toRequiredNumber(envelope, ENVELOPE_RX_TIME_NS_KEY);
        toRequiredNumber(envelope, ENVELOPE_PUBLISH_TIME_NS_KEY);
        toRequiredString(envelope, ENVELOPE_FRAME_TYPE_KEY);
        toRequiredNumber(envelope, ENVELOPE_FRAME_LENGTH_KEY);
        if (!(envelope.getOrDefault(ENVELOPE_METADATA_KEY, Collections.emptyMap()) instanceof Map<?, ?>)) {
            throw new IOException("metadata must be a map");
        }
    }

    private void validateMonitorEnvelope(String topic, Map<String, Object> envelope) throws IOException {
        String expectedType;
        switch (topic) {
            case STATUS_TOPIC:
                expectedType = MESSAGE_TYPE_MONITOR_STATUS;
                break;
            case EVENT_TOPIC:
                expectedType = MESSAGE_TYPE_MONITOR_EVENT;
                break;
            default:
                throw new IOException("Unsupported monitor topic: " + topic);
        }
        validateCommon(topic, envelope, expectedType);
        if (STATUS_TOPIC.equals(topic) && !(envelope.get(ENVELOPE_METRICS_KEY) instanceof Map<?, ?>)) {
            throw new IOException("monitor.status requires a metrics map");
        }
    }

    private void validateCommon(String topic, Map<String, Object> envelope, String expectedMessageType) throws IOException {
        if (!FRAME_TOPIC.equals(topic) && !STATUS_TOPIC.equals(topic) && !EVENT_TOPIC.equals(topic)) {
            throw new IOException("Unsupported SatDecode topic: " + topic);
        }
        if (toRequiredNumber(envelope, ENVELOPE_SCHEMA_VERSION_KEY).intValue() != SCHEMA_VERSION) {
            throw new IOException("Unsupported SatDecode schema_version");
        }
        if (!expectedMessageType.equals(toRequiredString(envelope, ENVELOPE_MESSAGE_TYPE_KEY))) {
            throw new IOException("SatDecode topic/message_type mismatch");
        }
        toRequiredString(envelope, ENVELOPE_RECEIVER_ID_KEY);
        toRequiredString(envelope, ENVELOPE_DECODER_KEY);
        if (toRequiredNumber(envelope, ENVELOPE_SEQUENCE_KEY).longValue() < 0) {
            throw new IOException("sequence must be non-negative");
        }
    }

    private ZFrame requireFrame(ZFrame frame, String label) throws IOException {
        if (frame == null) {
            throw new IOException("Missing " + label);
        }
        return frame;
    }

    private Map<String, Object> metadata(Map<String, Object> envelope) {
        Object metadata = envelope.get(ENVELOPE_METADATA_KEY);
        if (metadata instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) metadata;
            Map<String, Object> converted = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    String key = (String) entry.getKey();
                    converted.put(key, entry.getValue());
                }
            }
            return converted;
        }
        return Collections.emptyMap();
    }

    private Number numberFromMetadata(Map<String, Object> metadata, String key, Number fallback) {
        Object value = metadata.get(key);
        return value instanceof Number ? (Number) value : fallback;
    }

    private String firstString(Map<String, Object> metadata, String fallback, String... keys) {
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String) {
                String string = (String) value;
                if (!string.isBlank()) {
                    return string;
                }
            }
        }
        return fallback;
    }

    private String buildRoute(String source, Number vcid, String receiver, String decoder) {
        // SC ID and VC ID not needed: receiver + decoder is enough
        return receiver + "." + decoder;
    }

    private Quality mapQuality(Object quality) {
        if (quality instanceof String) {
            String q = (String) quality;
            try {
                return Quality.valueOf(q);
            } catch (IllegalArgumentException e) {
                return Quality.UNKNOWN;
            }
        }
        return Quality.UNKNOWN;
    }

    private Instant instantFromNs(Object value, Instant fallback) {
        if (value instanceof Number) {
            Number number = (Number) value;
            long ns = number.longValue();
            return Instant.ofEpochSecond(ns / 1_000_000_000L, ns % 1_000_000_000L);
        }
        return fallback;
    }

    private Number toRequiredNumber(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        throw new IOException(key + " must be a number");
    }

    private String toRequiredString(Map<String, Object> map, String key) throws IOException {
        Object value = map.get(key);
        if (value instanceof String) {
            String string = (String) value;
            if (!string.isEmpty()) {
                return string;
            }
        }
        throw new IOException(key + " must be a non-empty string");
    }

    private String requiredString(Object value, String key) throws TransportException {
        String stringValue = optionalString(value);
        if (stringValue.isEmpty()) {
            throw new TransportException(key + " must be specified");
        }
        return stringValue;
    }

    private String optionalString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private long optionalLong(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String string = (String) value;
            if (!string.isBlank()) {
                return Long.parseLong(string.trim());
            }
        }
        return fallback;
    }

    private String stringOrDefault(Object value, String fallback) {
        if (value instanceof String) {
            String string = (String) value;
            if (!string.isBlank()) {
                return string;
            }
        }
        return fallback;
    }

    private Path toFilePath(String configuredUrl) {
        try {
            return Path.of(URI.create(configuredUrl));
        } catch (IllegalArgumentException e) {
            String path = configuredUrl.substring("file://".length());
            return Path.of(path);
        }
    }

    private void readFully(PushbackInputStream inputStream, byte[] target, int offset, int length) throws IOException {
        int read = 0;
        while (read < length) {
            int justRead = inputStream.read(target, offset + read, length - read);
            if (justRead == -1) {
                throw new EOFException("Unexpected end of SatDecode file");
            }
            read += justRead;
        }
    }

    private enum SatDecodeDataType {
        TM_FRAME,
        AOS_FRAME,
        RAW
    }

    private static final class SatDecodeRuntimeConfiguration {
        private final String dataUrl;
        private final SatDecodeDataType dataType;
        private final String statusUrl;
        private final String statusParentPath;
        private final long dataThrottle;
        private final boolean fileBased;

        private SatDecodeRuntimeConfiguration(String dataUrl, SatDecodeDataType dataType, String statusUrl,
                                              String statusParentPath, long dataThrottle, boolean fileBased) {
            this.dataUrl = dataUrl;
            this.dataType = dataType;
            this.statusUrl = statusUrl;
            this.statusParentPath = statusParentPath;
            this.dataThrottle = dataThrottle;
            this.fileBased = fileBased;
        }

        private String dataUrl() {
            return dataUrl;
        }

        private SatDecodeDataType dataType() {
            return dataType;
        }

        private String statusUrl() {
            return statusUrl;
        }

        private String statusParentPath() {
            return statusParentPath;
        }

        private long dataThrottle() {
            return dataThrottle;
        }

        private boolean fileBased() {
            return fileBased;
        }

        boolean statusUsed() {
            return !fileBased && statusUrl != null && !statusUrl.isBlank();
        }
    }

    private static final class SatDecodeFrameMessage {
        private final String topic;
        private final Map<String, Object> envelope;
        private final byte[] payload;

        private SatDecodeFrameMessage(String topic, Map<String, Object> envelope, byte[] payload) {
            this.topic = topic;
            this.envelope = envelope;
            this.payload = payload;
        }

        private String topic() {
            return topic;
        }

        private Map<String, Object> envelope() {
            return envelope;
        }

        private byte[] payload() {
            return payload;
        }

    }

    private static final class SatDecodeMonitorMessage {
        private final String topic;
        private final Map<String, Object> envelope;

        private SatDecodeMonitorMessage(String topic, Map<String, Object> envelope) {
            this.topic = topic;
            this.envelope = envelope;
        }

        private String topic() {
            return topic;
        }

        private Map<String, Object> envelope() {
            return envelope;
        }

    }
}
