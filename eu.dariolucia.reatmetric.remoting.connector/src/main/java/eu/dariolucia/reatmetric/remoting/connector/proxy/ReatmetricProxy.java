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

package eu.dariolucia.reatmetric.remoting.connector.proxy;

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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageCollectorService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ReatmetricProxy implements IReatmetricSystem {

    private IReatmetricSystem delegate;
    private final ConnectorConfiguration connector;


    public ReatmetricProxy(ConnectorConfiguration cc) {
        this.connector = cc;
    }

    // local cache
    private OperationalMessageProvisionServiceProxy operationalMessageProvisionServiceProxy;
    private OperationalMessageCollectorServiceProxy operationalMessageCollectorServiceProxy;
    private AcknowledgedMessageProvisionServiceProxy acknowledgedMessageProvisionServiceProxy;
    private AcknowledgementServiceProxy acknowledgementServiceProxy;
    private RawDataProvisionServiceProxy rawDataProvisionServiceProxy;
    private ParameterDataProvisionServiceProxy parameterDataProvisionServiceProxy;
    private SystemModelProvisionServiceProxy systemModelProvisionServiceProxy;
    private EventDataProvisionServiceProxy eventDataProvisionServiceProxy;
    private AlarmParameterDataProvisionServiceProxy alarmParameterDataProvisionServiceProxy;
    private ActivityOccurrenceDataProvisionServiceProxy activityOccurrenceDataProvisionServiceProxy;
    private ActivityExecutionServiceProxy activityExecutionServiceProxy;
    private SchedulerProxy schedulerProxy;
    private ScheduledActivityDataProvisionServiceProxy scheduledActivityDataProvisionServiceProxy;
    private List<TransportConnectorProxy> transportConnectorProxyList;

    @Override
    public void initialise(Consumer<SystemStatus> statusUpdateSubscriber) throws ReatmetricException, RemoteException {
        // Perform look up, assume system is already initialised
        Registry reg = LocateRegistry.getRegistry(connector.getHost(), connector.getPort());
        try {
            this.delegate = (IReatmetricSystem) reg.lookup(connector.getName());
        } catch (NotBoundException e) {
            throw new ReatmetricException(e);
        }
        SystemStatus status = this.delegate.getStatus();
        statusUpdateSubscriber.accept(status);
    }

    @Override
    public SystemStatus getStatus() throws RemoteException {
        return delegate.getStatus();
    }

    @Override
    public void dispose() {
        if(operationalMessageProvisionServiceProxy != null) {
            operationalMessageProvisionServiceProxy.terminate();
        }
        operationalMessageProvisionServiceProxy = null;

        operationalMessageCollectorServiceProxy = null;

        if(acknowledgedMessageProvisionServiceProxy != null) {
            acknowledgedMessageProvisionServiceProxy.terminate();
        }
        acknowledgedMessageProvisionServiceProxy = null;

        acknowledgementServiceProxy = null;

        if(rawDataProvisionServiceProxy != null) {
            rawDataProvisionServiceProxy.terminate();
        }
        rawDataProvisionServiceProxy = null;

        if(parameterDataProvisionServiceProxy != null) {
            parameterDataProvisionServiceProxy.terminate();
        }
        parameterDataProvisionServiceProxy = null;

        if(systemModelProvisionServiceProxy != null) {
            systemModelProvisionServiceProxy.terminate();
        }
        systemModelProvisionServiceProxy = null;

        if(alarmParameterDataProvisionServiceProxy != null) {
            alarmParameterDataProvisionServiceProxy.terminate();
        }
        alarmParameterDataProvisionServiceProxy = null;

        if(activityOccurrenceDataProvisionServiceProxy != null) {
            activityOccurrenceDataProvisionServiceProxy.terminate();
        }
        activityOccurrenceDataProvisionServiceProxy = null;

        activityExecutionServiceProxy = null;

        if(schedulerProxy != null) {
            schedulerProxy.terminate();
        }
        schedulerProxy = null;

        if(scheduledActivityDataProvisionServiceProxy != null) {
            scheduledActivityDataProvisionServiceProxy.terminate();
        }
        scheduledActivityDataProvisionServiceProxy = null;

        if(transportConnectorProxyList != null) {
            transportConnectorProxyList.forEach(TransportConnectorProxy::terminate);
        }
        transportConnectorProxyList = null;
    }

    @Override
    public String getName() {
        // Never retrieved from the remote system, use the configuration name, as it must match the remote system name
        return connector.getName();
    }

    @Override
    public synchronized IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException {
        if(operationalMessageProvisionServiceProxy == null) {
            operationalMessageProvisionServiceProxy = new OperationalMessageProvisionServiceProxy(delegate.getOperationalMessageMonitorService());
        }
        return operationalMessageProvisionServiceProxy;
    }

    @Override
    public synchronized IOperationalMessageCollectorService getOperationalMessageCollectorService() throws ReatmetricException, RemoteException {
        if(operationalMessageCollectorServiceProxy == null) {
            operationalMessageCollectorServiceProxy = new OperationalMessageCollectorServiceProxy(delegate.getOperationalMessageCollectorService());
        }
        return operationalMessageCollectorServiceProxy;
    }

    @Override
    public synchronized IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException {
        if(acknowledgedMessageProvisionServiceProxy == null) {
            acknowledgedMessageProvisionServiceProxy = new AcknowledgedMessageProvisionServiceProxy(delegate.getAcknowledgedMessageMonitorService());
        }
        return acknowledgedMessageProvisionServiceProxy;
    }

    @Override
    public synchronized IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException {
        if(acknowledgementServiceProxy == null) {
            acknowledgementServiceProxy = new AcknowledgementServiceProxy(delegate.getAcknowledgementService());
        }
        return acknowledgementServiceProxy;
    }

    @Override
    public synchronized IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException {
        if(rawDataProvisionServiceProxy == null) {
            rawDataProvisionServiceProxy = new RawDataProvisionServiceProxy(delegate.getRawDataMonitorService());
        }
        return rawDataProvisionServiceProxy;
    }

    @Override
    public synchronized IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if(parameterDataProvisionServiceProxy == null) {
            parameterDataProvisionServiceProxy = new ParameterDataProvisionServiceProxy(delegate.getParameterDataMonitorService());
        }
        return parameterDataProvisionServiceProxy;
    }

    @Override
    public synchronized ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException {
        if(systemModelProvisionServiceProxy == null) {
            systemModelProvisionServiceProxy = new SystemModelProvisionServiceProxy(delegate.getSystemModelMonitorService());
        }
        return systemModelProvisionServiceProxy;
    }

    @Override
    public synchronized IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException {
        if(eventDataProvisionServiceProxy == null) {
            eventDataProvisionServiceProxy = new EventDataProvisionServiceProxy(delegate.getEventDataMonitorService());
        }
        return eventDataProvisionServiceProxy;
    }

    @Override
    public synchronized IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if(alarmParameterDataProvisionServiceProxy == null) {
            alarmParameterDataProvisionServiceProxy = new AlarmParameterDataProvisionServiceProxy(delegate.getAlarmParameterDataMonitorService());
        }
        return alarmParameterDataProvisionServiceProxy;
    }

    @Override
    public synchronized IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException {
        if(activityOccurrenceDataProvisionServiceProxy == null) {
            activityOccurrenceDataProvisionServiceProxy = new ActivityOccurrenceDataProvisionServiceProxy(delegate.getActivityOccurrenceDataMonitorService());
        }
        return activityOccurrenceDataProvisionServiceProxy;
    }

    @Override
    public synchronized IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException {
        if(activityExecutionServiceProxy == null) {
            activityExecutionServiceProxy = new ActivityExecutionServiceProxy(delegate.getActivityExecutionService());
        }
        return activityExecutionServiceProxy;
    }

    @Override
    public synchronized IScheduler getScheduler() throws ReatmetricException, RemoteException {
        if(schedulerProxy == null) {
            schedulerProxy = new SchedulerProxy(delegate.getScheduler());
        }
        return schedulerProxy;
    }

    @Override
    public synchronized IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException {
        if(scheduledActivityDataProvisionServiceProxy == null) {
            scheduledActivityDataProvisionServiceProxy = new ScheduledActivityDataProvisionServiceProxy(delegate.getScheduledActivityDataMonitorService());
        }
        return scheduledActivityDataProvisionServiceProxy;
    }

    @Override
    public synchronized List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException {
        if(transportConnectorProxyList == null) {
            List<ITransportConnector> theConnectors = delegate.getTransportConnectors();
            transportConnectorProxyList = new ArrayList<>(theConnectors.size());
            for(ITransportConnector t : theConnectors) {
                transportConnectorProxyList.add(new TransportConnectorProxy(t));
            }
        }
        return transportConnectorProxyList.stream().map(o -> (ITransportConnector) o).collect(Collectors.toList());
    }

    @Override
    public List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException {
        return delegate.currentDebugInfo();
    }
}
