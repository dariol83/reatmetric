/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataArchive;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.core.impl.managers.ParameterAccessManager;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class ProcessingModelManager implements IProcessingModelOutput {

    private final IParameterDataArchive parameterArchive;
    private final IEventDataArchive eventArchive;
    private final IActivityOccurrenceDataArchive activityArchive;
    private final IAlarmParameterDataArchive alarmArchive;

    private final IProcessingModel processingModel;

    private final ParameterAccessManager parameterAccessManager;

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
        parameterAccessManager = new ParameterAccessManager(parameterArchive);
        alarmAccessManager = new AlarmAccessManager(alarmArchive);
        eventAccessManager = new EventAccessManager(eventArchive);
        activityAccessManager = new ActivityAccessManager(activityArchive);
        // Create the model
        ServiceLoader<IProcessingModelFactory> modelLoader = ServiceLoader.load(IProcessingModelFactory.class);
        if(modelLoader.findFirst().isPresent()) {
            IProcessingModelFactory modelFactory = modelLoader.findFirst().get();
            processingModel = modelFactory.build(defs, this, initialUniqueCounters);
            parameterAccessManager.setProcessingModel(processingModel);
            alarmAccessManager.setProcessingModel(processingModel);
            eventAccessManager.setProcessingModel(processingModel);
            activityAccessManager.setProcessingModel(processingModel);
        } else {
            throw new ReatmetricException("Archive location configured, but no archive factory deployed");
        }
    }

    private ProcessingDefinition readProcessingDefinitions(String definitionsLocation) {
        ProcessingDefinition aggregated = new ProcessingDefinition();
        File folder = new File(definitionsLocation);
        for(File def : folder.listFiles()) {
            try {
                ProcessingDefinition eachDef = ProcessingDefinition.load(new FileInputStream(def));
                aggregated.getParameterDefinitions().addAll(eachDef.getParameterDefinitions());
                aggregated.getEventDefinitions().addAll(eachDef.getEventDefinitions());
                aggregated.getActivityDefinitions().addAll(eachDef.getActivityDefinitions());
            } catch(IOException | JAXBException e) {
                // TODO log properly
            }
        }
        return aggregated;
    }

    @Override
    public void notifyUpdate(List<AbstractDataItem> items) {
        parameterAccessManager.distribute(items);
        alarmAccessManager.distribute(items);
        eventAccessManager.distribute(items);
        activityAccessManager.distribute(items);
    }

    public IParameterDataProvisionService getParameterDataMonitorService() {
        return parameterAccessManager;
    }
}
