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

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.common.IReceptionOnlyConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reception-only client for JSatDecode TCP Binary Distribution Protocol (JTDP), version 1.
 * A single standard Java TCP socket carries frames, monitoring snapshots and events.
 */
public class JtdpConnector extends AbstractTransportConnector implements IReceptionOnlyConnector {

    private static final Logger LOG = Logger.getLogger(JtdpConnector.class.getName());

    public static final String DATA_URL_KEY = SatDecodeConnector.DATA_URL_KEY;
    public static final String DATA_TYPE_KEY = SatDecodeConnector.DATA_TYPE_KEY;
    public static final String STATUS_PARENT_PATH_KEY = SatDecodeConnector.STATUS_PARENT_PATH_KEY;
    public static final String ANTENNA_PREFIX = "antenna.";

    private static final List<String> EVENT_NAMES = List.of("DECODER_STARTED", "DECODER_STOPPED",
            "CARRIER_STATE_CHANGED", "SYMBOL_STATE_CHANGED", "FRAME_STATE_CHANGED", "DECODER_ERROR",
            "DISTRIBUTION_OVERRUN", "DATA_DISCARDED");

    private final Map<String, Object> configurationDefaults = new TreeMap<>();
    private final Map<String, AbstractSystemEntityDescriptor> descriptorCache = new ConcurrentHashMap<>();
    private final Set<String> missingEntities = ConcurrentHashMap.newKeySet();
    private final Set<Integer> unmappedAntennas = ConcurrentHashMap.newKeySet();
    private final AtomicLong rxBytes = new AtomicLong();
    private volatile Instant lastSamplingTime;
    private volatile IServiceCoreContext context;
    private volatile ReceptionFrameMapper frameMapper;
    private volatile Session session;

    public JtdpConnector() {
        super("JTDP Connector", "JSatDecode TCP telemetry connector");
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context,
                          IServiceBroker serviceBroker, String connectorInformation, boolean live) {
        this.context = context;
        this.frameMapper = new ReceptionFrameMapper(configuration, context.getRawDataBroker(), driverName, live);
        configurationDefaults.clear();
        if (connectorInformation != null && !connectorInformation.isBlank()) {
            for (String property : connectorInformation.split(";", -1)) {
                if (property.isBlank()) {
                    continue;
                }
                int separator = property.indexOf('=');
                if (separator <= 0) {
                    throw new IllegalArgumentException("Invalid JTDP connector property: " + property);
                }
                String key = property.substring(0, separator).trim();
                validatePropertyKey(key);
                configurationDefaults.put(key, property.substring(separator + 1).trim());
            }
        }
        descriptorCache.clear();
        missingEntities.clear();
        unmappedAntennas.clear();
    }

    @Override
    protected void addToInitialisationMap(Map<String, Object> values, Map<String, Pair<String, ValueTypeEnum>> descriptions) {
        descriptions.put(DATA_URL_KEY, Pair.of("JTDP server URL: tcp://<host>:<port>", ValueTypeEnum.CHARACTER_STRING));
        descriptions.put(DATA_TYPE_KEY, Pair.of("JTDP data type: TM_FRAME, AOS_FRAME or RAW", ValueTypeEnum.CHARACTER_STRING));
        descriptions.put(STATUS_PARENT_PATH_KEY, Pair.of("Processing model parent path containing mapped JTDP antenna names; blank disables status/events", ValueTypeEnum.CHARACTER_STRING));
        values.put(DATA_URL_KEY, configurationDefaults.getOrDefault(DATA_URL_KEY, ""));
        values.put(DATA_TYPE_KEY, configurationDefaults.getOrDefault(DATA_TYPE_KEY, ReceptionFrameMapper.DataType.TM_FRAME.name()));
        values.put(STATUS_PARENT_PATH_KEY, configurationDefaults.getOrDefault(STATUS_PARENT_PATH_KEY, ""));
        configurationDefaults.forEach((key, value) -> {
            if (key.startsWith(ANTENNA_PREFIX)) {
                descriptions.put(key, Pair.of("Antenna name used for publication route and model paths for JTDP antenna/decoder " + key.substring(ANTENNA_PREFIX.length()), ValueTypeEnum.CHARACTER_STRING));
                values.put(key, value);
            }
        });
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        Instant now = Instant.now();
        Instant previous = lastSamplingTime;
        lastSamplingTime = now;
        long bytes = rxBytes.getAndSet(0);
        if (previous == null) {
            return null;
        }
        long elapsedMillis = now.toEpochMilli() - previous.toEpochMilli();
        return Pair.of(0L, elapsedMillis <= 0 ? 0L : Math.round(bytes * 8000.0 / elapsedMillis));
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        if (session != null) {
            throw new TransportException("JTDP connector already connected");
        }
        RuntimeConfiguration configuration = currentConfiguration();
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        updateAlarmState(AlarmState.NOT_CHECKED);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(configuration.address().getHost(), configuration.address().getPort()), 5000);
            socket.setKeepAlive(true);
            Session connected = new Session(socket, configuration);
            connected.reader = new Thread(() -> readMessages(connected), getName() + " reader");
            connected.reader.setDaemon(true);
            session = connected;
            rxBytes.set(0);
            lastSamplingTime = Instant.now();
            updateConnectionStatus(TransportConnectionStatus.OPEN);
            updateAlarmState(AlarmState.NOMINAL);
            connected.reader.start();
        } catch (IOException | RuntimeException e) {
            closeSocket(socket);
            session = null;
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            updateAlarmState(AlarmState.ERROR);
            throw new TransportException(e);
        }
    }

    private void readMessages(Session connected) {
        try (InputStream input = new BufferedInputStream(connected.socket.getInputStream())) {
            while (session == connected) {
                JtdpMessageReader.Message message = JtdpMessageReader.read(input);
                if (message == null) {
                    throw new EOFException("JTDP server closed the connection");
                }
                if (session != connected) {
                    break;
                }
                try {
                    processMessage(connected.configuration, message, Instant.now());
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, getName() + ": cannot process JTDP message: " + e.getMessage(), e);
                    updateAlarmState(AlarmState.WARNING);
                }
            }
        } catch (IOException | RuntimeException e) {
            synchronized (this) {
                if (session == connected) {
                    session = null;
                    LOG.log(Level.SEVERE, getName() + ": JTDP connection failed; data continuity lost: " + e.getMessage(), e);
                    updateAlarmState(AlarmState.ERROR);
                    updateConnectionStatus(TransportConnectionStatus.ERROR);
                }
            }
        } finally {
            closeSocket(connected.socket);
        }
    }

    private void processMessage(RuntimeConfiguration configuration, JtdpMessageReader.Message message, Instant localReceptionTime) {
        if (message instanceof JtdpMessageReader.Unknown) {
            return;
        }
        if (message instanceof JtdpMessageReader.Event && ((JtdpMessageReader.Event) message).code() >= EVENT_NAMES.size()) {
            return;
        }
        String antennaName = configuration.antennas().get(message.antennaId());
        if (antennaName == null) {
            antennaName = Integer.toString(message.antennaId());
            if (unmappedAntennas.add(message.antennaId())) {
                LOG.warning(getName() + ": no antenna mapping for " + message.antennaId() + "; using its decimal ID as publication route and model path component");
            }
        }
        if (message instanceof JtdpMessageReader.FrameBatch) {
            for (JtdpMessageReader.Frame frame : ((JtdpMessageReader.FrameBatch) message).frames()) {
                try {
                    RawData rawData = frameMapper.map(configuration.dataType(), frame.data(), frame.receptionTime(),
                            frame.quality(), antennaName, Collections.emptyMap(), Constants.N_UNKNOWN_FRAME);
                    context.getRawDataBroker().distribute(Collections.singletonList(rawData));
                    rxBytes.addAndGet(frame.data().length);
                } catch (ReatmetricException | RuntimeException e) {
                    LOG.log(Level.WARNING, getName() + ": cannot publish JTDP frame: " + e.getMessage(), e);
                    updateAlarmState(AlarmState.WARNING);
                }
            }
        } else if (message instanceof JtdpMessageReader.Monitoring) {
            JtdpMessageReader.Monitoring monitoring = (JtdpMessageReader.Monitoring) message;
            Map<String, Object> values = statusValues(monitoring.status(), monitoring.frequency());
            values.put("CARRIER_OFFSET", monitoring.offset());
            values.put("SNR", monitoring.snr());
            values.put("TOTAL_FRAMES", monitoring.totalFrames());
            values.put("ERROR_FRAMES", monitoring.errorFrames());
            values.put("GOOD_FRAMES", monitoring.goodFrames());
            values.put("DISTRIBUTED_FRAMES", monitoring.distributedFrames());
            injectStatus(configuration, values, localReceptionTime, localReceptionTime, antennaName);
            updateFrameLock(monitoring.status());
        } else if (message instanceof JtdpMessageReader.Event) {
            JtdpMessageReader.Event event = (JtdpMessageReader.Event) message;
            Map<String, Object> values = statusValues(event.status(), event.frequency());
            injectStatus(configuration, values, event.time(), localReceptionTime, antennaName);
            updateFrameLock(event.status());
            String path = entityPath(configuration, antennaName, EVENT_NAMES.get(event.code()));
            AbstractSystemEntityDescriptor descriptor = resolveDescriptor(path, EventDescriptor.class);
            if (descriptor != null) {
                Map<String, Object> report = new LinkedHashMap<>(values);
                report.put("STATUS", (long) event.status());
                context.getProcessingModel().raiseEvent(EventOccurrence.of(descriptor.getExternalId(), event.time(),
                        localReceptionTime, null, null, Collections.unmodifiableMap(report), antennaName,
                        Integer.toString(event.antennaId())));
            }
        }
    }

    private Map<String, Object> statusValues(int status, long frequency) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("CARRIER_LOCK", (long) ((status >>> 6) & 3));
        values.put("SYMBOL_LOCK", (long) ((status >>> 4) & 3));
        values.put("FRAME_LOCK", (long) ((status >>> 2) & 3));
        values.put("SNR_AVAILABLE", (status & 3) == 1);
        values.put("CENTER_FREQUENCY", frequency);
        return values;
    }

    private void updateFrameLock(int status) {
        updateAlarmState(((status >>> 2) & 3) == 1 ? AlarmState.NOMINAL : AlarmState.WARNING);
    }

    private void injectStatus(RuntimeConfiguration configuration, Map<String, Object> values,
                              Instant generationTime, Instant receptionTime, String antennaName) {
        if (configuration.statusParentPath().isEmpty()) {
            return;
        }
        List<ParameterSample> samples = new ArrayList<>();
        values.forEach((name, value) -> {
            ParameterDescriptor descriptor = (ParameterDescriptor) resolveDescriptor(entityPath(configuration, antennaName, name), ParameterDescriptor.class);
            if (descriptor != null) {
                try {
                    samples.add(ParameterSample.of(descriptor.getExternalId(), generationTime, receptionTime, null,
                            ValueUtil.convert(value, descriptor.getRawDataType()), antennaName, null));
                } catch (ValueException e) {
                    LOG.log(Level.WARNING, getName() + ": cannot convert JTDP parameter " + descriptor.getPath(), e);
                }
            }
        });
        if (!samples.isEmpty()) {
            context.getProcessingModel().injectParameters(samples);
        }
    }

    private String entityPath(RuntimeConfiguration configuration, String antennaName, String name) {
        return configuration.statusParentPath().isEmpty() ? null : configuration.statusParentPath() + "." + antennaName + "." + name;
    }

    private AbstractSystemEntityDescriptor resolveDescriptor(String path, Class<? extends AbstractSystemEntityDescriptor> type) {
        if (path == null || missingEntities.contains(path)) {
            return null;
        }
        AbstractSystemEntityDescriptor descriptor = descriptorCache.get(path);
        if (descriptor != null) {
            return type.isInstance(descriptor) ? descriptor : null;
        }
        try {
            descriptor = context.getProcessingModel().getDescriptorOf(SystemEntityPath.fromString(path));
            if (type.isInstance(descriptor)) {
                descriptorCache.put(path, descriptor);
                return descriptor;
            }
        } catch (ReatmetricException | IllegalArgumentException e) {
            // Missing model entities are reported once and do not stop frame reception.
        }
        if (missingEntities.add(path)) {
            LOG.warning(getName() + ": JTDP processing model entity " + path + " missing or of the wrong type; skipping updates");
        }
        return null;
    }

    private RuntimeConfiguration currentConfiguration() throws TransportException {
        Map<String, Object> values = getInitialisationMap();
        try {
            URI address = URI.create(stringValue(values.get(DATA_URL_KEY)));
            if (!"tcp".equals(address.getScheme()) || address.getHost() == null || address.getPort() < 1 || address.getPort() > 65535 ||
                    address.getRawUserInfo() != null || address.getRawQuery() != null || address.getRawFragment() != null ||
                    (address.getRawPath() != null && !address.getRawPath().isEmpty())) {
                throw new IllegalArgumentException(DATA_URL_KEY + " must be tcp://<host>:<port>");
            }
            ReceptionFrameMapper.DataType dataType = ReceptionFrameMapper.DataType.valueOf(stringValue(values.get(DATA_TYPE_KEY)));
            String parent = stringValue(values.get(STATUS_PARENT_PATH_KEY));
            if (!parent.isEmpty() && Arrays.stream(parent.split("\\.", -1)).anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Invalid " + STATUS_PARENT_PATH_KEY + ": " + parent);
            }
            Map<Integer, String> antennas = new HashMap<>();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                validatePropertyKey(entry.getKey());
                if (entry.getKey().startsWith(ANTENNA_PREFIX)) {
                    String antennaName = stringValue(entry.getValue());
                    if (antennaName.isEmpty()) {
                        throw new IllegalArgumentException("Empty JTDP antenna mapping: " + entry.getKey());
                    }
                    antennas.put(Integer.parseInt(entry.getKey().substring(ANTENNA_PREFIX.length())), antennaName);
                }
            }
            return new RuntimeConfiguration(address, dataType, parent, Map.copyOf(antennas));
        } catch (IllegalArgumentException e) {
            throw new TransportException("Invalid JTDP configuration: " + e.getMessage(), e);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static void validatePropertyKey(String key) {
        if (DATA_URL_KEY.equals(key) || DATA_TYPE_KEY.equals(key) || STATUS_PARENT_PATH_KEY.equals(key)) {
            return;
        }
        if (key.startsWith(ANTENNA_PREFIX)) {
            String id = key.substring(ANTENNA_PREFIX.length());
            if (id.matches("0|[1-9][0-9]{0,4}") && Integer.parseInt(id) <= 65535) {
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported JTDP connector property: " + key);
    }

    @Override
    protected void doDisconnect() {
        Session previous;
        synchronized (this) {
            updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
            previous = session;
            session = null;
            if (previous != null) {
                closeSocket(previous.socket);
            }
        }
        if (previous != null && previous.reader != Thread.currentThread()) {
            try {
                previous.reader.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOT_CHECKED);
    }

    @Override
    protected void doDispose() {
        doDisconnect();
    }

    @Override
    public void abort() throws TransportException {
        disconnect();
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Cannot close JTDP socket", e);
        }
    }

    private record RuntimeConfiguration(URI address, ReceptionFrameMapper.DataType dataType,
                                        String statusParentPath, Map<Integer, String> antennas) {
    }

    private static final class Session {
        private final Socket socket;
        private final RuntimeConfiguration configuration;
        private Thread reader;

        private Session(Socket socket, RuntimeConfiguration configuration) {
            this.socket = socket;
            this.configuration = configuration;
        }
    }
}
