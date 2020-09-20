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

package eu.dariolucia.reatmetric.remoting;

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

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Class enabling remoting of a {@link IReatmetricSystem} via Java RMI.
 */
public class ReatmetricSystemRemoting implements IReatmetricSystem {

    private static final Logger LOG = Logger.getLogger(ReatmetricSystemRemoting.class.getName());

    private final IReatmetricSystem system;
    private final int port;
    private final String name;

    private Registry registry;
    private IReatmetricSystem activatedObject;

    private IOperationalMessageProvisionService remoteOperationalMessageProvisionService;
    private IAcknowledgedMessageProvisionService remoteAcknowledgedMessageProvisionService;
    private IAcknowledgementService remoteAcknowledgementService;
    private IRawDataProvisionService remoteRawDataProvisionService;
    private IParameterDataProvisionService remoteParameterDataProvisionService;
    private ISystemModelProvisionService remoteSystemModelProvisionService;
    private IEventDataProvisionService remoteEventDataProvisionService;
    private IAlarmParameterDataProvisionService remoteAlarmParameterDataProvisionService;
    private IActivityOccurrenceDataProvisionService remoteActivityOccurrenceDataProvisionService;
    private IActivityExecutionService remoteActivityExecutionService;
    private IScheduler remoteScheduler;
    private IScheduledActivityDataProvisionService remoteScheduledActivityDataProvisionService;
    private List<ITransportConnector> remoteTransportConnectorList;

    public ReatmetricSystemRemoting(int port, String name, IReatmetricSystem system) {
        this.port = port;
        this.name = name;
        this.system = system;
    }

    public ReatmetricSystemRemoting(Registry registry, String name, IReatmetricSystem system) {
        this.port = 0;
        this.name = name;
        this.system = system;
        this.registry = registry;
    }

    public synchronized void activate() throws RemoteException, AlreadyBoundException {
        if (activatedObject != null) {
            throw new IllegalStateException("Object already activated");
        }
        LOG.info("Activating ReatMetric Remoting on port " + this.port + " with name " + this.name);
        if (this.registry == null) {
            if (this.port == 0) {
                throw new IllegalStateException("Port not specified, cannot create registry");
            }
            this.registry = LocateRegistry.createRegistry(this.port);
        }
        this.activatedObject = (IReatmetricSystem) UnicastRemoteObject.exportObject(this, 0);
        this.registry.bind(this.name, this.activatedObject);
    }

    public synchronized void deactivate() throws RemoteException, NotBoundException {
        if (activatedObject == null) {
            throw new IllegalStateException("Object not bound yet");
        }
        LOG.info("Deactivating ReatMetric Remoting on port " + this.port + " with name " + this.name);
        try {
            this.registry.unbind(this.name);
            UnicastRemoteObject.unexportObject(this.activatedObject, true);
        } finally {
            this.activatedObject = null;
        }
        // Deactivate remoted objects
        if (remoteOperationalMessageProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteOperationalMessageProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteOperationalMessageProvisionService = null;

        if (remoteAcknowledgedMessageProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteAcknowledgedMessageProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteAcknowledgedMessageProvisionService = null;

        if (remoteAcknowledgementService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteAcknowledgementService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteAcknowledgementService = null;

        if (remoteRawDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteRawDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteRawDataProvisionService = null;

        if (remoteParameterDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteParameterDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteParameterDataProvisionService = null;

        if (remoteSystemModelProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteSystemModelProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteSystemModelProvisionService = null;

        if (remoteEventDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteEventDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteEventDataProvisionService = null;

        if (remoteAlarmParameterDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteAlarmParameterDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteAlarmParameterDataProvisionService = null;

        if (remoteActivityOccurrenceDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteActivityOccurrenceDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteActivityOccurrenceDataProvisionService = null;

        if (remoteActivityExecutionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteActivityExecutionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteActivityExecutionService = null;

        if (remoteScheduler != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteScheduler, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteScheduler = null;

        if (remoteScheduledActivityDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(remoteScheduledActivityDataProvisionService, true);
            } catch (NoSuchObjectException e) {
                // Ignore
            }
        }
        remoteScheduledActivityDataProvisionService = null;

        if (remoteTransportConnectorList != null) {
            for (ITransportConnector tc : remoteTransportConnectorList) {
                try {
                    UnicastRemoteObject.unexportObject(tc, true);
                } catch (NoSuchObjectException e) {
                    // Ignore
                }
            }
        }
        remoteTransportConnectorList = null;
    }

    public IReatmetricSystem getSystem() {
        return system;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public synchronized boolean isActive() {
        return this.activatedObject != null && this.registry != null;
    }

    @Override
    public void initialise(Consumer<SystemStatus> statusUpdateSubscriber) throws ReatmetricException {
        throw new ReatmetricException("Not allowed to be called from remote");
    }

    @Override
    public SystemStatus getStatus() throws RemoteException {
        return system.getStatus();
    }

    @Override
    public void dispose() throws ReatmetricException {
        throw new ReatmetricException("Not allowed to be called from remote");
    }

    @Override
    public synchronized IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException {
        if (remoteOperationalMessageProvisionService == null) {
            remoteOperationalMessageProvisionService = (IOperationalMessageProvisionService) UnicastRemoteObject.exportObject(system.getOperationalMessageMonitorService(), 0);
        }
        return remoteOperationalMessageProvisionService;
    }

    @Override
    public synchronized IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException {
        if (remoteAcknowledgedMessageProvisionService == null) {
            remoteAcknowledgedMessageProvisionService = (IAcknowledgedMessageProvisionService) UnicastRemoteObject.exportObject(system.getAcknowledgedMessageMonitorService(), 0);
        }
        return remoteAcknowledgedMessageProvisionService;
    }

    @Override
    public synchronized IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException {
        if (remoteAcknowledgementService == null) {
            remoteAcknowledgementService = (IAcknowledgementService) UnicastRemoteObject.exportObject(system.getAcknowledgementService(), 0);
        }
        return remoteAcknowledgementService;
    }

    @Override
    public synchronized IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteRawDataProvisionService == null) {
            remoteRawDataProvisionService = (IRawDataProvisionService) UnicastRemoteObject.exportObject(system.getRawDataMonitorService(), 0);
        }
        return remoteRawDataProvisionService;
    }

    @Override
    public synchronized IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteParameterDataProvisionService == null) {
            remoteParameterDataProvisionService = (IParameterDataProvisionService) UnicastRemoteObject.exportObject(system.getParameterDataMonitorService(), 0);
        }
        return remoteParameterDataProvisionService;
    }

    @Override
    public synchronized ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException {
        if (remoteSystemModelProvisionService == null) {
            remoteSystemModelProvisionService = (ISystemModelProvisionService) UnicastRemoteObject.exportObject(system.getSystemModelMonitorService(), 0);
        }
        return remoteSystemModelProvisionService;
    }

    @Override
    public synchronized IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteEventDataProvisionService == null) {
            remoteEventDataProvisionService = (IEventDataProvisionService) UnicastRemoteObject.exportObject(system.getEventDataMonitorService(), 0);
        }
        return remoteEventDataProvisionService;
    }

    @Override
    public synchronized IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteAlarmParameterDataProvisionService == null) {
            remoteAlarmParameterDataProvisionService = (IAlarmParameterDataProvisionService) UnicastRemoteObject.exportObject(system.getAlarmParameterDataMonitorService(), 0);
        }
        return remoteAlarmParameterDataProvisionService;
    }

    @Override
    public synchronized IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteActivityOccurrenceDataProvisionService == null) {
            remoteActivityOccurrenceDataProvisionService = (IActivityOccurrenceDataProvisionService) UnicastRemoteObject.exportObject(system.getActivityOccurrenceDataMonitorService(), 0);
        }
        return remoteActivityOccurrenceDataProvisionService;
    }

    @Override
    public synchronized IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException {
        if (remoteActivityExecutionService == null) {
            remoteActivityExecutionService = (IActivityExecutionService) UnicastRemoteObject.exportObject(system.getActivityExecutionService(), 0);
        }
        return remoteActivityExecutionService;
    }

    @Override
    public synchronized IScheduler getScheduler() throws ReatmetricException, RemoteException {
        if (remoteScheduler == null) {
            remoteScheduler = (IScheduler) UnicastRemoteObject.exportObject(system.getScheduler(), 0);
            // The same object can be used as IScheduledActivityDataProvisionService
            remoteScheduledActivityDataProvisionService = remoteScheduler;
        }
        return remoteScheduler;
    }

    @Override
    public synchronized IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteScheduledActivityDataProvisionService == null) {
            remoteScheduledActivityDataProvisionService = (IScheduledActivityDataProvisionService) UnicastRemoteObject.exportObject(system.getScheduledActivityDataMonitorService(), 0);
            // Check if the same object can be used as IScheduler
            if (remoteScheduledActivityDataProvisionService instanceof IScheduler) {
                remoteScheduler = (IScheduler) remoteScheduledActivityDataProvisionService;
            }
        }
        return remoteScheduledActivityDataProvisionService;
    }

    @Override
    public synchronized List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException {
        if (remoteTransportConnectorList == null) {
            remoteTransportConnectorList = new ArrayList<>();
            for (ITransportConnector tc : system.getTransportConnectors()) {
                ITransportConnector remoted = (ITransportConnector) UnicastRemoteObject.exportObject(tc, 0);
                remoteTransportConnectorList.add(remoted);
            }
        }
        return remoteTransportConnectorList;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException {
        return system.currentDebugInfo();
    }
}
