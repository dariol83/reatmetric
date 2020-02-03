/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
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
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration) throws DriverException {
        try {
            this.definitions = ProcessingDefinition.loadAll(coreConfiguration.getDefinitionsLocation());
        } catch (ReatmetricException e) {
            throw new DriverException(e);
        }
        // TODO add raw data broker
        this.connectors.add(createTcConnector("TC", "RouteA"));
        // TODO add raw data broker
        this.connectors.add(createTcConnector("Custom", "RouteA", "RouteB"));
        // TODO add raw data broker
        this.connectors.add(createTmConnector("TM", "RouteA"));
        this.running = true;
        this.context = context;
    }

    private ITransportConnector createTcConnector(String type, String... routes) {
        return new TelecommandTransportConnectorImpl(type, type, routes);
    }

    private ITransportConnector createTmConnector(String name, String... routes) {
        return new TelemetryTransportConnectorImpl(name, routes, definitions, context.getProcessingModel());
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
