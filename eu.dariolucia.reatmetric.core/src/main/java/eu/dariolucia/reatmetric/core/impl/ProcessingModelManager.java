/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelProvisionService;
import eu.dariolucia.reatmetric.api.model.ISystemModelSubscriber;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.core.impl.managers.ActivityOccurrenceDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.AlarmParameterDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.EventDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.ParameterDataAccessManager;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcessingModelManager implements IProcessingModelOutput, ISystemModelProvisionService, IActivityExecutionService {

    private static final Logger LOG = Logger.getLogger(ProcessingModelManager.class.getName());

    private final IParameterDataArchive parameterArchive;
    private final IEventDataArchive eventArchive;
    private final IActivityOccurrenceDataArchive activityArchive;
    private final IAlarmParameterDataArchive alarmArchive;

    private final IProcessingModel processingModel;

    private final ParameterDataAccessManager parameterDataAccessManager;
    private final EventDataAccessManager eventDataAccessManager;
    private final AlarmParameterDataAccessManager alarmDataAccessManager;
    private final ActivityOccurrenceDataAccessManager activityOccurrenceDataAccessManager;

    private final Map<ISystemModelSubscriber, SystemModelSubscriberWrapper> subscribers = new LinkedHashMap<>();

    public ProcessingModelManager(IArchive archive, String definitionsLocation) throws ReatmetricException {
        Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters = new HashMap<>();
        if(archive != null) {
            parameterArchive = archive.getArchive(IParameterDataArchive.class);
            eventArchive = archive.getArchive(IEventDataArchive.class);
            activityArchive = archive.getArchive(IActivityOccurrenceDataArchive.class);
            alarmArchive = archive.getArchive(IAlarmParameterDataArchive.class);
            // Retrieve the IDs
            initialUniqueCounters.put(ParameterData.class, parameterArchive.retrieveLastId().asLong());
            initialUniqueCounters.put(EventData.class, eventArchive.retrieveLastId().asLong());
            initialUniqueCounters.put(AlarmParameterData.class, alarmArchive.retrieveLastId().asLong());
            initialUniqueCounters.put(ActivityOccurrenceData.class, activityArchive.retrieveLastId(ActivityOccurrenceData.class).asLong());
            initialUniqueCounters.put(ActivityOccurrenceReport.class, activityArchive.retrieveLastId(ActivityOccurrenceReport.class).asLong());
        } else {
            parameterArchive = null;
            eventArchive = null;
            activityArchive = null;
            alarmArchive = null;
        }
        // Aggregate all the definitions inside the definitionsLocation path
        ProcessingDefinition defs = readProcessingDefinitions(definitionsLocation);
        // Create the access services
        parameterDataAccessManager = new ParameterDataAccessManager(parameterArchive);
        alarmDataAccessManager = new AlarmParameterDataAccessManager(alarmArchive);
        eventDataAccessManager = new EventDataAccessManager(eventArchive);
        activityOccurrenceDataAccessManager = new ActivityOccurrenceDataAccessManager(activityArchive);
        // Create the model
        ServiceLoader<IProcessingModelFactory> modelLoader = ServiceLoader.load(IProcessingModelFactory.class);
        if(modelLoader.findFirst().isPresent()) {
            IProcessingModelFactory modelFactory = modelLoader.findFirst().get();
            processingModel = modelFactory.build(defs, this, initialUniqueCounters);
            parameterDataAccessManager.setProcessingModel(processingModel);
            alarmDataAccessManager.setProcessingModel(processingModel);
            eventDataAccessManager.setProcessingModel(processingModel);
            activityOccurrenceDataAccessManager.setProcessingModel(processingModel);
        } else {
            throw new ReatmetricException("Archive location configured, but no archive factory deployed");
        }
    }

    private ProcessingDefinition readProcessingDefinitions(String definitionsLocation) throws ReatmetricException {
        ProcessingDefinition aggregated = new ProcessingDefinition();
        File folder = new File(definitionsLocation);
        if(!folder.exists() || folder.listFiles() == null) {
            throw new ReatmetricException("Cannot read definition files in folder " + definitionsLocation);
        }
        for(File def : folder.listFiles()) {
            try {
                ProcessingDefinition eachDef = ProcessingDefinition.load(new FileInputStream(def));
                aggregated.getParameterDefinitions().addAll(eachDef.getParameterDefinitions());
                aggregated.getEventDefinitions().addAll(eachDef.getEventDefinitions());
                aggregated.getActivityDefinitions().addAll(eachDef.getActivityDefinitions());
            } catch(IOException | JAXBException e) {
                LOG.log(Level.WARNING, "Cannot read definitions at " + def.getAbsolutePath(), e);
            }
        }
        return aggregated;
    }

    @Override
    public void notifyUpdate(List<AbstractDataItem> items) {
        parameterDataAccessManager.distribute(items);
        alarmDataAccessManager.distribute(items);
        eventDataAccessManager.distribute(items);
        activityOccurrenceDataAccessManager.distribute(items);

        for(SystemModelSubscriberWrapper w : subscribers.values()) {
            w.notifyItems(items);
        }
    }

    public IParameterDataProvisionService getParameterDataMonitorService() {
        return parameterDataAccessManager;
    }

    public IEventDataProvisionService getEventDataMonitorService() {
        return eventDataAccessManager;
    }

    public IActivityOccurrenceDataProvisionService getActivityOccurrenceDataMonitorService() {
        return activityOccurrenceDataAccessManager;
    }

    public AlarmParameterDataAccessManager getAlarmParameterDataMonitorService() {
        return alarmDataAccessManager;
    }

    @Override
    public void subscribe(ISystemModelSubscriber subscriber) {
        if(!subscribers.containsKey(subscriber)) {
            subscribers.put(subscriber, new SystemModelSubscriberWrapper(subscriber));
        }
    }

    @Override
    public void unsubscribe(ISystemModelSubscriber subscriber) {
        SystemModelSubscriberWrapper wrapper = subscribers.remove(subscriber);
        if(wrapper != null) {
            wrapper.terminate();
        }
    }

    @Override
    public SystemEntity getRoot() throws ReatmetricException {
        return processingModel.getRoot();
    }

    @Override
    public List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ReatmetricException {
        return processingModel.getContainedEntities(se);
    }

    @Override
    public SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException {
        return processingModel.getSystemEntityAt(path);
    }

    @Override
    public SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException {
        return processingModel.getSystemEntityOf(externalId);
    }

    @Override
    public int getExternalIdOf(SystemEntityPath path) throws ReatmetricException {
        return processingModel.getExternalIdOf(path);
    }

    @Override
    public SystemEntityPath getPathOf(int externalId) throws ReatmetricException {
        return processingModel.getPathOf(externalId);
    }

    @Override
    public IUniqueId startActivity(ActivityRequest request) throws ReatmetricException {
        return processingModel.startActivity(request);
    }

    @Override
    public IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ReatmetricException {
        return processingModel.createActivity(request, currentProgress);
    }

    @Override
    public void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ReatmetricException {
        processingModel.purgeActivities(activityOccurrenceIds);
    }

    public IProcessingModel getProcessingModel() {
        return processingModel;
    }

    private static class SystemModelSubscriberWrapper {

        private final ISystemModelSubscriber subscriber;
        private final ExecutorService dispatcher;

        public SystemModelSubscriberWrapper(ISystemModelSubscriber subscriber) {
            this.subscriber = subscriber;
            this.dispatcher = Executors.newSingleThreadExecutor((r) -> {
                Thread t = new Thread();
                t.setName("Reatmetric System Model Dispatcher - " + subscriber);
                t.setDaemon(true);
                return t;
            });
        }

        public void notifyItems(List<AbstractDataItem> toDistribute) {
            if(this.dispatcher.isShutdown()) {
                return;
            }
            List<SystemEntity> filtered = toDistribute.stream().filter((i) -> i.getClass().equals(SystemEntity.class)).map(o -> (SystemEntity) o).collect(Collectors.toList());
            if(!filtered.isEmpty()) {
                subscriber.dataItemsReceived(filtered);
            }
        }

        public void terminate() {
            this.dispatcher.shutdownNow();
            try {
                this.dispatcher.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Nothing to do
            }
        }
    }
}
