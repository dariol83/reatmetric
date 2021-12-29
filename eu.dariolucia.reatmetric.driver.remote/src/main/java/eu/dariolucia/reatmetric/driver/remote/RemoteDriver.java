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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class RemoteDriver implements IDriver, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(RemoteDriver.class.getName());

    public static final String CONFIGURATION_FILE = "configuration.xml";

    // Driver generic properties
    private String name;
    private IServiceCoreContext context;
    private IDriverListener driverSubscriber;
    private SystemStatus driverStatus;

    // For activity handling
    private IProcessingModel model;
    private List<String> supportedActivityTypes;

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
        this.driverSubscriber = subscriber;
        // Create the protocol manager

        try {
            // Read the configuration
            this.configuration = RemoteConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));
            // Create connector
            this.remoteSystemConnector = new RemoteSystemConnector(this, this.configuration);
            // Start the connector
            this.remoteSystemConnector.prepare();
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
        String prefix = this.configuration.getLocalPathPrefix();
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
                LOG.log(Level.SEVERE, "Error when distributing operational message from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
            }
        }
    }

    public <T extends AbstractDataItem> void ingestProcessingData(List<T> data) {
        try {
            context.getProcessingModel().mirror((List<AbstractDataItem>) data);
        } catch (ProcessingModelException e) {
            LOG.log(Level.SEVERE, "Error when mirroring processing data from remote system " + this.configuration.getName() + ": " + e.getMessage(), e);
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
        return Collections.singletonList(configuration.getRoute());
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
        String prefix = this.configuration.getLocalPathPrefix();
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
                    ActivityDescriptor ad = null;
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
        // TODO - If invocation is for an activity of the managed subtree, forward and report remote activity occurrence id,
        //  as result, and complete this invocation. Else exception.
        // TODO - Post-execution verification and timeouts to be ignored in the local processing model.
    }

    @Override
    public boolean getRouteAvailability(String route) throws ActivityHandlingException {
        if(!route.equals(this.configuration.getRoute())) {
            return false;
        } else {
            return this.remoteSystemConnector.getConnectionStatus() == TransportConnectionStatus.OPEN;
        }
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // TODO - If activityId is for an activity of the managed subtree, then fetch current (local) activity state,
        //  retrieve the remote activity id (result) and, if present, forward the abort request.
    }
}
