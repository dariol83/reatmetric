/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.value.BitString;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.processing.definition.EventProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

class TelemetryTransportConnectorImpl implements ITransportConnector {

    private static final Logger LOG = Logger.getLogger(TelemetryTransportConnectorImpl.class.getName());

    private final String name;
    private final String[] routes;
    private final List<ITransportSubscriber> subscriber = new CopyOnWriteArrayList<>();
    private final ProcessingDefinition definitions;
    private final IProcessingModel model;
    private final IRawDataBroker broker;
    private final ExecutorService notifier = Executors.newSingleThreadExecutor();
    private volatile boolean connected;
    private volatile boolean initialised;
    private volatile String message;
    private volatile Thread generator;
    private final Map<Integer, AtomicLong> intParamValueMap = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();

    public TelemetryTransportConnectorImpl(String name, String[] routes, ProcessingDefinition definitions, IProcessingModel model, IRawDataBroker broker) {
        this.name = name;
        this.routes = routes;
        this.definitions = definitions;
        this.model = model;
        this.broker = broker;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Connector " + name + ": " + Arrays.toString(routes);
    }

    @Override
    public TransportConnectionStatus getConnectionStatus() {
        return deriveStatus();
    }

    @Override
    public boolean isInitialised() {
        return initialised;
    }

    @Override
    public void initialise(Map<String, Object> properties) {
        this.initialised = true;
        this.message = "Initialised";
        this.properties.clear();
        this.properties.putAll(properties);
        notifyState();
    }

    @Override
    public void connect() {
        connected = true;
        message = "Connected";
        notifyState();
        startTmGeneration();
    }

    private void storeRawData(String name, Instant generationTime, String route, String type) throws ReatmetricException {
        IUniqueId internalId = broker.nextRawDataId();
        RawData rd = new RawData(internalId, generationTime, name, type, route, "", Quality.GOOD, null, new byte[45], Instant.now(), null);
        broker.distribute(Collections.singletonList(rd), true);
    }

    private void startTmGeneration() {
        this.generator = new Thread(() -> {
            int paramCurrentIdx = 0;
            int eventCurrentIdx = 0;
            while(connected && this.generator == Thread.currentThread()) {
                try {
                    Thread.sleep(1234);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!connected) {
                    return;
                }
                List<ParameterSample> samples = new LinkedList<>();
                for(int i = 0; i < 80; ++i) {
                    ParameterSample ps = generateTmSample(paramCurrentIdx);
                    if(ps != null) {
                        samples.add(ps);
                    }
                    if(++paramCurrentIdx >= definitions.getParameterDefinitions().size()) {
                        paramCurrentIdx = 0;
                        message = "Wrapping around parameters";
                        notifyState();
                    }
                }
                if(!connected) {
                    return;
                }
                if(model != null) {
                    try {
                        storeRawData("TM00" + (System.currentTimeMillis() % 15), Instant.now(), routes[0], "TM Packet");
                    } catch (ReatmetricException e) {
                        LOG.log(Level.FINE, "Cannot store TM raw data", e);
                    }
                    model.injectParameters(samples);
                }
                if(!connected) {
                    return;
                }
                for(int i = 0; i < 3; ++i) {
                    EventOccurrence event = generateEvent(eventCurrentIdx);
                    if(++eventCurrentIdx >= definitions.getEventDefinitions().size()) {
                        eventCurrentIdx = 0;
                        message = "Wrapping around events";
                        notifyState();
                    }
                    if(!connected) {
                        return;
                    }
                    if(model != null && event != null) {
                        try {
                            storeRawData("EVT00" + (System.currentTimeMillis() % 9), Instant.now(), routes[0], "TM Packet");
                        } catch (ReatmetricException e) {
                            LOG.log(Level.FINE, "Cannot store TM raw data", e);
                        }
                        model.raiseEvent(event);
                    }
                }
                if(System.currentTimeMillis() % 100 == 0) {
                    LOG.info("TM log message to be reported");
                }
            }
        });
        this.generator.start();
    }

    private EventOccurrence generateEvent(int eventCurrentIdx) {
        EventProcessingDefinition eventDef = definitions.getEventDefinitions().get(eventCurrentIdx);
        if(eventDef.getCondition() != null) {
            return null;
        }
        return EventOccurrence.of(eventDef.getId(), Instant.now(), Instant.now(), null, "Qual1", eventDef.hashCode(), routes[0], "SC1");
    }

    private ParameterSample generateTmSample(int currentIdx) {
        ParameterProcessingDefinition paramDef = definitions.getParameterDefinitions().get(currentIdx);
        if(paramDef.getExpression() != null) {
            return null;
        }
        return ParameterSample.of(paramDef.getId(), Instant.now(), Instant.now(), null, deriveValue(paramDef), routes[0], null);
    }

    private Object deriveValue(ParameterProcessingDefinition paramDef) {
        switch(paramDef.getRawType()) {
            case REAL: return Math.sin(paramDef.hashCode() / (double) (Instant.now().toEpochMilli() % 10000));
            case SIGNED_INTEGER: return generateSignedIntParameter(paramDef.getId());
            case UNSIGNED_INTEGER: return (long) Math.abs(generateSignedIntParameter(paramDef.getId()));
            case BOOLEAN: return Instant.now().toEpochMilli() % 2 == 0;
            case ENUMERATED: return generateEnumParameter(paramDef.getId());
            case ABSOLUTE_TIME: return Instant.now().plusMillis(paramDef.hashCode());
            case RELATIVE_TIME: return Duration.ofMillis((Instant.now().toEpochMilli() % 10000));
            case CHARACTER_STRING: return "TEST" + (Instant.now().toEpochMilli() % 10);
            case OCTET_STRING: return ("BYTE" + Instant.now().toString()).getBytes();
            case BIT_STRING: return new BitString(new byte[] {0x00, (byte) 0xFF, (byte) 0xA3}, 23);
            default: return null;
        }
    }

    private int generateEnumParameter(int id) {
        AtomicLong val = intParamValueMap.computeIfAbsent(id, (a) -> new AtomicLong(0));
        if(val.get() > 10) {
            val.set(0);
        }
        return (int) val.getAndIncrement();
    }

    private long generateSignedIntParameter(int id) {
        AtomicLong val = intParamValueMap.computeIfAbsent(id, (a) -> new AtomicLong(0));
        if(val.get() > 100) {
            val.set(-10);
        }
        return val.getAndIncrement();
    }

    @Override
    public void disconnect() {
        connected = false;
        message = "Disconnected";
        notifyState();
    }

    @Override
    public void abort() {
        connected = false;
        message = "Aborted";
        notifyState();
    }

    @Override
    public void dispose() {
        connected = false;
        initialised = false;
        message = "Disposed";
        notifyState();
    }

    private void notifyState() {
        for(ITransportSubscriber s : subscriber) {
            notifyState(s);
        }
    }

    @Override
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() {
        Map<String, Pair<String, ValueTypeEnum>> toReturn = new LinkedHashMap<>();
        toReturn.put("key1", Pair.of("Boolean parameter", ValueTypeEnum.BOOLEAN));
        toReturn.put("key2", Pair.of("Integer parameter", ValueTypeEnum.SIGNED_INTEGER));
        toReturn.put("key3", Pair.of("Real parameter", ValueTypeEnum.REAL));
        toReturn.put("key4", Pair.of("String parameter", ValueTypeEnum.OCTET_STRING));
        return toReturn;
    }

    @Override
    public Map<String, Object> getCurrentProperties() {
        return Collections.unmodifiableMap(this.properties);
    }

    @Override
    public void register(ITransportSubscriber listener) {
        if(!subscriber.contains(listener)) {
            subscriber.add(listener);
            notifyState(listener);
        }
    }

    private void notifyState(ITransportSubscriber listener) {
        TransportStatus status = buildTransportStatus();
        notifier.execute(() -> listener.status(this, status));
    }

    private TransportStatus buildTransportStatus() {
        return new TransportStatus(name, message, deriveStatus(), 0, 0, AlarmState.NOMINAL);
    }

    private TransportConnectionStatus deriveStatus() {
        if(connected) {
            return TransportConnectionStatus.OPEN;
        } else {
            if(initialised) {
                return TransportConnectionStatus.IDLE;
            } else {
                return TransportConnectionStatus.NOT_INIT;
            }
        }
    }

    @Override
    public void deregister(ITransportSubscriber listener) {
        subscriber.remove(listener);
    }
}
