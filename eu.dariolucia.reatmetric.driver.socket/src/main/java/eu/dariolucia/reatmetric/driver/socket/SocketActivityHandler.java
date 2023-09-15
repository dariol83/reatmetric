/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.socket;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.driver.socket.configuration.SocketConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.InitType;
import eu.dariolucia.reatmetric.driver.socket.configuration.protocol.RouteConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class SocketActivityHandler implements IActivityHandler {

    private final SocketConfiguration configuration;
    private final SocketDriverConnector connector;
    private final List<String> supportedRoutes;
    private final List<String> supportedActivityTypes;

    public SocketActivityHandler(SocketConfiguration configuration, SocketDriverConnector connector) {
        this.configuration = configuration;
        this.connector = connector;
        // Collect the list of routes
        this.supportedRoutes = configuration.getConnections().stream().map(AbstractConnectionConfiguration::getRoute).map(RouteConfiguration::getName).collect(Collectors.toList());
        // Collect the list of supported activity types (on all routes)
        Set<String> temp = new LinkedHashSet<>();
        configuration.getConnections().stream().map(AbstractConnectionConfiguration::getRoute).forEach(r -> temp.addAll(r.getActivityTypes()));
        this.supportedActivityTypes = new LinkedList<>(temp);
    }

    @Override
    public void registerModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public List<String> getSupportedRoutes() {
        return Collections.unmodifiableList(this.supportedRoutes);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.unmodifiableList(this.supportedActivityTypes);
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        // Simply forward this information to the connector
        this.connector.executeActivity(activityInvocation);
    }

    @Override
    public boolean getRouteAvailability(String route) throws ActivityHandlingException {
        // The route is available if the connector is started and the specific connection is:
        // - NOT connector-driven or
        // - connector-driven and open
        if(this.connector.getConnectionStatus() != TransportConnectionStatus.OPEN) {
            return false;
        }
        Optional<AbstractConnectionConfiguration> connection = configuration.getConnections().stream().filter(c -> c.getRoute().getName().equals(route)).findFirst();
        if(connection.isEmpty()) {
            // No such route
            return false;
        }
        if(connection.get().getInit() == InitType.CONNECTOR && !connection.get().isOpen()) {
            // Connector-driven route, but not open
            return false;
        }
        // You can try to send
        return true;
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // Not supported for this driver
        throw new ActivityHandlingException("Operation not supported");
    }
}
