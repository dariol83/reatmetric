/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.*;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.DriverConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.core.impl.OperationalMessageBrokerImpl;
import eu.dariolucia.reatmetric.core.impl.ProcessingModelManager;
import eu.dariolucia.reatmetric.core.impl.RawDataBrokerImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ServiceCoreImpl implements IReatmetricSystem, IServiceCoreContext, IDriverListener {

    private static final Logger LOG = Logger.getLogger(ServiceCoreImpl.class.getName());
    private static final String INIT_FILE_KEY = "reatmetric.core.config"; // Absolute location of the configuration file, to configure the core instance

    private final List<IDriver> drivers = new ArrayList<>();
    private final List<ITransportConnector> transportConnectors = new ArrayList<>();
    private final List<ITransportConnector> transportConnectorsImmutable = Collections.unmodifiableList(transportConnectors);
    private final ServiceCoreConfiguration configuration;

    private volatile SystemStatus systemStatus = SystemStatus.UNKNOWN;

    private IArchive archive;
    private OperationalMessageBrokerImpl messageBroker;
    private RawDataBrokerImpl rawDataBroker;
    private ProcessingModelManager processingModelManager;
    private Consumer<SystemStatus> statusSubscriber;

    public ServiceCoreImpl() {
        try {
            String configurationFileLocation = System.getProperty(INIT_FILE_KEY);
            configuration = ServiceCoreConfiguration.load(new FileInputStream(configurationFileLocation));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialise(Consumer<SystemStatus> consumer) throws ReatmetricException {
        LOG.info("Reatmetric Core System initialisation");
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
        messageBroker = new OperationalMessageBrokerImpl(messageArchive);
        // Load the raw data broker
        LOG.info("Loading raw data broker");
        IRawDataArchive rawDataArchive = archive != null ? archive.getArchive(IRawDataArchive.class) : null;
        rawDataBroker = new RawDataBrokerImpl(rawDataArchive);
        // Load the processing model manager and services
        LOG.info("Loading processing model");
        processingModelManager = new ProcessingModelManager(archive, configuration.getDefinitionsLocation());
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
                LOG.info("Driver " + dc.getName() + " successfully loaded");
            } else {
                LOG.severe("Driver " + dc.getName() + " not found in the service registry");
            }
        }
        // Derive system status
        statusSubscriber = consumer;
        deriveSystemStatus();
        // Done and ready to go
        LOG.info("Reatmetric Core System loaded with status " + systemStatus);
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
        for(IDriver d : drivers) {
            d.dispose();
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
    }

    private IDriver loadDriver(DriverConfiguration dc) throws DriverException {
        ServiceLoader<IDriver> serviceLoader = ServiceLoader.load(IDriver.class);
        Optional<ServiceLoader.Provider<IDriver>> provider = serviceLoader.stream().filter(pr -> pr.type().getName().equals(dc.getType())).findFirst();
        IDriver driver = null;
        if(provider.isPresent()) {
            driver = provider.get().get();
            driver.initialise(dc.getName(), dc.getConfiguration(), this, this.configuration, this);
        }
        return driver;
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
    public List<ITransportConnector> getTransportConnectors() {
        return transportConnectorsImmutable;
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
    public void driverStatusUpdate(String driverName, SystemStatus status) {
        deriveSystemStatus();
    }
}
