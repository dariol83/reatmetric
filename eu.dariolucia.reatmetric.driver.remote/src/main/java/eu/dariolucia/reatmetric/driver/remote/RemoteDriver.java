/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.remote;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.value.Array;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.remote.connectors.RemoteSystemConnector;
import eu.dariolucia.reatmetric.driver.remote.definition.RemoteConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 */
public class RemoteDriver implements IDriver, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(RemoteDriver.class.getName());

    public static final String CONFIGURATION_FILE = "configuration.xml";

    public static final String RELAY_STAGE_NAME = "Relay";
    public static final String ENTITY_SYSTEM_SEPARATOR = "@";

    // Driver generic properties
    private String name;
    private IServiceCoreContext context;
    private SystemStatus driverStatus;

    // For activity handling
    private volatile IProcessingModel model;
    private List<String> supportedActivityTypes; // From the definitions
    private final Set<String> supportedRoutes = new LinkedHashSet<>(); // From the remote system, plus suffix '@configuration.getName()'
    private ExecutorService activityExecutor;

    // Driver specific properties
    private RemoteConfiguration configuration;

    // Remote system
    private RemoteSystemConnector remoteSystemConnector;

    public RemoteDriver() {
        //
    }

    // --------------------------------------------------------------------
    // IDriver methods
    // --------------------------------------------------------------------

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.driverStatus = SystemStatus.NOMINAL;
        try {
            // Read the configuration
            this.configuration = RemoteConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));
            // Create connector
            this.remoteSystemConnector = new RemoteSystemConnector(this, this.configuration);
            // Start the connector
            this.remoteSystemConnector.prepare();
            // Start the thread to handle activity execution requests on the remote system
            this.activityExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Remote System " + this.configuration.getRemoteSystemName() + " - Activity Handler Thread");
                t.setDaemon(true);
                return t;
            });
            // Inform that everything is fine
            this.driverStatus = SystemStatus.NOMINAL;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
        } catch (Exception e) {
            this.driverStatus = SystemStatus.ALARM;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
            throw new DriverException(e);
        }
    }

    @Override
    public SystemStatus getDriverStatus() {
        return this.driverStatus;
    }

    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.emptyList();
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this);
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.singletonList(this.remoteSystemConnector);
    }

    @Override
    public void dispose() {
        this.remoteSystemConnector.dispose();
        this.activityExecutor.shutdown();
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Collections.emptyList();
    }

    // --------------------------------------------------------------------
    // Internal methods
    // --------------------------------------------------------------------

    public List<Integer> getLocalParameters() {
        return visitFor(SystemEntityType.PARAMETER);
    }

    public List<Integer> getLocalEvents() {
        return visitFor(SystemEntityType.EVENT);
    }

    public List<Integer> getLocalActivities() {
        return visitFor(SystemEntityType.ACTIVITY);
    }

    private List<Integer> visitFor(SystemEntityType type) {
        List<Integer> toReturn = new LinkedList<>();
        String prefix = this.configuration.getRemotePathSelector();
        // Visit the model for type
        this.context.getProcessingModel().visit(new IProcessingModelVisitor() {
            @Override
            public boolean shouldDescend(SystemEntity path) {
                return path.getPath().asString().startsWith(prefix) || prefix.startsWith(path.getPath().asString());
            }

            @Override
            public void startVisit(SystemEntity path) {
                if(path.getType() == type && path.getPath().asString().startsWith(prefix)) {
                    toReturn.add(path.getExternalId());
                }
            }

            @Override
            public void onVisit(AbstractDataItem item) {
                // Nothing
            }

            @Override
            public void endVisit(SystemEntity path) {
                // Nothing
            }
        });
        // Done
        return toReturn;
    }

    public void ingestMessages(List<OperationalMessage> operationalMessages) {
        for(OperationalMessage om : operationalMessages) {
            try {
                context.getOperationalMessageBroker().distribute(om.getId(), om.getMessage(), om.getSource(), om.getSeverity(), om.getExtension(), om.getLinkedEntityId(), true);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Error when distributing operational message from remote system " + this.configuration.getRemoteSystemName() + ": " + e.getMessage(), e);
            }
        }
    }

    public <T extends AbstractDataItem> void ingestProcessingData(List<T> data) {
        try {
            context.getProcessingModel().mirror((List<AbstractDataItem>) data);
        } catch (ProcessingModelException e) {
            LOG.log(Level.SEVERE, "Error when mirroring processing data from remote system " + this.configuration.getRemoteSystemName() + ": " + e.getMessage(), e);
        }
    }

    public void ingestActivityProcessingData(List<ActivityOccurrenceData> activityOccurrenceData) {
        try {
            // Remap route
            List<AbstractDataItem> remapped = new ArrayList<>(activityOccurrenceData.size());
            for(ActivityOccurrenceData aod : activityOccurrenceData) {
                ActivityOccurrenceData remappedAod = new ActivityOccurrenceData(aod.getInternalId(), aod.getGenerationTime(),
                        aod.getExtension(), aod.getExternalId(), aod.getName(), aod.getPath(), aod.getType(),
                        aod.getArguments(),
                        aod.getProperties(),
                        aod.getProgressReports(),
                        aod.getRoute() + RemoteDriver.ENTITY_SYSTEM_SEPARATOR + configuration.getRemoteSystemName(),
                        aod.getSource());
                remapped.add(remappedAod);
            }
            context.getProcessingModel().mirror(remapped);
        } catch (ProcessingModelException e) {
            LOG.log(Level.SEVERE, "Error when mirroring activity processing data from remote system " + this.configuration.getRemoteSystemName() + ": " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------------------
    // IActivityHandler methods
    // --------------------------------------------------------------------

    @Override
    public void registerModel(IProcessingModel model) {
        this.model = model;
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        this.model = null;
    }

    @Override
    public List<String> getSupportedRoutes() {
        // With the approach below, as soon as the routes are present (even if new routes)
        // they will be added to the set and always checked for availability
        List<String> routes = this.remoteSystemConnector.retrieveRemoteRoutes().stream().map(o -> o + ENTITY_SYSTEM_SEPARATOR + configuration.getRemoteSystemName()).collect(Collectors.toList());
        this.supportedRoutes.addAll(routes);
        return new ArrayList<>(this.supportedRoutes);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        if(this.supportedActivityTypes == null) {
            retrieveSupportedActivityTypes();
        }
        return this.supportedActivityTypes;
    }

    private void retrieveSupportedActivityTypes() {
        Set<String> toReturn = new TreeSet<>();
        String prefix = this.configuration.getRemotePathSelector();
        // Visit the model for type
        model.visit(new IProcessingModelVisitor() {
            @Override
            public boolean shouldDescend(SystemEntity path) {
                return path.getPath().asString().startsWith(prefix) || prefix.startsWith(path.getPath().asString());
            }

            @Override
            public void startVisit(SystemEntity path) {
                if(path.getType() == SystemEntityType.ACTIVITY && path.getPath().asString().startsWith(prefix)) {
                    // Get descriptor
                    ActivityDescriptor ad;
                    try {
                        ad = (ActivityDescriptor) model.getDescriptorOf(path.getExternalId());
                        toReturn.add(ad.getActivityType());
                    } catch (ProcessingModelException e) {
                        LOG.log(Level.WARNING, "Cannot retrieve activity descriptor of entity " + path.getExternalId() + ": " + e.getMessage(), e);
                    }
                }
            }

            @Override
            public void onVisit(AbstractDataItem item) {
                // Nothing
            }

            @Override
            public void endVisit(SystemEntity path) {
                // Nothing
            }
        });
        // Done
        this.supportedActivityTypes = List.copyOf(new ArrayList<>(toReturn));
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        if(this.activityExecutor.isShutdown()) {
            throw new ActivityHandlingException(this.name + " driver disposed");
        }
        if(!isInManagedSubtree(activityInvocation)) {
            throw new ActivityHandlingException("Activity " + activityInvocation.getPath() + " is not part of remote system " + configuration.getRemoteSystemName());
        }
        if(this.remoteSystemConnector.getConnectionStatus() != TransportConnectionStatus.OPEN) {
            throw new ActivityHandlingException("Remote system " + configuration.getRemoteSystemName() + " is not connected");
        }
        String route = getRemoteRouteIdentifier(activityInvocation.getRoute());
        if(!this.remoteSystemConnector.isRemoteRouteAvailable(route)) {
            throw new ActivityHandlingException("Route " + route + " on remote system " + configuration.getRemoteSystemName() + " is not available");
        }
        ActivityRequest request = deriveActivityRequest(activityInvocation);
        // At this stage, all checks are completed, go for it
        this.activityExecutor.execute(() -> executeRemoteActivity(activityInvocation, request));
    }

    private void executeRemoteActivity(ActivityInvocation localInvocation, ActivityRequest mappedRequest) {
        // If invocation is for an activity of the managed subtree, forward and report remote activity occurrence id,
        // as result, and complete this invocation. Else exception.
        try {
            // Record verification
            announce(localInvocation, Instant.now(), RELAY_STAGE_NAME, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceState.TRANSMISSION, null);
            // Transmission
            IUniqueId remoteActivityOccurrenceId = remoteSystemConnector.invokeRemoteActivity(mappedRequest);
            // No exception means that the activity transmission is done and the activity is on the remote system now, this occurrence is over and the
            // processing model can transition it to complete stage
            announce(localInvocation, Instant.now(), RELAY_STAGE_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceState.VERIFICATION, remoteActivityOccurrenceId.asLong());
        } catch(Exception e) {
            LOG.log(Level.SEVERE, "Transmission of activity " + localInvocation.getActivityOccurrenceId() + " failed: " + e.getMessage(), e);
            announce(localInvocation, Instant.now(), RELAY_STAGE_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION, ActivityOccurrenceState.TRANSMISSION, null);
        }
    }

    private ActivityRequest deriveActivityRequest(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        IProcessingModel cModel = this.model;
        if(cModel == null) {
            throw new ActivityHandlingException("Model not registered in driver " + this.name);
        }
        String remotePathToUse = activityInvocation.getPath().asString();
        if(!remotePathToUse.startsWith(configuration.getRemotePathPrefix())) {
            throw new ActivityHandlingException("Remote activity path derivation failed: invoked activity path is " + activityInvocation.getPath().asString() + ", but remote path prefix is " + configuration.getRemotePathPrefix());
        }
        remotePathToUse = remotePathToUse.substring(configuration.getRemotePathPrefix().length());
        ActivityRequest.Builder builder = ActivityRequest.newRequest(activityInvocation.getActivityId(), SystemEntityPath.fromString(remotePathToUse));
        String route = getRemoteRouteIdentifier(activityInvocation.getRoute());
        builder.withRoute(route);
        builder.withSource(activityInvocation.getSource() + ENTITY_SYSTEM_SEPARATOR + context.getSystemName());
        builder.withProperties(activityInvocation.getProperties());
        // Map arguments
        ActivityDescriptor activityDescriptor;
        try {
            activityDescriptor = (ActivityDescriptor) cModel.getDescriptorOf(activityInvocation.getActivityId());
        } catch (ProcessingModelException | ClassCastException e) {
            throw new ActivityHandlingException("Cannot retrieve activity " + activityInvocation.getActivityId() + " from processing model", e);
        }
        if(activityDescriptor == null) {
            throw new ActivityHandlingException("Activity " + activityInvocation.getActivityId() + " not defined in the processing model");
        }
        List<AbstractActivityArgument> argMap = new LinkedList<>();
        for(Map.Entry<String, Object> arg : activityInvocation.getArguments().entrySet()) {
            // Remember: at this stage, all args are de-calibrated
            // Retrieve the argument descriptor
            AbstractActivityArgumentDescriptor argDesc = getArgumentDescriptor(activityDescriptor, arg.getKey());
            if(argDesc == null) {
                throw new ActivityHandlingException("Provided argument " + arg.getKey() + " not found in the list of arguments for activity " + activityInvocation.getPath());
            }
            if(argDesc instanceof ActivityPlainArgumentDescriptor) {
                argMap.add(new PlainActivityArgument(arg.getKey(), arg.getValue(), null, false));
            } else if(argDesc instanceof ActivityArrayArgumentDescriptor) {
                ActivityArrayArgumentDescriptor arrayDesc = (ActivityArrayArgumentDescriptor) argDesc;
                // This means that the value is Array (eu.dariolucia.reatmetric.api)
                Array arrayValue = (Array) arg.getValue();
                // Map to value hierarchy
                ArrayActivityArgument arrayArg = mapToArrayArgument(activityInvocation, arrayDesc, arrayValue);
                argMap.add(arrayArg);
            }
        }
        builder.withArguments(argMap);
        return builder.build();
    }

    private ArrayActivityArgument mapToArrayArgument(ActivityInvocation invocation, ActivityArrayArgumentDescriptor arrayDesc, Array arrayValue) throws ActivityHandlingException {
        List<ArrayActivityArgumentRecord> records = new ArrayList<>(arrayValue.getRecords().size());
        for(Array.Record arrayValueRecord : arrayValue.getRecords()) {
            List<AbstractActivityArgument> argMap = new LinkedList<>();
            for(Pair<String, Object> arg : arrayValueRecord.getElements()) {
                // Retrieve the argument descriptor
                AbstractActivityArgumentDescriptor argDesc = getArrayArgumentDescriptor(arrayDesc, arg.getFirst());
                if(argDesc == null) {
                    throw new ActivityHandlingException("Provided array argument " + arg.getFirst() + " not found in the list of arguments for activity " + invocation.getPath());
                }
                if(argDesc instanceof ActivityPlainArgumentDescriptor) {
                    argMap.add(new PlainActivityArgument(arg.getFirst(), arg.getSecond(), null, false));
                } else if(argDesc instanceof ActivityArrayArgumentDescriptor) {
                    ActivityArrayArgumentDescriptor arrayDesc2 = (ActivityArrayArgumentDescriptor) argDesc;
                    // This means that the value is Array (eu.dariolucia.reatmetric.api)
                    Array arrayValue2 = (Array) arg.getSecond();
                    // Map to value hierarchy
                    ArrayActivityArgument arrayArg = mapToArrayArgument(invocation, arrayDesc2, arrayValue2);
                    argMap.add(arrayArg);
                }
            }
            ArrayActivityArgumentRecord rec = new ArrayActivityArgumentRecord(argMap);
            records.add(rec);
        }
        return new ArrayActivityArgument(arrayDesc.getName(), records);
    }

    private AbstractActivityArgumentDescriptor getArrayArgumentDescriptor(ActivityArrayArgumentDescriptor arrayDesc, String recordName) {
        for(AbstractActivityArgumentDescriptor argDesc : arrayDesc.getElements()) {
            if(argDesc.getName().equals(recordName)) {
                return argDesc;
            }
        }
        return null;
    }

    private AbstractActivityArgumentDescriptor getArgumentDescriptor(ActivityDescriptor activityDescriptor, String argumentName) {
        for(AbstractActivityArgumentDescriptor argDesc : activityDescriptor.getArgumentDescriptors()) {
            if(argDesc.getName().equals(argumentName)) {
                return argDesc;
            }
        }
        return null;
    }

    public synchronized void announce(IActivityHandler.ActivityInvocation invocation, Instant genTime, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextState, Object result) {
        IProcessingModel cModel = this.model;
        if(cModel != null) {
            cModel.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, genTime, occState, null, reportState, nextState, result));
        }
    }

    private boolean isInManagedSubtree(ActivityInvocation activityInvocation) {
        return activityInvocation.getPath().asString().startsWith(this.configuration.getRemotePathSelector());
    }

    @Override
    public boolean getRouteAvailability(String route) {
        if(!this.supportedRoutes.contains(route)) {
            // No route available
            return false;
        } else if (this.remoteSystemConnector.getConnectionStatus() != TransportConnectionStatus.OPEN) {
            // No remote system available
            return false;
        } else {
            // Route is remotely available? Extract remote route name and ask the connector
            route = getRemoteRouteIdentifier(route);
            return this.remoteSystemConnector.isRemoteRouteAvailable(route);
        }
    }

    private String getRemoteRouteIdentifier(String route) {
        return route.substring(0, route.lastIndexOf(ENTITY_SYSTEM_SEPARATOR));
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // If activityId is for an activity of the managed subtree, then fetch current (local) activity state,
        // retrieve the remote activity id (property field or result) and, if present, forward the abort request.
        if(this.activityExecutor.isShutdown()) {
            throw new ActivityHandlingException(this.name + " driver disposed");
        }
        if(this.remoteSystemConnector.getConnectionStatus() != TransportConnectionStatus.OPEN) {
            throw new ActivityHandlingException("Remote system " + configuration.getRemoteSystemName() + " is not connected");
        }
        IProcessingModel cModel = this.model;
        if(cModel == null) {
            throw new ActivityHandlingException("Model not registered in driver " + this.name);
        }
        // We need to support the abort of an activity even remotely executed, so we check the properties
        List<AbstractDataItem> activityStates;
        try {
            activityStates = cModel.getById(Collections.singletonList(activityId));
        } catch (ProcessingModelException e) {
            throw new ActivityHandlingException("Cannot retrieve occurrences of activity ID " + activityId + " from the processing model: " + e.getMessage(), e);
        }
        for(AbstractDataItem as : activityStates) {
            if(as instanceof ActivityOccurrenceData && as.getInternalId().equals(activityOccurrenceId)) {
                Map<String, String> properties = ((ActivityOccurrenceData) as).getProperties();
                if(properties.containsKey(ActivityOccurrenceData.MIRRORED_ACTIVITY_OCCURRENCE_ID_PROPERTY_KEY)) {
                    IUniqueId remoteOccurrenceId = new LongUniqueId(Long.parseLong(properties.get(ActivityOccurrenceData.MIRRORED_ACTIVITY_OCCURRENCE_ID_PROPERTY_KEY)));
                    activityExecutor.execute(() -> executeRemoteAbort(activityId, remoteOccurrenceId));
                    return;
                } else if(((ActivityOccurrenceData) as).getResult() != null && ((ActivityOccurrenceData) as).getResult() instanceof Long) {
                    IUniqueId remoteOccurrenceId = new LongUniqueId((Long) ((ActivityOccurrenceData) as).getResult());
                    activityExecutor.execute(() -> executeRemoteAbort(activityId, remoteOccurrenceId));
                    return;
                }
            }
        }
        throw new ActivityHandlingException("Cannot abort occurrence " + activityOccurrenceId.asLong() + " of activity " + activityId + ": remote occurrence ID not found");
    }

    private void executeRemoteAbort(int activityId, IUniqueId remoteOccurrenceId) {
        try {
            remoteSystemConnector.invokeRemoteAbort(activityId, remoteOccurrenceId);
        } catch(Exception e) {
            LOG.log(Level.SEVERE, "Abort of activity " + activityId + "[" + remoteOccurrenceId + "] failed: " + e.getMessage(), e);
        }
    }
}
