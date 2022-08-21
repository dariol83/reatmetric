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

package eu.dariolucia.reatmetric.driver.httpserver;

import com.sun.net.httpserver.HttpServer;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.httpserver.definition.HttpServerConfiguration;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * HTTP server driver that can provide monitoring data (parameters, events, operational messages, alarms) based on a REST API.
 */
public class HttpServerDriver implements IDriver {

    private static final Logger LOG = Logger.getLogger(HttpServerDriver.class.getName());

    public static final String CONFIGURATION_FILE = "configuration.xml";

    public static final String PARAMETERS_PATH = "parameters";
    public static final String PARAMETER_CURRENT_STATE_PATH = "current";
    public static final String PARAMETER_STREAM_PATH = "stream";
    public static final String EVENTS_PATH = "events";
    public static final String MESSAGES_PATH = "messages";
    public static final String MODEL_PATH = "model";
    public static final String CONNECTORS_PATH = "connectors";

    // For subscriptions
    public static final String REGISTRATION_URL = "register";
    public static final String GET_URL = "get";
    public static final String DEREGISTRATION_URL = "deregister";
    public static final String LIST_URL = "list";

    // Driver generic properties
    private String name;
    private IServiceCoreContext context;
    private IDriverListener driverSubscriber;
    private SystemStatus driverStatus;
    // Driver specific properties
    private HttpServerConfiguration configuration;
    private HttpServer server;
    // Handlers
    private final Map<String, AbstractHttpRequestHandler> context2handlers = new ConcurrentHashMap<>();
    // Handler cleanup for subscriptions
    private volatile Timer cleanupTimer;
    private volatile TimerTask cleanupJob;
    // Model information cache
    private final List<ParameterDescriptor> parameters = new LinkedList<>();
    private final List<EventDescriptor> events = new LinkedList<>();
    private final List<ActivityDescriptor> activities = new LinkedList<>();

    public HttpServerDriver() {
        // Nothing to do here
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
        try {
            // Read the configuration
            this.configuration = HttpServerConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));

            // Build parameters/events cache
            buildCache();

            // Create handlers for requests
            createHandlers();

            // Start the HTTP server
            startHttpServer();

            // Schedule the cleanup job
            startCleanupJob();

            // Inform that everything is fine
            this.driverStatus = SystemStatus.NOMINAL;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
        } catch (Exception e) {
            this.driverStatus = SystemStatus.ALARM;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
            throw new DriverException(e);
        }
    }

    private void startCleanupJob() {
        this.cleanupTimer = new Timer("ReatMetric HTTP Driver - Subscription cleanup job", true);
        this.cleanupJob = new TimerTask() {
            @Override
            public void run() {
                for(AbstractHttpRequestHandler h : new HashSet<>(context2handlers.values())) {
                    try {
                        h.cleanup();
                    } catch (Exception e) {
                        // Ignore and go ahead
                    }
                }
            }
        };
        this.cleanupTimer.schedule(this.cleanupJob, AbstractHttpRequestHandler.SUBSCRIPTION_EXPIRATION_TIME, AbstractHttpRequestHandler.SUBSCRIPTION_EXPIRATION_TIME);
    }

    private void createHandlers() {
        EventRequestHandler eh = new EventRequestHandler(this);
        context2handlers.put("/" + this.context.getSystemName() + "/" + EVENTS_PATH + "/" + REGISTRATION_URL, eh);
        context2handlers.put("/" + this.context.getSystemName() + "/" + EVENTS_PATH + "/" + LIST_URL, eh);

        ParameterRequestHandler ph = new ParameterRequestHandler(this);
        context2handlers.put("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + REGISTRATION_URL, ph);
        context2handlers.put("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + REGISTRATION_URL, ph);
        context2handlers.put("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + LIST_URL, ph);

        MessageRequestHandler mh = new MessageRequestHandler(this);
        context2handlers.put("/" + this.context.getSystemName() + "/" + MESSAGES_PATH + "/" + REGISTRATION_URL, mh);

        ModelRequestHandler moh = new ModelRequestHandler(this);
        context2handlers.put("/" + this.context.getSystemName() + "/" + MODEL_PATH, moh);

        ConnectorRequestHandler ch = new ConnectorRequestHandler(this);
        context2handlers.put("/" + this.context.getSystemName() + "/" + CONNECTORS_PATH, ch);
    }

    private void buildCache() throws ReatmetricException, RemoteException {
        // Get the root node
        SystemEntity root = context.getServiceFactory().getSystemModelMonitorService().getRoot();
        Queue<SystemEntity> constructionQueue = new LinkedList<>();
        constructionQueue.add(root);
        while (!constructionQueue.isEmpty()) {
            SystemEntity toProcess = constructionQueue.poll();
            if (toProcess.getType() == SystemEntityType.CONTAINER) {
                List<SystemEntity> children = context.getServiceFactory().getSystemModelMonitorService().getContainedEntities(toProcess.getPath());
                if (children != null) {
                    constructionQueue.addAll(children);
                }
            } else if (toProcess.getType() == SystemEntityType.EVENT) {
                this.events.add((EventDescriptor) context.getServiceFactory().getSystemModelMonitorService().getDescriptorOf(toProcess.getExternalId()));
            } else if (toProcess.getType() == SystemEntityType.PARAMETER) {
                this.parameters.add((ParameterDescriptor) context.getServiceFactory().getSystemModelMonitorService().getDescriptorOf(toProcess.getExternalId()));
            } else if (toProcess.getType() == SystemEntityType.ACTIVITY) {
                this.activities.add((ActivityDescriptor) context.getServiceFactory().getSystemModelMonitorService().getDescriptorOf(toProcess.getExternalId()));
            }
        }
    }

    private void startHttpServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(this.configuration.getHost(), this.configuration.getPort());
        this.server = HttpServer.create(address, 10);
        this.server.setExecutor(null);
        this.server.start();
        // Add the context
        for(Map.Entry<String, AbstractHttpRequestHandler> e : this.context2handlers.entrySet()) {
            this.server.createContext(e.getKey(), e.getValue());
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
        return Collections.emptyList();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        // Stop the cleanup task
        if(this.cleanupJob != null) {
            this.cleanupJob.cancel();
            this.cleanupJob = null;
        }
        if(this.cleanupTimer != null) {
            this.cleanupTimer.cancel();
            this.cleanupTimer = null;
        }
        // Stop the HTTP server
        this.server.stop(1);
        this.server = null;
        // Dispose the handlers
        for(AbstractHttpRequestHandler h : new HashSet<>(context2handlers.values())) {
            h.dispose();
        }
        context2handlers.clear();
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Collections.emptyList();
    }

    // --------------------------------------------------------------------
    // Internal/driver methods
    // --------------------------------------------------------------------

    public HttpServer getServer() {
        return server;
    }

    public IServiceCoreContext getContext() {
        return context;
    }

    public String getSystemName() {
        return this.context.getSystemName();
    }

    public List<ParameterDescriptor> getParameterList() {
        return Collections.unmodifiableList(this.parameters);
    }

    public List<EventDescriptor> getEventList () {
        return Collections.unmodifiableList(this.events);
    }

    public List<ActivityDescriptor> getActivityList() {
        return Collections.unmodifiableList(activities);
    }
}
