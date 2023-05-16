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

package eu.dariolucia.reatmetric.core;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.*;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.scheduler.IScheduledActivityDataProvisionService;
import eu.dariolucia.reatmetric.api.scheduler.IScheduler;
import eu.dariolucia.reatmetric.api.scheduler.ISchedulerFactory;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.*;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.DriverConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.core.impl.OperationalMessageBrokerImpl;
import eu.dariolucia.reatmetric.core.impl.ProcessingModelManager;
import eu.dariolucia.reatmetric.core.impl.RawDataBrokerImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Reference ReatMetric system implementation.
 */
public class ReatmetricSystemImpl implements IReatmetricSystem, IServiceCoreContext, IDriverListener, ITransportSubscriber {

    private static final Logger LOG = Logger.getLogger(ReatmetricSystemImpl.class.getName());

    private final List<IDriver> drivers = new ArrayList<>();
    private final List<ITransportConnector> transportConnectors = new ArrayList<>();
    private final List<ITransportConnector> transportConnectorsImmutable = Collections.unmodifiableList(transportConnectors);

    private final Map<Pair<String, String>, IRawDataRenderer> renderers = new HashMap<>();
    private final ServiceCoreConfiguration configuration;

    private volatile SystemStatus systemStatus = SystemStatus.NOT_INITED;

    private IArchive archive;
    private IScheduler scheduler;
    private OperationalMessageBrokerImpl messageBroker;
    private RawDataBrokerImpl rawDataBroker;
    private ProcessingModelManager processingModelManager;
    private Consumer<SystemStatus> statusSubscriber;

    private volatile boolean initialised = false;

    private final Timer executor = new Timer("ReatMetric Core - Executor", true);

    public ReatmetricSystemImpl(String initString) throws ReatmetricException {
        try {
            configuration = ServiceCoreConfiguration.load(new FileInputStream(initString));
        } catch (Exception e) {
            throw new ReatmetricException(e);
        }
    }

    @Override
    public void initialise(Consumer<SystemStatus> consumer) throws ReatmetricException {
        LOG.info("Reatmetric Core System initialisation");

        statusSubscriber = consumer;
        // Prepare the logging facility
        if(configuration.getLogPropertyFile() != null) {
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(configuration.getLogPropertyFile()));
            } catch (IOException e) {
                throw new ReatmetricException(e);
            }
        }
        // Load the archive
        if(configuration.getArchiveLocation() != null) {
            LOG.info("Loading archive at location " + configuration.getArchiveLocation());
            ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
            if(archiveLoader.findFirst().isPresent()) {
                archive = archiveLoader.findFirst().get().buildArchive(configuration.getArchiveLocation());
                archive.connect();
            } else {
                throw new ReatmetricException("Archive location configured, but no archive factory deployed");
            }
        }
        // Load the operational data broker
        LOG.info("Loading operational message broker");
        IOperationalMessageArchive messageArchive = archive != null ? archive.getArchive(IOperationalMessageArchive.class) : null;
        IAcknowledgedMessageArchive ackMessageArchive = archive != null ? archive.getArchive(IAcknowledgedMessageArchive.class) : null;
        messageBroker = new OperationalMessageBrokerImpl(messageArchive, ackMessageArchive);
        // Load the raw data broker
        LOG.info("Loading raw data broker");
        IRawDataArchive rawDataArchive = archive != null ? archive.getArchive(IRawDataArchive.class) : null;
        rawDataBroker = new RawDataBrokerImpl(this, rawDataArchive);
        // Load the processing model manager and services
        LOG.info("Loading processing model");
        processingModelManager = new ProcessingModelManager(archive, configuration.getDefinitionsLocation(), configuration.getInitialisation());
        // Load the scheduler
        LOG.info("Loading scheduler");
        ServiceLoader<ISchedulerFactory> scheduleLoader = ServiceLoader.load(ISchedulerFactory.class);
        if(scheduleLoader.findFirst().isPresent()) {
            scheduler = scheduleLoader.findFirst().get().buildScheduler(configuration.getSchedulerConfiguration(), archive, processingModelManager, processingModelManager.getEventDataMonitorService(), processingModelManager.getActivityOccurrenceDataMonitorService(), processingModelManager.getParameterDataMonitorService());
        } else {
            LOG.warning("Scheduler implementation not found");
        }
        // Load the drivers
        for(DriverConfiguration dc : configuration.getDrivers()) {
            LOG.info("Loading driver " + dc.getName());
            IDriver driver = loadDriver(dc);
            if(driver != null) {
                // Register the driver
                drivers.add(driver);
                // Get and register the sle connectors
                registerConnectors(driver.getTransportConnectors());
                // Get and register the activity handlers
                registerActivityHandlers(dc.getName(), driver.getActivityHandlers());
                // Get and register the raw data renderer
                registerRawDataRenderers(driver.getRawDataRenderers());
                LOG.info("Driver " + dc.getName() + " successfully loaded");
            } else {
                LOG.severe("Driver " + dc.getName() + " not found in the service registry");
            }
        }
        // Initialise scheduler now
        if(scheduler != null) {
            try {
                scheduler.initialise();
            } catch (RemoteException e) {
                // Ignore for this method
            }
        }
        // Check the connection status of the connectors (autoconnect, reconnect)
        initialiseConnectorsProperties();
        // Derive system status
        deriveSystemStatus();
        initialised = true;
        // Done and ready to go
        LOG.info("Reatmetric Core System loaded with status " + systemStatus);
    }

    private void initialiseConnectorsProperties() {
        List<ITransportConnector> toBeStartedNow = new LinkedList<>();
        for(ITransportConnector connector : transportConnectors) {
            // Reconnection flag
            boolean isReconnectExcluded = configuration.getAutostartConnectors().getReconnectExclusions().stream().anyMatch(o -> {
                try {
                    return connector.getName().matches(o);
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Cannot retrieve name of transport connector: " + e.getMessage(), e);
                    return true;
                }
            });
            try {
                connector.setReconnect(configuration.getAutostartConnectors().isReconnect() && !isReconnectExcluded);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Cannot set reconnection status of transport connector: " + e.getMessage(), e);
            }
            boolean isAutostartExcluded = configuration.getAutostartConnectors().getStartupExclusions().stream().anyMatch(o -> {
                try {
                    return connector.getName().matches(o);
                } catch (RemoteException e) {
                    LOG.log(Level.WARNING, "Cannot retrieve name of transport connector: " + e.getMessage(), e);
                    return true;
                }
            });
            if(configuration.getAutostartConnectors().isStartup() && !isAutostartExcluded) {
                toBeStartedNow.add(connector);
            }
        }
        // Start now!
        for(ITransportConnector connector : toBeStartedNow) {
            try {
                connector.initialise(connector.getCurrentProperties());
                connector.connect();
            } catch (Exception e) { // Protect the startup sequence!
                LOG.log(Level.WARNING, "Cannot start transport connector: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public SystemStatus getStatus() {
        return systemStatus;
    }

    private void registerRawDataRenderers(List<IRawDataRenderer> rawDataRenderers) {
        for(IRawDataRenderer renderer : rawDataRenderers) {
            // The selection is performed by handler and type: a renderer supports 1 handler and many types (for that handler)
            String handler = renderer.getHandler();
            List<String> types = renderer.getSupportedTypes();
            for(String type : types) {
                renderers.put(Pair.of(handler, type), renderer);
            }
        }
    }

    private void deriveSystemStatus() {
        SystemStatus cumulative = SystemStatus.UNKNOWN;
        for(IDriver d : drivers) {
            if(d.getDriverStatus().ordinal() > cumulative.ordinal()) {
                cumulative = d.getDriverStatus();
            }
        }
        if(systemStatus != cumulative) {
            systemStatus = cumulative;
            statusSubscriber.accept(cumulative);
        }
    }

    @Override
    public void dispose() throws ReatmetricException {
        initialised = false;
        for(ITransportConnector tc : transportConnectors) {
            try {
                tc.deregister(this);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Cannot deregister from transport connector: " + e.getMessage(), e);
            }
        }
        for(IDriver d : drivers) {
            d.dispose();
        }
        if(scheduler != null) {
            try {
                scheduler.dispose();
            } catch (RemoteException e) {
                // Ignore for this method
            }
            scheduler = null;
        }
        if(processingModelManager != null) {
            processingModelManager.dispose();
            processingModelManager = null;
        }
        rawDataBroker = null;
        if(messageBroker != null) {
            messageBroker.close();
            messageBroker = null;
        }
        if(archive != null) {
            archive.dispose();
            archive = null;
        }
        drivers.clear();
        transportConnectors.clear();
        statusSubscriber = null;
    }

    private void registerActivityHandlers(String driverName, List<IActivityHandler> activityHandlers) {
        for(IActivityHandler h : activityHandlers) {
            try {
                getProcessingModel().registerActivityHandler(h);
            } catch (ProcessingModelException e) {
                LOG.log(Level.WARNING, "Cannot register activity handler " + h + "from driver " + driverName + ", handler ignored", e);
            }
        }
    }

    private void registerConnectors(List<ITransportConnector> transportConnectors) {
        this.transportConnectors.addAll(transportConnectors);
        transportConnectors.forEach(o -> {
            try {
                o.register(this);
            } catch (RemoteException e) {
                LOG.log(Level.WARNING, "Cannot register to transport connector: " + e.getMessage(), e);
            }
        });
    }

    private IDriver loadDriver(DriverConfiguration dc) throws DriverException {
        ServiceLoader<IDriver> serviceLoader = ServiceLoader.load(IDriver.class);
        Optional<ServiceLoader.Provider<IDriver>> provider = serviceLoader.stream().filter(pr -> filterDriver(pr.type(), dc.getType())).findFirst();
        IDriver driver = null;
        if(provider.isPresent()) {
            driver = provider.get().get();
            driver.initialise(dc.getName(), dc.getConfiguration(), this, this.configuration, this);
        }
        return driver;
    }

    private boolean filterDriver(Class<? extends IDriver> provider, String target) {
        return provider.getName().equals(target);
    }

    public LinkedHashMap<String, String> getRenderedInformation(RawData rawData) {
        if(rawData == null) {
            return new LinkedHashMap<>();
        }
        Pair<String, String> key = Pair.of(rawData.getHandler(), rawData.getType());
        IRawDataRenderer renderer = renderers.get(key);
        if(renderer == null) {
            return new LinkedHashMap<>();
        } else {
            try {
                return Objects.requireNonNullElseGet(renderer.render(rawData), LinkedHashMap::new);
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                return new LinkedHashMap<>();
            }
        }
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() {
        return messageBroker;
    }

    @Override
    public IOperationalMessageCollectorService getOperationalMessageCollectorService() {
        return messageBroker.getCollectorService();
    }

    @Override
    public IAcknowledgedMessageProvisionService getAcknowledgedMessageMonitorService() {
        return messageBroker.getAcknowledgedMessageBroker();
    }

    @Override
    public IAcknowledgementService getAcknowledgementService() {
        return messageBroker.getAcknowledgedMessageBroker().getAcknowledgementService();
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService()  {
        return rawDataBroker;
    }

    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() {
        return processingModelManager.getParameterDataMonitorService();
    }

    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() {
        return processingModelManager;
    }

    @Override
    public IEventDataProvisionService getEventDataMonitorService() {
        return processingModelManager.getEventDataMonitorService();
    }

    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() {
        return processingModelManager.getAlarmParameterDataMonitorService();
    }

    @Override
    public IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() {
        return processingModelManager.getActivityOccurrenceDataMonitorService();
    }

    @Override
    public IActivityExecutionService getActivityExecutionService() {
        return processingModelManager;
    }

    @Override
    public IScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public IScheduledActivityDataProvisionService getScheduledActivityDataMonitorService() {
        return scheduler;
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return transportConnectorsImmutable;
    }

    @Override
    public IArchive getArchive() {
        return archive;
    }

    @Override
    public IProcessingModel getProcessingModel() {
        return processingModelManager.getProcessingModel();
    }

    @Override
    public IReatmetricSystem getServiceFactory() {
        return this;
    }

    @Override
    public IRawDataBroker getRawDataBroker() {
        return rawDataBroker;
    }

    @Override
    public IOperationalMessageBroker getOperationalMessageBroker() {
        return messageBroker;
    }

    @Override
    public String getSystemName() {
        return getName();
    }

    @Override
    public void driverStatusUpdate(String driverName, SystemStatus status) {
        deriveSystemStatus();
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        if(!initialised) {
            return Collections.emptyList();
        }
        List<DebugInformation> toReturn = new ArrayList<>(100);
        toReturn.addAll(this.processingModelManager.getProcessingModel().currentDebugInfo());
        if(this.archive != null) {
            toReturn.addAll(this.archive.currentDebugInfo());
        }
        for(IDriver d : this.drivers) {
            toReturn.addAll(d.currentDebugInfo());
        }
        // System specific information
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        int avlProcess = Runtime.getRuntime().availableProcessors();

        toReturn.add(DebugInformation.of("Java", "Reserved Memory", totalMemory/1000000, null, "MB" ));
        toReturn.add(DebugInformation.of("Java", "Free Memory", freeMemory/1000000, null, "MB" ));
        // toReturn.add(DebugInformation.of("Java", "Used Memory", (totalMemory/1000000) - (freeMemory/1000000), null, "MB" ));
        toReturn.add(DebugInformation.of("Java", "Used Memory", (totalMemory/1000000) - (freeMemory/1000000), (totalMemory/1000000), "MB" ));
        toReturn.add(DebugInformation.of("Java", "Max Memory", maxMemory/1000000, null, "MB" ));
        toReturn.add(DebugInformation.of("Java", "CPUs", avlProcess, null, null ));
        //
        return toReturn;
    }

    @Override
    public void status(TransportStatus status) throws RemoteException {
        if(status.isAutoReconnect() &&
                (status.getStatus() == TransportConnectionStatus.ERROR ||
                 status.getStatus() == TransportConnectionStatus.ABORTED ||
                 status.getStatus() == TransportConnectionStatus.IDLE)) {
            // Look for the connector
            for(ITransportConnector c : transportConnectors) {
                if(c.getName().equals(status.getName())) {
                    scheduleReconnectionOf(c);
                    return;
                }
            }
        }
    }

    private void scheduleReconnectionOf(ITransportConnector c) {
        executor.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if(c.getConnectionStatus() != TransportConnectionStatus.OPEN && c.getConnectionStatus() != TransportConnectionStatus.CONNECTING) {
                        c.connect();
                    }
                } catch (RemoteException | TransportException e) {
                    LOG.log(Level.WARNING, "Cannot reconnect transport connector: " + e.getMessage(), e);
                    scheduleReconnectionOf(c);
                }
            }
        }, 1000);
    }
}
