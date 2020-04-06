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
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestDriver implements IDriver, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(TestDriver.class.getName());

    private volatile boolean running;
    private volatile IServiceCoreContext context;
    private volatile IProcessingModel model;
    private volatile ProcessingDefinition definitions;
    private volatile IDriverListener subscriber;

    // For activity execution
    private final ExecutorService executor = Executors.newFixedThreadPool(4, (t) -> {
        Thread toReturn = new Thread(t, "TestDriver Activity Handler Thread");
        toReturn.setDaemon(true);
        return toReturn;
    });
    private final List<ITransportConnector> connectors = new LinkedList<>();
    private final List<String> types = Arrays.asList("TC", "Custom");
    private final List<String> routes = Arrays.asList("RouteA", "RouteB");

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
        this.connectors.add(createTcConnector("TC", "RouteA"));
        this.connectors.add(createTcConnector("Custom", "RouteA", "RouteB"));
        this.connectors.add(createTmConnector("TM", context.getRawDataBroker(), "RouteA"));
        this.running = true;
    }

    @Override
    public SystemStatus getDriverStatus() {
        return SystemStatus.NOMINAL;
    }

    private ITransportConnector createTcConnector(String type, String... routes) {
        return new TelecommandTransportConnectorImpl(type, type, routes);
    }

    private ITransportConnector createTmConnector(String name, IRawDataBroker broker, String... routes) {
        return new TelemetryTransportConnectorImpl(name, routes, definitions, context.getProcessingModel(), broker);
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this);
    }

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
            log(activityInvocation, "Release finalisation");
            storeRawData(activityInvocation.getActivityOccurrenceId(), activityInvocation.getPath(), activityInvocation.getGenerationTime(), activityInvocation.getRoute(), "TC");
            announce(activityInvocation, model, "Final Release", ActivityReportState.OK, ActivityOccurrenceState.RELEASE, ActivityOccurrenceState.TRANSMISSION);
            log(activityInvocation, "Transmission started");
            for (int i = 0; i < 3; ++i) {
                announce(activityInvocation, model, "T" + i, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
            }
            int transmissionForEachState = 2000 / 3;
            for (int i = 0; i < 3; ++i) {
                Thread.sleep(transmissionForEachState);
                announce(activityInvocation, model, "T" + i, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, i != 3 - 1 ? ActivityOccurrenceState.TRANSMISSION : ActivityOccurrenceState.EXECUTION);
            }
            log(activityInvocation, "Transmission completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            return;
        }
        try {
            log(activityInvocation, "Execution started");
            for (int i = 0; i < 4; ++i) {
                announce(activityInvocation, model, "E" + i, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION);
            }
            int executionForEachState = 3500 / 4;
            for (int i = 0; i < 4; ++i) {
                Thread.sleep(executionForEachState);
                announce(activityInvocation, model, "E" + i, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, i != 4 - 1 ? ActivityOccurrenceState.EXECUTION : ActivityOccurrenceState.VERIFICATION, null, null);
            }
            log(activityInvocation, "Execution completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION);
        }
    }

    private void storeRawData(IUniqueId activityOccurrenceId, SystemEntityPath path, Instant generationTime, String route, String type) throws ReatmetricException {
        IUniqueId internalId = context.getRawDataBroker().nextRawDataId();
        RawData rd = new RawData(internalId, generationTime, path.getLastPathElement(), type, route, "", Quality.GOOD, activityOccurrenceId, new byte[25], Instant.now(), null);
        context.getRawDataBroker().distribute(Collections.singletonList(rd), true);
    }

    protected void log(IActivityHandler.ActivityInvocation invocation, String message) {
        log(invocation, message, null);
    }

    protected void log(IActivityHandler.ActivityInvocation invocation, String message, Exception e) {
        LOG.log(Level.INFO, String.format("Activity occurrence %d - %s - %s", invocation.getActivityOccurrenceId().asLong(), invocation.getPath(), message), e);
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

}
