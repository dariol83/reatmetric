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


package eu.dariolucia.reatmetric.api;

import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IDebugInfoProvider;
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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main interface of a ReatMetric system implementation
 */
public interface IReatmetricSystem extends Remote {

    void initialise(Consumer<SystemStatus> statusUpdateSubscriber) throws ReatmetricException, RemoteException;

    SystemStatus getStatus() throws RemoteException;

    void dispose() throws ReatmetricException, RemoteException;

    String getName() throws RemoteException;
    
    IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException;

    IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException;

    IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException;

    IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException;
    
    IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException;
    
    ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException;
    
    IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException;
    
    IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException;

    IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException;

    IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException;

    IScheduler getScheduler() throws ReatmetricException, RemoteException;

    IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException;

    List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException;

    List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException;
}
