package eu.dariolucia.reatmetric.driver.remote.connectors;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.driver.remote.RemoteDriver;
import eu.dariolucia.reatmetric.driver.remote.definition.RemoteConfiguration;
import eu.dariolucia.reatmetric.remoting.connector.ReatmetricConnectorRegistry;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RemoteSystemConnector extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(RemoteSystemConnector.class.getName());

    private final RemoteDriver driver;
    private final RemoteConfiguration configuration;

    private volatile IReatmetricSystem system;

    private final IParameterDataSubscriber parameterDataSubscriber = this::remoteParametersReceived;

    private void remoteParametersReceived(List<ParameterData> parameterData) {
        driver.ingestProcessingData(parameterData);
    }

    private final IEventDataSubscriber eventDataSubscriber = this::remoteEventsReceived;

    private void remoteEventsReceived(List<EventData> eventData) {
        driver.ingestProcessingData(eventData);
    }

    private final IActivityOccurrenceDataSubscriber activityDataSubscriber = this::remoteActivitiesReceived;

    private void remoteActivitiesReceived(List<ActivityOccurrenceData> activityOccurrenceData) {
        driver.ingestActivityProcessingData(activityOccurrenceData);
    }

    private final IOperationalMessageSubscriber messageSubscriber = this::remoteMessagesReceived;

    private void remoteMessagesReceived(List<OperationalMessage> operationalMessages) {
        driver.ingestMessages(operationalMessages);
    }

    public RemoteSystemConnector(RemoteDriver remoteDriver, RemoteConfiguration configuration) {
        super(configuration.getName() + " connector", "Connector to remote Reatmetric system " + configuration.getName());
        this.driver = remoteDriver;
        this.configuration = configuration;
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        return Pair.of(0L, 0L); // No way to really know the amount of data transferred/received from the remote system
    }

    @Override
    protected synchronized void doConnect() {
        if(system != null) {
            // Do nothing
            return;
        }
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        try {
            // Discover the system
            ReatmetricConnectorRegistry registry = new ReatmetricConnectorRegistry();
            for (IReatmetricSystem s : registry.availableSystems()) {
                if (s.getName().equals(this.configuration.getName())) {
                    system = s;
                    break;
                }
            }

            if (system == null) {
                // No such system
                throw new TransportException("Remote system " + this.configuration.getName() +
                        " not found in the connector registry: ensure that the property " +
                        ReatmetricConnectorRegistry.INIT_FILE_KEY + " is set");
            }

            // Initialise the system
            AtomicReference<SystemStatus> initResult = new AtomicReference<>();
            system.initialise(initResult::set);

            LOG.log(Level.INFO, "Remote system " + this.configuration.getName() + " initialisation status: " + initResult.get());

            // Register to the remote system
            registerToParameters();
            registerToEvents();
            registerToActivities();
            registerToMessages();

            // Done
            updateConnectionStatus(TransportConnectionStatus.OPEN);
            switch(initResult.get()) {
                case ALARM:
                    updateAlarmState(AlarmState.ALARM);
                    break;
                case WARNING:
                    updateAlarmState(AlarmState.WARNING);
                    break;
                default:
                    updateAlarmState(AlarmState.NOMINAL);
                    break;
            }
            LOG.log(Level.INFO, "Nominal connection to remote system " + this.configuration.getName() + " established");
        } catch (Exception e) {
            reportConnectionProblem(e);
        }
    }

    private void registerToParameters() throws RemoteException, ReatmetricException {
        // Get all parameters by navigating from localPathPrefix
        List<Integer> parameters = driver.getLocalParameters();
        // Subscribe
        system.getParameterDataMonitorService().subscribe(this.parameterDataSubscriber, new ParameterDataFilter(null, null, null, null, null, parameters));
    }

    private void registerToEvents() throws RemoteException, ReatmetricException {
        // Get all events by navigating from localPathPrefix
        List<Integer> events = driver.getLocalEvents();
        // Subscribe
        system.getEventDataMonitorService().subscribe(this.eventDataSubscriber, new EventDataFilter(null, null, null, null, null, null, events));
    }

    private void registerToActivities() throws RemoteException, ReatmetricException {
        // Get all activities by navigating from localPathPrefix
        List<Integer> activities = driver.getLocalActivities();
        // Subscribe
        system.getActivityOccurrenceDataMonitorService().subscribe(this.activityDataSubscriber, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, activities));
    }

    private void registerToMessages() throws RemoteException, ReatmetricException {
        // Subscribe
        system.getOperationalMessageMonitorService().subscribe(this.messageSubscriber, new OperationalMessageFilter(null, null, null, List.of(Severity.ALARM, Severity.WARN, Severity.INFO, Severity.ERROR)));
    }

    @Override
    protected synchronized void doDisconnect() {
        if(system == null) {
            // Do nothing
            return;
        }
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        try {
            // Deregister from the remote system
            try {
                system.getParameterDataMonitorService().unsubscribe(this.parameterDataSubscriber);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Cannot unsubscribe parameter data from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
            try {
                system.getEventDataMonitorService().unsubscribe(this.eventDataSubscriber);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Cannot unsubscribe event data from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
            try {
                system.getActivityOccurrenceDataMonitorService().unsubscribe(this.activityDataSubscriber);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Cannot unsubscribe activity data from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
            try {
                system.getOperationalMessageMonitorService().unsubscribe(this.messageSubscriber);
            } catch (Exception e) {
                LOG.log(Level.FINE, "Cannot unsubscribe operational messages from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }

            try {
                system.dispose();
            } catch (ReatmetricException | RemoteException ex) {
                LOG.log(Level.WARNING, "Cannot close connection to remote system " + this.configuration.getName() + ": " + ex.getMessage(), ex);
            }
            system = null;
            // Done
            updateConnectionStatus(TransportConnectionStatus.IDLE);
            updateAlarmState(AlarmState.NOT_APPLICABLE);
            LOG.log(Level.INFO, "Nominal connection to remote system " + this.configuration.getName() + " closed");
        } catch (Exception e) {
            reportConnectionProblem(e);
        }
    }

    private void reportConnectionProblem(Exception e) {
        LOG.log(Level.SEVERE, "Problem when connecting to remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
        // Clean up system
        if(system != null) {
            try {
                system.dispose();
            } catch (ReatmetricException | RemoteException ex) {
                LOG.log(Level.SEVERE, "Cannot dispose connection to remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
        }
        system = null;

        // Done
        updateConnectionStatus(TransportConnectionStatus.ERROR);
        updateAlarmState(AlarmState.ERROR);
    }

    @Override
    protected synchronized void doDispose() {
        try {
            disconnect();
        } catch (TransportException e) {
            LOG.log(Level.SEVERE, "Error when disposing connector to remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
        }
        system = null;
    }

    @Override
    public synchronized void abort() throws TransportException, RemoteException {
        disconnect();
    }

    /**
     * Connect to the remote system and retrieve the remote available routes.
     *
     * @return the available routes
     */
    public List<String> retrieveRemoteRoutes() {
        IReatmetricSystem s = getReatmetricSystem();
        if(s != null) {
            try {
                List<ActivityRouteState> routes = s.getActivityExecutionService().getRouteAvailability();
                return routes.stream().map(ActivityRouteState::getRoute).collect(Collectors.toList());
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Error when retrieving available routes from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
        }
        // Cannot retrieve, no system, no routes
        return Collections.emptyList();
    }

    private synchronized IReatmetricSystem getReatmetricSystem() {
        return this.system;
    }

    /**
     * Check if the specified route is available on the remote system.
     *
     * @param route the remote route to check (remote name)
     * @return true if the route is available, otherwise false
     */
    public boolean isRemoteRouteAvailable(String route) {
        IReatmetricSystem s = getReatmetricSystem();
        if(s != null) {
            // Route is clean
            route = route.substring(0, route.lastIndexOf(RemoteDriver.ROUTE_SYSTEM_SEPARATOR));
            try {
                List<ActivityRouteState> routes = s.getActivityExecutionService().getRouteAvailability();
                for (ActivityRouteState r : routes) {
                    if (r.getRoute().equals(route)) {
                        return r.getAvailability() == ActivityRouteAvailability.AVAILABLE;
                    }
                }
                return false;
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Error when retrieving available routes from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
        }
        return false;
    }

    public IUniqueId invokeRemoteActivity(ActivityRequest request) throws ActivityHandlingException {
        IReatmetricSystem s = getReatmetricSystem();
        if(s != null) {
            try {
                return s.getActivityExecutionService().startActivity(request);
            } catch (ReatmetricException | RemoteException e) {
                throw new ActivityHandlingException("Forwarding of activity request (" + request.getId() + ") with path " + request.getPath() + " to remote system failed: " + e.getMessage(), e);
            }
        } else {
            throw new ActivityHandlingException("Remote system " + configuration.getName() + " not available, remote activity invocation " + request.getPath() + " failed");
        }
    }

    public void invokeRemoteAbort(int activityId, IUniqueId remoteOccurrenceId) throws ActivityHandlingException {
        IReatmetricSystem s = getReatmetricSystem();
        if(s != null) {
            try {
                s.getActivityExecutionService().abortActivity(activityId, remoteOccurrenceId);
            } catch (ReatmetricException | RemoteException e) {
                throw new ActivityHandlingException("Forwarding of abort request for activity " + activityId + "[" + remoteOccurrenceId + "] to remote system failed: " + e.getMessage(), e);
            }
        } else {
            throw new ActivityHandlingException("Remote system " + configuration.getName() + " not available, remote abort invocation for activity " + activityId + "[" + remoteOccurrenceId + "] failed");
        }
    }
}
