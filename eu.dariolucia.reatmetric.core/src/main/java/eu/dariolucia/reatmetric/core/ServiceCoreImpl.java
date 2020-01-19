/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IOperationalMessageBroker;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.core.impl.OperationalMessageBrokerImpl;
import eu.dariolucia.reatmetric.core.impl.ProcessingModelManager;
import eu.dariolucia.reatmetric.core.impl.RawDataBrokerImpl;

import java.io.FileInputStream;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.LogManager;

public class ServiceCoreImpl implements IServiceFactory, IServiceCoreContext {

    private static final String INIT_FILE_KEY = "reatmetric.core.config"; // Absolute location of the init file, to configure the core instance

    private final ServiceCoreConfiguration configuration;

    private IArchive archive;
    private OperationalMessageBrokerImpl messageBroker;
    private RawDataBrokerImpl rawDataBroker;
    private ProcessingModelManager processingModelManager;

    public ServiceCoreImpl() {
        try {
            String configurationFileLocation = System.getProperty(INIT_FILE_KEY);
            configuration = ServiceCoreConfiguration.load(new FileInputStream(configurationFileLocation));
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise() throws Exception {
        // Prepare the logging facility
        if(configuration.getLogPropertyFile() != null) {
            LogManager.getLogManager().readConfiguration(new FileInputStream(configuration.getLogPropertyFile()));
        }
        // Load the archive
        if(configuration.getArchiveLocation() != null) {
            ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
            if(archiveLoader.findFirst().isPresent()) {
                archive = archiveLoader.findFirst().get().buildArchive(configuration.getArchiveLocation());
                archive.connect();
            } else {
                throw new ReatmetricException("Archive location configured, but no archive factory deployed");
            }
        }
        // Load the operational data broker
        IOperationalMessageArchive messageArchive = archive != null ? archive.getArchive(IOperationalMessageArchive.class) : null;
        messageBroker = new OperationalMessageBrokerImpl(messageArchive);
        // Load the raw data broker
        IRawDataArchive rawDataArchive = archive != null ? archive.getArchive(IRawDataArchive.class) : null;
        rawDataBroker = new RawDataBrokerImpl(rawDataArchive);
        // Load the processing model manager and services
        processingModelManager = new ProcessingModelManager(archive, configuration.getDefinitionsLocation());
        // Load the drivers
        // For each driver, get and register the transport connectors
        // For each driver, get and register the activity handlers
        // TODO
        // Done and ready to go
    }

    @Override
    public String getSystem() {
        return configuration.getName();
    }

    @Override
    public IOperationalMessageProvisionService getOperationalMessageMonitorService() throws ReatmetricException {
        return messageBroker;
    }

    @Override
    public IRawDataProvisionService getRawDataMonitorService() throws ReatmetricException {
        return rawDataBroker;
    }

    @Override
    public IParameterDataProvisionService getParameterDataMonitorService() throws ReatmetricException {
        return processingModelManager.getParameterDataMonitorService();
    }

    @Override
    public ISystemModelProvisionService getSystemModelMonitorService() throws ReatmetricException {
        return processingModelManager.getSystemModelMonitorService();
    }

    @Override
    public IEventDataProvisionService getEventDataMonitorService() throws ReatmetricException {
        return processingModelManager.getEventDataMonitorService();
    }

    @Override
    public IAlarmParameterDataProvisionService getAlarmParameterDataMonitorService() throws ReatmetricException {
        return processingModelManager.getAlarmParameterDataMonitorService();
    }

    @Override
    public IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() throws ReatmetricException {
        return processingModelManager.getActivityOccurrenceDataMonitorService();
    }

    @Override
    public IActivityExecutionService getActivityExecutionService() throws ReatmetricException {
        return processingModelManager.getActivityExecutionService();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() throws ReatmetricException {
        return null;
    }

    @Override
    public IProcessingModel getProcessingModel() {
        return processingModelManager.getProcessingModel();
    }

    @Override
    public IServiceFactory getServiceFactory() {
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
}
