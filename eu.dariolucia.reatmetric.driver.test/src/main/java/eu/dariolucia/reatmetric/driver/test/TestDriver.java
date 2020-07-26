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
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
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
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 *      <li>4 bytes: command ID - integer that identifies the command - 0xFFFFFFFF identifies a parameter set, other values are control commands</li>
 *      <li>4 bytes: command unique tag - positive integer (counter tag)</li>
 *      <li>4 bytes: first argument or 0xFFFFFFFF - in case of parameter set, this contains the idx of the parameter</li>
 *      <li>4 bytes: second argument or 0xFFFFFFFF - in case of parameter set, this contains the value of the parameter (0/1 for booleans of int value or MSB for longs or double)</li>
 *      <li>4 bytes: third argument or 0xFFFFFFFF - in case of parameter set, this contains the value of the parameter (LSB for longs or doubles) </li>
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
    public static final String ACCEPTANCE_STAGE = "Acceptance";
    public static final String EXECUTION_START_STAGE = "Execution Start";
    public static final String EXECUTION_COMPLETED_STAGE = "Execution Completed";
    public static final String TRANSMISSION_STAGE = "Transmission";

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

    // Monitoring decoder
    private MonitoringDecoder decoder;

    // Command encoder
    private final CommandEncoder encoder;

    // Command verifier
    private CommandVerifier verifier;

    // Connector
    private volatile StationTransportConnector connector;

    public TestDriver() {
        //
        encoder = new CommandEncoder();
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
        this.connector.prepare(); // Now it is ready
        // Since this object is performing the event raising function, it registers to the broker to receive
        // notification of received events
        this.context.getRawDataBroker().subscribe(this::eventReceived, null, new RawDataFilter(true, null, null, Arrays.asList(STATION_EVENT), null, Collections.singletonList(Quality.GOOD)), null);
        // Decoder
        this.decoder = new MonitoringDecoder(context.getProcessingModel(), context.getRawDataBroker());
        // CommandVerifier
        this.verifier = new CommandVerifier(context.getProcessingModel(), context.getRawDataBroker());

        this.running = true;
        // Inform that everything is fine
        subscriber.driverStatusUpdate(this.name, SystemStatus.NOMINAL);
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
        // Clean up omitted... it should be done
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
        if(!connectorReady()) {
            throw new ActivityHandlingException("Activity invocation: type " + activityInvocation.getType() + " and route " + activityInvocation.getRoute() + " not available now");
        }
        executor.submit(() -> execute(activityInvocation, model));
    }

    @Override
    public boolean getRouteAvailability(String route) throws ActivityHandlingException {
        if(route.equals(STATION_ROUTE)) {
            return connector.getConnectionStatus() == TransportConnectionStatus.OPEN;
        } else {
            throw new ActivityHandlingException("Route " + route + " not supported");
        }
    }

    private boolean connectorReady() {
        return connector.isInitialised() && connector.isReady();
    }

    private void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        Pair<Integer, byte[]> encodedCommand = null;
        try {
            encodedCommand = encoder.encode(activityInvocation);
            storeRawData(activityInvocation.getActivityOccurrenceId(), activityInvocation.getPath(), activityInvocation.getGenerationTime(), activityInvocation.getRoute(), activityInvocation.getType(), activityInvocation.getSource(), encodedCommand.getSecond());
            synchronized (this) {
                // Record verification
                announce(activityInvocation, model, Instant.now(), TRANSMISSION_STAGE, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
                verifier.recordCommandVerification(encodedCommand.getFirst(), activityInvocation);
                // Transmission
                connector.send(encodedCommand.getSecond());
                announce(activityInvocation, model, Instant.now(), TRANSMISSION_STAGE, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
            }
        } catch(Exception e) {
            LOG.log(Level.SEVERE, "Transmission of activity " + activityInvocation.getActivityOccurrenceId() + " failed: " + e.getMessage(), e);
            announce(activityInvocation, model, Instant.now(), TRANSMISSION_STAGE, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            verifier.removeCommandVerification(encodedCommand.getFirst());
        }
    }

    private void storeRawData(IUniqueId activityOccurrenceId, SystemEntityPath path, Instant generationTime, String route, String type, String source, byte[] encodedCommand) throws ReatmetricException {
        IUniqueId internalId = context.getRawDataBroker().nextRawDataId();
        RawData rd = new RawData(internalId, generationTime, path.getLastPathElement(), type, route, source, Quality.GOOD, activityOccurrenceId, encodedCommand, Instant.now(), this.name,null);
        context.getRawDataBroker().distribute(Collections.singletonList(rd), true);
    }

    protected static void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, Instant genTime, String name, ActivityReportState reportState, ActivityOccurrenceState occState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, genTime, occState, null, reportState, occState, null));
    }

    protected static void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, Instant genTime, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextOccState, Instant executionTime, Object result) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, genTime, occState, executionTime, reportState, nextOccState, result));
    }

    /* ************************************************************************* *
     * {@link IRawDataRenderer} implementation
     * ************************************************************************* */

    @Override
    public String getHandler() {
        return this.name;
    }

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList(STATION_CMD, STATION_ACK, STATION_TM, STATION_EVENT);
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) {
        return decoder.render(rawData);
    }

    /* ************************************************************************* *
     * {@link IDebugInfoProvider} implementation
     * ************************************************************************* */

    @Override
    public List<DebugInformation> currentDebugInfo() {
        // TODO: implement if needed
        return Collections.emptyList();
    }
}
