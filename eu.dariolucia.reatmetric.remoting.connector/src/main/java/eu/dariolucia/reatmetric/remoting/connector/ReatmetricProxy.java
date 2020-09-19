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

package eu.dariolucia.reatmetric.remoting.connector;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IAcknowledgedMessageProvisionService;
import eu.dariolucia.reatmetric.api.messages.IAcknowledgementService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.scheduler.IScheduledActivityDataProvisionService;
import eu.dariolucia.reatmetric.api.scheduler.IScheduler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.remoting.connector.configuration.ConnectorConfiguration;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.function.Consumer;

public class ReatmetricProxy implements IReatmetricSystem {

    private IReatmetricSystem delegate;
    private final ConnectorConfiguration connector;

    public ReatmetricProxy(ConnectorConfiguration cc) {
        this.connector = cc;
    }

    @Override
    public void initialise(Consumer<SystemStatus> statusUpdateSubscriber) throws ReatmetricException, RemoteException {
        // Perform look up, assume system is already initialised
        Registry reg = LocateRegistry.getRegistry(connector.getHost(), connector.getPort());
        try {
            this.delegate = (IReatmetricSystem) reg.lookup(connector.getName());
        } catch (NotBoundException e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public SystemStatus getStatus() throws RemoteException {
        return delegate.getStatus();
    }

    @Override
    public void dispose() {
        // Nothing to be done
    }

    @Override
    public String getName() {
        return connector.getName();
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getOperationalMessageMonitorService();
    }

    @Override
    public IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getAcknowledgedMessageMonitorService();
    }

    @Override
    public IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException {
        return delegate.getAcknowledgementService();
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getRawDataMonitorService();
    }

    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getParameterDataMonitorService();
    }

    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getSystemModelMonitorService();
    }

    @Override
    public IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getEventDataMonitorService();
    }

    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getAlarmParameterDataMonitorService();
    }

    @Override
    public IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getActivityOccurrenceDataMonitorService();
    }

    @Override
    public IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException {
        return delegate.getActivityExecutionService();
    }

    @Override
    public IScheduler getScheduler() throws ReatmetricException, RemoteException {
        return delegate.getScheduler();
    }

    @Override
    public IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException {
        return delegate.getScheduledActivityDataMonitorService();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException {
        return delegate.getTransportConnectors();
    }

    @Override
    public List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException {
        return delegate.currentDebugInfo();
    }
}
