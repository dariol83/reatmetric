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

package eu.dariolucia.reatmetric.driver.snmp;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpDevice;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnmpActivityHandler implements IActivityHandler {

    private final List<String> supportedRoutes;
    private final List<String> supportedActivityTypes;
    private final SnmpConfiguration configuration;
    private final Map<String, SnmpTransportConnector> connectors;

    public SnmpActivityHandler(SnmpConfiguration configuration, Map<String, SnmpTransportConnector> connectors) {
        this.configuration = configuration;
        this.connectors = connectors;
        // Collect the list of routes
        this.supportedRoutes = configuration.getSnmpDeviceList().stream().map(SnmpDevice::getName).collect(Collectors.toList());
        // Activity type
        this.supportedActivityTypes = new LinkedList<>(Collections.singletonList(SnmpDriver.SNMP_MESSAGE_TYPE));
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
        // Get the right connector and simply forward this information to the connector
        String route = activityInvocation.getRoute();
        SnmpTransportConnector connector = this.connectors.get(route);
        if(connector == null) {
            throw new ActivityHandlingException("SNMP connector for route " + route + " on driver " + configuration.getName() + " not present");
        } else {
            connector.executeActivity(activityInvocation);
        }
    }

    @Override
    public boolean getRouteAvailability(String route) throws ActivityHandlingException {
        SnmpTransportConnector connector = this.connectors.get(route);
        if(connector == null) {
            return false;
        } else {
            // The route is available if the connector is started
            return connector.getConnectionStatus() == TransportConnectionStatus.OPEN;
            // You can try to send
        }
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // Not supported for this driver
        throw new ActivityHandlingException("Operation not supported");
    }
}
