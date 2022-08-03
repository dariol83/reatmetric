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
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
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
import eu.dariolucia.reatmetric.driver.httpserver.protocol.HttpRequestHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    private HttpRequestHandler handler;
    // Model information cache
    private final List<ParameterDescriptor> parameters = new LinkedList<>();
    private final List<EventDescriptor> events = new LinkedList<>();

    public HttpServerDriver() {
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
        try {
            // Read the configuration
            this.configuration = HttpServerConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));

            // Build parameters/events cache
            buildCache();

            // Create a global handler for requests
            this.handler = new HttpRequestHandler(this);

            // Start the HTTP server
            startHttpServer();

            // Inform that everything is fine
            this.driverStatus = SystemStatus.NOMINAL;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
        } catch (Exception e) {
            this.driverStatus = SystemStatus.ALARM;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
            throw new DriverException(e);
        }
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
            }
        }
    }

    private void startHttpServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(this.configuration.getHost(), this.configuration.getPort());
        this.server = HttpServer.create(address, 10);
        this.server.setExecutor(null);
        this.server.start();
        // Add the context
        this.server.createContext("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + REGISTRATION_URL, this.handler);
        this.server.createContext("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + REGISTRATION_URL, this.handler);
        this.server.createContext("/" + this.context.getSystemName() + "/" + PARAMETERS_PATH + "/" + LIST_URL, this.handler);
        this.server.createContext("/" + this.context.getSystemName() + "/" + EVENTS_PATH + "/" + REGISTRATION_URL, this.handler);
        this.server.createContext("/" + this.context.getSystemName() + "/" + EVENTS_PATH + "/" + LIST_URL, this.handler);
        this.server.createContext("/" + this.context.getSystemName() + "/" + MESSAGES_PATH + "/" + REGISTRATION_URL, this.handler);
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
        // Stop the HTTP server
        this.server.stop(1);
        this.server = null;
        this.handler.dispose();
        this.handler = null;
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

    public void register(IParameterDataSubscriber sub, ParameterDataFilter filter) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getParameterDataMonitorService().subscribe(sub, filter);
    }

    public void deregister(IParameterDataSubscriber sub) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getParameterDataMonitorService().unsubscribe(sub);
    }

    public void register(IEventDataSubscriber sub, EventDataFilter filter) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getEventDataMonitorService().subscribe(sub, filter);
    }

    public void deregister(IEventDataSubscriber sub) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getEventDataMonitorService().unsubscribe(sub);
    }

    public void register(IOperationalMessageSubscriber sub, OperationalMessageFilter filter) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getOperationalMessageMonitorService().subscribe(sub, filter);
    }

    public void deregister(IOperationalMessageSubscriber sub) throws ReatmetricException, RemoteException {
        this.context.getServiceFactory().getOperationalMessageMonitorService().unsubscribe(sub);
    }

    public List<ParameterDescriptor> getParameterList() {
        return Collections.unmodifiableList(this.parameters);
    }

    public List<EventDescriptor> getEventList () {
        return Collections.unmodifiableList(this.events);
    }

    public String getSystemName() {
        return this.context.getSystemName();
    }
}
