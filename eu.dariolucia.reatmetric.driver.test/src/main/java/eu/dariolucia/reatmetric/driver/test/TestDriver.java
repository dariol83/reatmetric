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

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This test driver is implemented to show the main functionalities and characteristics that
 * a ReatMetric driver should have.
 *
 * The driver implements the generation of simulated data according to the model defined in the
 * XML file (part of the module resources).
 *
 * Monitoring data is delivered by means of byte arrays with the following format:
 * <ul>
 *     <li>1 byte - 4 MSB: equipment ID</li>
 *     <li>1 byte - 4 LSB: 0: parameters - 1: event - 2: command ack - 3: command start - 4: command completed</li>
 *     <li>8 bytes: timestamp - milliseconds from Java epoch</li>
 *     <li>the rest: definition of the equipment specific monitoring format (depends on type and number of parameters)</li>
 * </ul>
 *
 * A command is a byte array with the following format:
 * <ul>
 *      <li>1 byte - 4 MSB: equipment ID</li>
 *      <li>1 byte - 4 LSB: 15 (0xF)</li>
 *      <li>4 bytes: command ID - integer that identifies the command</li>
 *      <li>4 bytes: command unique tag - positive integer (counter tag)</li>
 *      <li>4 bytes: first argument or 0xFFFFFFFF</li>
 *      <li>4 bytes: second argument or 0xFFFFFFFF</li>
 *      <li>4 bytes: third argument or 0xFFFFFFFF</li>
 * </ul>
 *
 * The raw data is produced by a simulated model, and provided to the connector, which distributes the data inside the
 * ReatMetric system.
 */
public class TestDriver implements IDriver, IActivityHandler, IRawDataRenderer {

    private static final Logger LOG = Logger.getLogger(TestDriver.class.getName());

    public static final String STATION_CMD = "STATION CMD";
    public static final String STATION_ROUTE = "STATION ROUTE";
    public static final String STATION_SOURCE = "STATION";
    public static final String STATION_ACK = "STATION ACK";
    public static final String STATION_TM = "STATION TM";
    public static final String STATION_EVENT = "STATION EVENT";
    public static final String EQUIPMENT_ID_ARGKEY = "EQUIPMENT_ID";
    public static final String COMMAND_ID_ARGKEY = "COMMAND_ID";
    public static final String ARG_1_ARGKEY = "ARG1";
    public static final String ARG_2_ARGKEY = "ARG2";
    public static final String ARG_3_ARGKEY = "ARG3";

    private volatile String name;
    private volatile boolean running;
    private volatile IServiceCoreContext context;
    private volatile IProcessingModel model;
    private volatile ProcessingDefinition definitions;
    private volatile IDriverListener subscriber;

    // For activity execution
    private final ExecutorService executor = Executors.newFixedThreadPool(1, (t) -> {
        Thread toReturn = new Thread(t, "TestDriver Activity Handler Thread");
        toReturn.setDaemon(true);
        return toReturn;
    });
    private final Map<Integer, ActivityInvocation> commandTag2activityInvocation = new HashMap<>();
    private final AtomicInteger commandTagSequencer = new AtomicInteger(0);

    private volatile StationTransportConnector connector;

    public TestDriver() {
        // Nothing to do
    }

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.subscriber = subscriber;
        try {
            this.definitions = ProcessingDefinition.loadAll(coreConfiguration.getDefinitionsLocation());
        } catch (ReatmetricException e) {
            throw new DriverException(e);
        }
        this.connector = new StationTransportConnector(name, "Station Connector", "Test connector to simulate data", context.getRawDataBroker());
        // Since this object is also performing the event raising function, it registers to the broker to receive
        // notification of received events
        this.context.getRawDataBroker().subscribe(this::eventReceived, null, new RawDataFilter(true, null, null, Arrays.asList(STATION_EVENT), null, Collections.singletonList(Quality.GOOD)), null);
        // Since this object is also performing the command verification function, it registers to the broker to receive
        // notification of received command acks
        this.context.getRawDataBroker().subscribe(this::commandAckReceived, null, new RawDataFilter(true, null, null, Arrays.asList(STATION_ACK), null, Collections.singletonList(Quality.GOOD)), null);
        this.running = true;
    }

    private void commandAckReceived(List<RawData> rawData) {
        for(RawData rd : rawData) {
            processCommandAck(rd);
        }
    }

    private synchronized void processCommandAck(RawData rd) {
        // TODO
    }

    private void eventReceived(List<RawData> rawData) {
        // TODO
    }

    @Override
    public SystemStatus getDriverStatus() {
        return SystemStatus.NOMINAL;
    }

    /**
     * Renderers are used to visualise raw data in human readable format. For a simple driver, the render can be the
     * driver itself.
     *
     * @return the supported raw data renderers
     */
    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.singletonList(this);
    }

    /**
     * For a simple driver, an activity handler can be the driver itself.
     *
     * @return the activity handler
     */
    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this);
    }

    /**
     * This driver defines a single combined connector for TM and commands.
     *
     * @return the transport connectors
     */
    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.singletonList(this.connector);
    }

    @Override
    public void dispose() {
        running = false;
        try {
            connector.abort();
        } catch (TransportException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerModel(IProcessingModel model) {
        // Register it for activity purposes
        this.model = model;
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        // Deregister it for activity purposes
        this.model = null;
    }

    @Override
    public List<String> getSupportedRoutes() {
        return Collections.singletonList(TestDriver.STATION_ROUTE);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(TestDriver.STATION_CMD);
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        LOG.info("Activity invocation: " + activityInvocation);
        if(!activityInvocation.getType().equals(TestDriver.STATION_CMD)) {
            throw new ActivityHandlingException("Type " + activityInvocation.getType() + " not supported");
        }
        if(activityInvocation.getArguments() == null) {
            throw new ActivityHandlingException("Activity invocation has null argument map");
        }
        if(!connectorReady(activityInvocation.getType(), activityInvocation.getRoute())) {
            throw new ActivityHandlingException("Activity invocation: type " + activityInvocation.getType() + " and route " + activityInvocation.getRoute() + " not available now");
        }
        executor.submit(() -> execute(activityInvocation, model));
    }

    private boolean connectorReady(String type, String route) {
        return connector.isInitialised() && connector.isReady();
    }

    private void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        byte[] encodedCommand = null;
        try {
            encodedCommand = encode(activityInvocation);
            storeRawData(activityInvocation.getActivityOccurrenceId(), activityInvocation.getPath(), activityInvocation.getGenerationTime(), activityInvocation.getRoute(), activityInvocation.getType(), activityInvocation.getSource(), encodedCommand);
            recordCommandVerification(encodedCommand, activityInvocation);
            announce(activityInvocation, model, "Final Release", ActivityReportState.OK, ActivityOccurrenceState.RELEASE, ActivityOccurrenceState.TRANSMISSION);
            synchronized (this) {
                // Transmission
                announce(activityInvocation, model, "Transmission", ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                connector.send(encodedCommand);
                announce(activityInvocation, model, "Transmission", ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
            }
        } catch(Exception e) {
            LOG.log(Level.SEVERE, "Transmission of activity " + activityInvocation.getActivityOccurrenceId() + " failed: " + e.getMessage(), e);
            announce(activityInvocation, model, "Transmission", ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            removeCommandVerification(encodedCommand);
        }
    }

    private byte[] encode(ActivityInvocation activityInvocation) {
        // Read the activityInvocation arguments and encode accordingly
        int eqId = (Integer) activityInvocation.getArguments().get(EQUIPMENT_ID_ARGKEY);
        int commandId = (Integer) activityInvocation.getArguments().get(COMMAND_ID_ARGKEY);
        Number arg1 = (Number) activityInvocation.getArguments().get(ARG_1_ARGKEY);
        if(arg1 == null) {
            arg1 = -1;
        }
        Number arg2 = (Number) activityInvocation.getArguments().get(ARG_2_ARGKEY);
        if(arg2 == null) {
            arg2 = -1;
        }
        Number arg3 = (Number) activityInvocation.getArguments().get(ARG_3_ARGKEY);
        if(arg3 == null) {
            arg3 = -1;
        }
        byte firstByte = (byte) eqId;
        firstByte <<= 4;
        firstByte |= 0x0F;
        ByteBuffer cmdBuffer = ByteBuffer.allocate(21);
        cmdBuffer.put(firstByte);
        cmdBuffer.putInt(commandId);
        cmdBuffer.putInt(commandTagSequencer.incrementAndGet());
        cmdBuffer.putInt(arg1.intValue());
        cmdBuffer.putInt(arg2.intValue());
        cmdBuffer.putInt(arg3.intValue());
        return cmdBuffer.array();
    }

    private void removeCommandVerification(byte[] encodedCommand) {
        int cmdTag = extractCommandTag(encodedCommand);
        this.commandTag2activityInvocation.remove(cmdTag);
    }

    private int extractCommandTag(byte[] encodedCommand) {
        ByteBuffer bb = ByteBuffer.wrap(encodedCommand);
        bb.get();
        return bb.getInt();
    }

    private synchronized void recordCommandVerification(byte[] encodedCommand, ActivityInvocation activityInvocation) {
        int cmdTag = extractCommandTag(encodedCommand);
        this.commandTag2activityInvocation.put(cmdTag, activityInvocation);
    }

    private void storeRawData(IUniqueId activityOccurrenceId, SystemEntityPath path, Instant generationTime, String route, String type, String source, byte[] encodedCommand) throws ReatmetricException {
        IUniqueId internalId = context.getRawDataBroker().nextRawDataId();
        RawData rd = new RawData(internalId, generationTime, path.getLastPathElement(), type, route, source, Quality.GOOD, activityOccurrenceId, encodedCommand, Instant.now(), this.name,null);
        context.getRawDataBroker().distribute(Collections.singletonList(rd), true);
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextOccState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, null, reportState, nextOccState, null));
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, null, reportState, occState, null));
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextOccState, Instant executionTime, Object result) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, executionTime, reportState, nextOccState, result));
    }

    @Override
    public String getHandler() {
        return this.name;
    }

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList(STATION_CMD, STATION_ACK, STATION_TM, STATION_EVENT);
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException {
        // TODO
        return new LinkedHashMap<>();
    }
}
