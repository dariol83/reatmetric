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
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.*;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.time.Instant;
import java.util.*;
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
    private final List<ITransportConnector> connectors = new LinkedList<>();
    private final List<String> types = Arrays.asList(STATION_CMD);
    private final List<String> routes = Arrays.asList(STATION_ROUTE);

    public TestDriver() {
        // Nothing to do
    }

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.context = context;
        this.subscriber = subscriber;
        try {
            this.definitions = ProcessingDefinition.loadAll(coreConfiguration.getDefinitionsLocation());
        } catch (ReatmetricException e) {
            throw new DriverException(e);
        }
        this.connectors.add(createConnector(STATION_CMD, STATION_ROUTE, context.getRawDataBroker()));
        this.running = true;
    }

    private ITransportConnector createConnector(String cmdType, String route, IRawDataBroker rawDataBroker) {
        // TODO
        return null;
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
        return Collections.unmodifiableList(this.connectors);
    }

    @Override
    public void dispose() {
        running = false;
        for(ITransportConnector ctr : connectors) {
            try {
                ctr.abort();
            } catch (TransportException e) {
                // Ignore, it is test driver
                e.printStackTrace();
            }
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
        return routes;
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return types;
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        LOG.info("Activity invocation: " + activityInvocation);
        if(!types.contains(activityInvocation.getType())) {
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
        for(ITransportConnector c : this.connectors) {
            if(c.isInitialised() && c instanceof TelecommandTransportConnectorImpl) {
                if(((TelecommandTransportConnectorImpl) c).getType().equals(type)
                && ((TelecommandTransportConnectorImpl) c).getRoutes().contains(route)
                && ((TelecommandTransportConnectorImpl) c).isReady()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        try {
            storeRawData(activityInvocation.getActivityOccurrenceId(), activityInvocation.getPath(), activityInvocation.getGenerationTime(), activityInvocation.getRoute(), "TC");
            announce(activityInvocation, model, "Final Release", ActivityReportState.OK, ActivityOccurrenceState.RELEASE, ActivityOccurrenceState.TRANSMISSION);
            for (int i = 0; i < 3; ++i) {
                announce(activityInvocation, model, "T" + i, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
            }
            int transmissionForEachState = 2000 / 3;
            for (int i = 0; i < 3; ++i) {
                Thread.sleep(transmissionForEachState);
                announce(activityInvocation, model, "T" + i, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, i != 3 - 1 ? ActivityOccurrenceState.TRANSMISSION : ActivityOccurrenceState.EXECUTION);
            }
        } catch(Exception e) {
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            return;
        }
        try {
            for (int i = 0; i < 4; ++i) {
                announce(activityInvocation, model, "E" + i, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION);
            }
            int executionForEachState = 3500 / 4;
            for (int i = 0; i < 4; ++i) {
                Thread.sleep(executionForEachState);
                announce(activityInvocation, model, "E" + i, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, i != 4 - 1 ? ActivityOccurrenceState.EXECUTION : ActivityOccurrenceState.VERIFICATION, null, null);
            }
        } catch(Exception e) {
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION);
        }
    }

    private void storeRawData(IUniqueId activityOccurrenceId, SystemEntityPath path, Instant generationTime, String route, String type) throws ReatmetricException {
        IUniqueId internalId = context.getRawDataBroker().nextRawDataId();
        RawData rd = new RawData(internalId, generationTime, path.getLastPathElement(), type, route, "", Quality.GOOD, activityOccurrenceId, new byte[25], Instant.now(), null);
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
    public String getSource() {
        return STATION_SOURCE;
    }

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList(STATION_CMD, STATION_ACK, STATION_TM, STATION_EVENT);
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException {
        // TODO
        return null;
    }
}
