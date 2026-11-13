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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageCollectorService;
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
 * Main interface of a ReatMetric system implementation.
 * This interface provides access to all the services and components of a ReatMetric system.
 */
public interface IReatmetricSystem extends Remote {

    /**
     * Initializes the ReatMetric system.
     * The provided subscriber will be notified of any status updates during the system lifecycle.
     *
     * @param statusUpdateSubscriber the consumer that will receive status updates, cannot be null
     * @throws ReatmetricException if a problem arises during initialization
     * @throws RemoteException in case of remoting problem
     */
    void initialise(Consumer<SystemStatus> statusUpdateSubscriber) throws ReatmetricException, RemoteException;

    /**
     * Returns the current status of the system.
     *
     * @return the current system status
     * @throws RemoteException in case of remoting problem
     */
    SystemStatus getStatus() throws RemoteException;

    /**
     * Disposes of the system resources and shuts down the system.
     * After this method is called, the system should not be used anymore.
     *
     * @throws ReatmetricException if a problem arises during disposal
     * @throws RemoteException in case of remoting problem
     */
    void dispose() throws ReatmetricException, RemoteException;

    /**
     * Returns the name of the system.
     *
     * @return the system name
     * @throws RemoteException in case of remoting problem
     */
    String getName() throws RemoteException;
    
    /**
     * Returns the operational message provision service for monitoring operational messages.
     *
     * @return the operational message provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the operational message collector service for collecting operational messages.
     *
     * @return the operational message collector service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IOperationalMessageCollectorService getOperationalMessageCollectorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the acknowledged message provision service for monitoring acknowledged messages.
     *
     * @return the acknowledged message provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the acknowledgement service for acknowledging messages.
     *
     * @return the acknowledgement service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException;

    /**
     * Returns the raw data provision service for monitoring raw data.
     *
     * @return the raw data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException;
    
    /**
     * Returns the parameter data provision service for monitoring parameter data.
     *
     * @return the parameter data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException;
    
    /**
     * Returns the system model provision service for monitoring system model updates.
     *
     * @return the system model provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException;
    
    /**
     * Returns the event data provision service for monitoring event data.
     *
     * @return the event data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException;
    
    /**
     * Returns the alarm parameter data provision service for monitoring alarm parameter data.
     *
     * @return the alarm parameter data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the activity occurrence data provision service for monitoring activity occurrence data.
     *
     * @return the activity occurrence data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the activity execution service for executing activities.
     *
     * @return the activity execution service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException;

    /**
     * Returns the scheduler service for scheduling activities.
     *
     * @return the scheduler service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IScheduler getScheduler() throws ReatmetricException, RemoteException;

    /**
     * Returns the scheduled activity data provision service for monitoring scheduled activity data.
     *
     * @return the scheduled activity data provision service
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException;

    /**
     * Returns the list of transport connectors available in the system.
     *
     * @return the list of transport connectors
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException;

    /**
     * Returns the current debug information from all components in the system.
     *
     * @return the list of debug information objects
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException;
}
