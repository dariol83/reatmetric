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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.driver.socket.configuration.SocketConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.InitType;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketDriverConnector extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(SocketDriverConnector.class.getName());

    private final SocketConfiguration configuration;
    public SocketDriverConnector(SocketConfiguration configuration) {
        super(configuration.getName(), configuration.getDescription());
        this.configuration = configuration;
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        return null;
    }

    @Override
    protected void doConnect() {
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        for(AbstractConnectionConfiguration con : configuration.getConnections()) {
            if(con.getInit() == InitType.CONNECTOR) {
                con.openConnection();
            }
        }
        // Check
        updateConnectionStatus(TransportConnectionStatus.OPEN);
        // TODO: introduce a way to report a reasonable/real status, i.e. callback to indicate that a connection is active/not active
        updateAlarmState(AlarmState.NOMINAL);
        // TODO: activate auto-sending of configured commands for connector-driven routes
    }

    @Override
    protected void doDisconnect() {
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        for(AbstractConnectionConfiguration con : configuration.getConnections()) {
            if(con.getInit() == InitType.CONNECTOR) {
                con.closeConnection();
            }
        }
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOMINAL);
        // TODO: stop auto-sending of configured commands for connector-driven routes
    }

    @Override
    protected void doDispose() {
        // Nothing specific to do
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        if(getConnectionStatus() != TransportConnectionStatus.OPEN) {
            throw new ActivityHandlingException("Connector " + getName() + " not started");
        }
        // Get the route
        String route = activityInvocation.getRoute();
        // Retrieve the connection linked to this route
        AbstractConnectionConfiguration connection = null;
        for(AbstractConnectionConfiguration con : configuration.getConnections()) {
            if(con.getName().equals(route)) {
                connection = con;
                break;
            }
        }
        if(connection == null) {
            // No connection, error
            throw new ActivityHandlingException("No route " + route + " found on connector " + getName());
        }
        // OK, forward
        connection.getRoute().dispatchActivity(activityInvocation);
    }
}
