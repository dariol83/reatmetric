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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageCollectorService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.scheduler.IScheduledActivityDataProvisionService;
import eu.dariolucia.reatmetric.api.scheduler.IScheduler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.remoting.stubs.TransportConnectorDelegate;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<Object, Remote> exportedObjects = new ConcurrentHashMap<>();

    private Registry registry;
    private IReatmetricSystem activatedObject;

    private IOperationalMessageProvisionService remoteOperationalMessageProvisionService;
    private IOperationalMessageCollectorService remoteOperationalMessageCollectorService;
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

        if (this.registry == null) {
            if (this.port == 0) {
                throw new IllegalStateException("Port not specified, cannot create registry");
            }
            LOG.info("Activating ReatMetric Remoting on port " + this.port + " with name " + this.name);
            this.registry = LocateRegistry.createRegistry(this.port);
        } else {
            LOG.info("Activating ReatMetric Remoting on registry " + registry + " with name " + this.name);
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
            UnicastRemoteObject.unexportObject(this, true);
        } finally {
            this.activatedObject = null;
        }
        // Deactivate remoted objects
        if (remoteOperationalMessageProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getOperationalMessageMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteOperationalMessageProvisionService = null;

        if (remoteOperationalMessageCollectorService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getOperationalMessageCollectorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteOperationalMessageCollectorService = null;

        if (remoteAcknowledgedMessageProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getAcknowledgedMessageMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteAcknowledgedMessageProvisionService = null;

        if (remoteAcknowledgementService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getAcknowledgementService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteAcknowledgementService = null;

        if (remoteRawDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getRawDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteRawDataProvisionService = null;

        if (remoteParameterDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getParameterDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteParameterDataProvisionService = null;

        if (remoteSystemModelProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getSystemModelMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteSystemModelProvisionService = null;

        if (remoteEventDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getEventDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteEventDataProvisionService = null;

        if (remoteAlarmParameterDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getAlarmParameterDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteAlarmParameterDataProvisionService = null;

        if (remoteActivityOccurrenceDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getActivityOccurrenceDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteActivityOccurrenceDataProvisionService = null;

        if (remoteActivityExecutionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getActivityExecutionService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteActivityExecutionService = null;

        if (remoteScheduler != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getScheduler(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteScheduler = null;

        if (remoteScheduledActivityDataProvisionService != null) {
            try {
                UnicastRemoteObject.unexportObject(system.getScheduledActivityDataMonitorService(), true);
            } catch (NoSuchObjectException | ReatmetricException e) {
                // Ignore
            }
        }
        remoteScheduledActivityDataProvisionService = null;

        if (remoteTransportConnectorList != null) {
            try {
                for (ITransportConnector tc : system.getTransportConnectors()) {
                    try {
                        UnicastRemoteObject.unexportObject(tc, true);
                    } catch (NoSuchObjectException e) {
                        // Ignore
                    }
                }
            } catch (ReatmetricException e) {
                // Ignore
            }
        }
        remoteTransportConnectorList = null;

        exportedObjects.clear();
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

    protected <T extends Remote> Remote exportObject(T instance) throws RemoteException {
        Remote remote = exportedObjects.get(instance);
        if (remote == null) {
            remote = UnicastRemoteObject.exportObject(instance, 0);
            exportedObjects.put(instance, remote);
        }
        return remote;
    }

    @Override
    public synchronized IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException, RemoteException {
        if (remoteOperationalMessageProvisionService == null) {
            remoteOperationalMessageProvisionService = (IOperationalMessageProvisionService) exportObject(system.getOperationalMessageMonitorService());
        }
        return remoteOperationalMessageProvisionService;
    }

    @Override
    public synchronized IOperationalMessageCollectorService getOperationalMessageCollectorService() throws ReatmetricException, RemoteException {
        if (remoteOperationalMessageCollectorService == null) {
            remoteOperationalMessageCollectorService = (IOperationalMessageCollectorService) exportObject(system.getOperationalMessageCollectorService());
        }
        return remoteOperationalMessageCollectorService;
    }

    @Override
    public synchronized IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() throws ReatmetricException, RemoteException {
        if (remoteAcknowledgedMessageProvisionService == null) {
            remoteAcknowledgedMessageProvisionService = (IAcknowledgedMessageProvisionService) exportObject(system.getAcknowledgedMessageMonitorService());
        }
        return remoteAcknowledgedMessageProvisionService;
    }

    @Override
    public synchronized IAcknowledgementService getAcknowledgementService() throws ReatmetricException, RemoteException {
        if (remoteAcknowledgementService == null) {
            remoteAcknowledgementService = (IAcknowledgementService) exportObject(system.getAcknowledgementService());
        }
        return remoteAcknowledgementService;
    }

    @Override
    public synchronized IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteRawDataProvisionService == null) {
            remoteRawDataProvisionService = (IRawDataProvisionService) exportObject(system.getRawDataMonitorService());
        }
        return remoteRawDataProvisionService;
    }

    @Override
    public synchronized IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteParameterDataProvisionService == null) {
            remoteParameterDataProvisionService = (IParameterDataProvisionService) exportObject(system.getParameterDataMonitorService());
        }
        return remoteParameterDataProvisionService;
    }

    @Override
    public synchronized ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException, RemoteException {
        if (remoteSystemModelProvisionService == null) {
            remoteSystemModelProvisionService = (ISystemModelProvisionService) exportObject(system.getSystemModelMonitorService());
        }
        return remoteSystemModelProvisionService;
    }

    @Override
    public synchronized IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteEventDataProvisionService == null) {
            remoteEventDataProvisionService = (IEventDataProvisionService) exportObject(system.getEventDataMonitorService());
        }
        return remoteEventDataProvisionService;
    }

    @Override
    public synchronized IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteAlarmParameterDataProvisionService == null) {
            remoteAlarmParameterDataProvisionService = (IAlarmParameterDataProvisionService) exportObject(system.getAlarmParameterDataMonitorService());
        }
        return remoteAlarmParameterDataProvisionService;
    }

    @Override
    public synchronized IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteActivityOccurrenceDataProvisionService == null) {
            remoteActivityOccurrenceDataProvisionService = (IActivityOccurrenceDataProvisionService) exportObject(system.getActivityOccurrenceDataMonitorService());
        }
        return remoteActivityOccurrenceDataProvisionService;
    }

    @Override
    public synchronized IActivityExecutionService getActivityExecutionService() throws ReatmetricException, RemoteException {
        if (remoteActivityExecutionService == null) {
            remoteActivityExecutionService = (IActivityExecutionService) exportObject(system.getActivityExecutionService());
        }
        return remoteActivityExecutionService;
    }

    @Override
    public synchronized IScheduler getScheduler() throws ReatmetricException, RemoteException {
        if (remoteScheduler == null) {
            remoteScheduler = (IScheduler) exportObject(system.getScheduler());
        }
        return remoteScheduler;
    }

    @Override
    public synchronized IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() throws ReatmetricException, RemoteException {
        if (remoteScheduledActivityDataProvisionService == null) {
            remoteScheduledActivityDataProvisionService = (IScheduledActivityDataProvisionService) exportObject(system.getScheduledActivityDataMonitorService());
        }
        return remoteScheduledActivityDataProvisionService;
    }

    @Override
    public synchronized List<ITransportConnector> getTransportConnectors() throws ReatmetricException, RemoteException {
        // Since the transport connectors are provided by the drivers, it could happen that one driver extends the connector
        // interface to provide additional capabilities. In this case, there is the risk of a exception if the RMI classloader
        // is disabled (the extended interface might not be present in the UI. To avoid this, for this specific class only,
        // a server delegate stub is used.
        if (remoteTransportConnectorList == null) {
            remoteTransportConnectorList = new ArrayList<>();
            for (ITransportConnector tc : system.getTransportConnectors()) {
                ITransportConnector remoted = (ITransportConnector) exportObject(new TransportConnectorDelegate(tc));
                remoteTransportConnectorList.add(remoted);
            }
        }
        return remoteTransportConnectorList;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() throws ReatmetricException, RemoteException {
        return system.currentDebugInfo();
    }

    public IReatmetricSystem getSystem() {
        return system;
    }
}
