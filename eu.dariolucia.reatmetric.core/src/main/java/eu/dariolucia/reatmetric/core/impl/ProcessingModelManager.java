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
import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
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
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.core.configuration.AbstractInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.configuration.TimeInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.impl.managers.ActivityOccurrenceDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.AlarmParameterDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.EventDataAccessManager;
import eu.dariolucia.reatmetric.core.impl.managers.ParameterDataAccessManager;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcessingModelManager implements IProcessingModelOutput, ISystemModelProvisionService, IActivityExecutionService {

    private static final Logger LOG = Logger.getLogger(ProcessingModelManager.class.getName());

    private final IProcessingModel processingModel;

    private final ParameterDataAccessManager parameterDataAccessManager;
    private final EventDataAccessManager eventDataAccessManager;
    private final AlarmParameterDataAccessManager alarmDataAccessManager;
    private final ActivityOccurrenceDataAccessManager activityOccurrenceDataAccessManager;

    private final Map<ISystemModelSubscriber, SystemModelSubscriberWrapper> subscribers = new LinkedHashMap<>();

    public ProcessingModelManager(IArchive archive, String definitionsLocation, AbstractInitialisationConfiguration initialisation) throws ReatmetricException {
        if(initialisation instanceof TimeInitialisationConfiguration) {
            // Clean up required
            cleanUp(archive, ((TimeInitialisationConfiguration) initialisation).getTime());
        }
        Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters = new HashMap<>();
        IParameterDataArchive parameterArchive;
        IEventDataArchive eventArchive;
        IActivityOccurrenceDataArchive activityArchive;
        IAlarmParameterDataArchive alarmArchive;
        if(archive != null) {
            parameterArchive = archive.getArchive(IParameterDataArchive.class);
            eventArchive = archive.getArchive(IEventDataArchive.class);
            activityArchive = archive.getArchive(IActivityOccurrenceDataArchive.class);
            alarmArchive = archive.getArchive(IAlarmParameterDataArchive.class);
            // Retrieve the IDs
            IUniqueId lastId = parameterArchive.retrieveLastId();
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer("Parameter Data last unique ID: " + lastId);
            }
            initialUniqueCounters.put(ParameterData.class, lastId != null ? lastId.asLong() : 0L);
            lastId = eventArchive.retrieveLastId();
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer("Event Data last unique ID: " + lastId);
            }
            initialUniqueCounters.put(EventData.class, lastId != null ? lastId.asLong() : 0L);
            lastId = alarmArchive.retrieveLastId();
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer("Alarm Parameter Data last unique ID: " + lastId);
            }
            initialUniqueCounters.put(AlarmParameterData.class, lastId != null ? lastId.asLong() : 0L);
            lastId = activityArchive.retrieveLastId(ActivityOccurrenceData.class);
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer("Activity Occurrence Data last unique ID: " + lastId);
            }
            initialUniqueCounters.put(ActivityOccurrenceData.class, lastId != null ? lastId.asLong() : 0L);
            lastId = activityArchive.retrieveLastId(ActivityOccurrenceReport.class);
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer("Activity Occurrence Report last unique ID: " + lastId);
            }
            initialUniqueCounters.put(ActivityOccurrenceReport.class, lastId != null ? lastId.asLong() : 0L);
        } else {
            parameterArchive = null;
            eventArchive = null;
            activityArchive = null;
            alarmArchive = null;
        }
        // Aggregate all the definitions inside the definitionsLocation path
        ProcessingDefinition defs = ProcessingDefinition.loadAll(definitionsLocation);
        // Create the access services
        parameterDataAccessManager = new ParameterDataAccessManager(parameterArchive);
        alarmDataAccessManager = new AlarmParameterDataAccessManager(alarmArchive);
        eventDataAccessManager = new EventDataAccessManager(eventArchive);
        activityOccurrenceDataAccessManager = new ActivityOccurrenceDataAccessManager(activityArchive);
        // If the processing model initialisation is needed, create the initialiser
        ArchiveInitialiser initialiser = null;
        if(initialisation != null) {
            initialiser = new ArchiveInitialiser(archive, initialisation);
        }
        // Create the model
        ServiceLoader<IProcessingModelFactory> modelLoader = ServiceLoader.load(IProcessingModelFactory.class);
        if(modelLoader.findFirst().isPresent()) {
            IProcessingModelFactory modelFactory = modelLoader.findFirst().get();
            processingModel = modelFactory.build(defs, this, initialUniqueCounters, initialiser);
            parameterDataAccessManager.setProcessingModel(processingModel);
            alarmDataAccessManager.setProcessingModel(processingModel);
            eventDataAccessManager.setProcessingModel(processingModel);
            activityOccurrenceDataAccessManager.setProcessingModel(processingModel);
            if(initialiser != null) {
                initialiser.dispose();
            }
        } else {
            if(initialiser != null) {
                initialiser.dispose();
            }
            throw new ReatmetricException("Archive location configured, but no archive factory deployed");
        }
    }

    private void cleanUp(IArchive archive, Date time) throws ReatmetricException {
        List<Pair<Class<? extends IDataItemArchive>, Class<? extends AbstractDataItem>>> toCheckPairs = Arrays.asList(
                Pair.of(IParameterDataArchive.class, ParameterData.class),
                Pair.of(IAlarmParameterDataArchive.class, AlarmParameterData.class),
                Pair.of(IEventDataArchive.class, EventData.class),
                Pair.of(IActivityOccurrenceDataArchive.class, ActivityOccurrenceReport.class),
                Pair.of(IActivityOccurrenceDataArchive.class, ActivityOccurrenceData.class)
        );
        for(Pair<Class<? extends IDataItemArchive>, Class<? extends AbstractDataItem>> pair : toCheckPairs) {
            IDataItemArchive arc = archive.getArchive(pair.getFirst());
            if(arc != null) {
                try {
                    arc.purge(time.toInstant(), RetrievalDirection.TO_FUTURE);
                } catch (ArchiveException e) {
                    throw new ReatmetricException("Archive purge problem for time " + time, e);
                }
            }
        }
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
    public void enable(SystemEntityPath path) throws ReatmetricException {
        processingModel.enable(path);
    }

    @Override
    public void disable(SystemEntityPath path) throws ReatmetricException {
        processingModel.disable(path);
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

    public void dispose() {
        // Kill subscribers
        for(SystemModelSubscriberWrapper s : subscribers.values()) {
            s.terminate();
        }
        subscribers.clear();
        // Kill managers
        parameterDataAccessManager.dispose();
        eventDataAccessManager.dispose();
        alarmDataAccessManager.dispose();
        activityOccurrenceDataAccessManager.dispose();
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
