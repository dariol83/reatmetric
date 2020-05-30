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

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.core.configuration.AbstractInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ResumeInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.configuration.TimeInitialisationConfiguration;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ArchiveInitialiser implements IProcessingModelInitialiser {

    private static final Logger LOG = Logger.getLogger(ArchiveInitialiser.class.getName());

    private final Instant maxLookBackTime;
    private final Instant initTime;
    private final IArchive initArchive;
    private final boolean externalArchive;

    private final Map<Integer, List<AbstractDataItem>> parameter2state = new HashMap<>();
    private final Map<Integer, List<AbstractDataItem>> event2state = new HashMap<>();
    private final Map<Integer, List<AbstractDataItem>> activity2state = new HashMap<>();

    public ArchiveInitialiser(IArchive processingArchive, AbstractInitialisationConfiguration configuration, ProcessingDefinition definitions) throws ReatmetricException {
        if(configuration instanceof TimeInitialisationConfiguration) {
            String archiveLocation = ((TimeInitialisationConfiguration) configuration).getArchiveLocation();
            if(archiveLocation == null) {
                this.initArchive = processingArchive;
                this.externalArchive = false;
            } else {
                this.externalArchive = true;
                ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
                if (archiveLoader.findFirst().isPresent()) {
                    initArchive = archiveLoader.findFirst().get().buildArchive(((TimeInitialisationConfiguration) configuration).getArchiveLocation());
                    initArchive.connect();
                } else {
                    throw new ReatmetricException("Initialisation archive configured to " + archiveLocation + ", but no archive factory deployed");
                }
            }
            this.initTime = ((TimeInitialisationConfiguration) configuration).getTime().toInstant();
        } else if(configuration instanceof ResumeInitialisationConfiguration) {
            this.externalArchive = false;
            initArchive = processingArchive;
            // In order to be optimal with the dates, let's get the latest stored generation time for each data type: events, parameters, activities
            Instant lastStoredGenerationTime = null;
            List<Pair<Class<? extends IDataItemArchive>, Class<? extends AbstractDataItem>>> toCheckPairs = Arrays.asList(
                    Pair.of(IParameterDataArchive.class, ParameterData.class),
                    Pair.of(IAlarmParameterDataArchive.class, AlarmParameterData.class),
                    Pair.of(IEventDataArchive.class, EventData.class),
                    Pair.of(IActivityOccurrenceDataArchive.class, ActivityOccurrenceReport.class),
                    Pair.of(IActivityOccurrenceDataArchive.class, ActivityOccurrenceData.class)
            );
            for(Pair<Class<? extends IDataItemArchive>, Class<? extends AbstractDataItem>> pair : toCheckPairs) {
                IDataItemArchive arc = processingArchive.getArchive(pair.getFirst());
                if(arc != null) {
                    Instant retrieved = arc.retrieveLastGenerationTime(pair.getSecond());
                    if(retrieved != null && (lastStoredGenerationTime == null || lastStoredGenerationTime.isBefore(retrieved))) {
                        lastStoredGenerationTime = retrieved;
                    }
                }
            }
            // Still null? Use now
            if(lastStoredGenerationTime == null) {
                lastStoredGenerationTime = Instant.now();
            }
            initTime = lastStoredGenerationTime;
        } else {
            throw new IllegalArgumentException("Initialisation configuration " + configuration + " not supported");
        }
        this.maxLookBackTime = this.initTime.minusSeconds(configuration.getLookBackTime());
        // Now pre-load all definition states, according to the provided definitions
        preloadStates(definitions);
    }

    private void preloadStates(ProcessingDefinition defs) {
        List<Integer> parameterDefs = defs.getParameterDefinitions().stream().map(AbstractProcessingDefinition::getId).collect(Collectors.toList());
        List<Integer> eventDefs = defs.getEventDefinitions().stream().map(AbstractProcessingDefinition::getId).collect(Collectors.toList());
        List<Integer> actDefs = defs.getActivityDefinitions().stream().map(AbstractProcessingDefinition::getId).collect(Collectors.toList());
        // 1000 parameters per query
        int chunkSize = 1000;
        // Parameters
        preloadParameters(parameterDefs, chunkSize);
        // Events
        preloadEvents(eventDefs, chunkSize);
        // Activities
        preloadActivities(actDefs, chunkSize);
        // Done
    }

    private void preloadParameters(List<Integer> parameterDefs, int chunkSize) {
        int startIdx = 0;
        boolean isDone = false;
        // Parameters
        IParameterDataArchive arc = initArchive.getArchive(IParameterDataArchive.class);
        IAlarmParameterDataArchive arc2 = initArchive.getArchive(IAlarmParameterDataArchive.class);
        try {
            while (!isDone) {
                int maxIdx = Math.min(startIdx + chunkSize, parameterDefs.size());
                List<Integer> chunk = parameterDefs.subList(startIdx, maxIdx);
                List<ParameterData> paramData = arc.retrieve(initTime, new ParameterDataFilter(null, null, null, null, null, chunk), maxLookBackTime);
                List<AlarmParameterData> alarmData = arc2.retrieve(initTime, new AlarmParameterDataFilter(null, null, null, chunk), maxLookBackTime);
                // Add parameters first
                for (ParameterData pd : paramData) {
                    List<AbstractDataItem> paramList = new ArrayList<>(2);
                    paramList.add(pd);
                    parameter2state.put(pd.getExternalId(), paramList);
                }
                // Now add alarm data
                for (AlarmParameterData ad : alarmData) {
                    List<AbstractDataItem> paramList = parameter2state.get(ad.getExternalId());
                    // If there is no such parameter, then do nothing.
                    if (paramList != null) {
                        paramList.add(ad);
                    }
                }
                if (maxIdx == parameterDefs.size()) {
                    isDone = true;
                } else {
                    startIdx = maxIdx;
                }
                LOG.log(Level.INFO, "Parameter cycle - Initial states: " + parameter2state.size() + ", current retrieval " + maxIdx + "/" + parameterDefs.size());
            }
            LOG.log(Level.INFO, "Retrieved parameter initial states: " + parameter2state.size());
        } catch (ArchiveException e) {
            LOG.log(Level.SEVERE, "Cannot retrieve parameters from initialising archive: " + e.getMessage());
        }
    }

    private void preloadEvents(List<Integer> eventDefs, int chunkSize) {
        int startIdx = 0;
        boolean isDone = false;
        IEventDataArchive arc = initArchive.getArchive(IEventDataArchive.class);
        try {
            while (!isDone) {
                int maxIdx = Math.min(startIdx + chunkSize, eventDefs.size());
                List<Integer> chunk = eventDefs.subList(startIdx, maxIdx);
                List<EventData> eventData = arc.retrieve(initTime, new EventDataFilter(null, null, null, null, null, null, chunk), maxLookBackTime);
                // Add events
                for (EventData pd : eventData) {
                    List<AbstractDataItem> eventList = new ArrayList<>(1);
                    eventList.add(pd);
                    event2state.put(pd.getExternalId(), eventList);
                }
                if (maxIdx == eventDefs.size()) {
                    isDone = true;
                } else {
                    startIdx = maxIdx;
                }
            }
            LOG.log(Level.INFO, "Retrieved event initial states: " + event2state.size());
        } catch (ArchiveException e) {
            LOG.log(Level.SEVERE, "Cannot retrieve events from initialising archive: " + e.getMessage());
        }
    }

    private void preloadActivities(List<Integer> actDefs, int chunkSize) {
        int startIdx = 0;
        boolean isDone = false;
        IActivityOccurrenceDataArchive arc = initArchive.getArchive(IActivityOccurrenceDataArchive.class);
        try {
            while (!isDone) {
                int maxIdx = Math.min(startIdx + chunkSize, actDefs.size());
                List<Integer> chunk = actDefs.subList(startIdx, maxIdx);
                List<ActivityOccurrenceData> activities = arc.retrieve(initTime, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, chunk), maxLookBackTime);
                // Add activities
                for(ActivityOccurrenceData aod : activities) {
                    // Reports are sorted by generation time
                    ActivityOccurrenceData sanitized = aod;
                    if(!aod.getProgressReports().isEmpty()) {
                        // Remove old reports
                        sanitized = sanitize(aod);
                    }
                    // Check status
                    if(sanitized.getCurrentState() != ActivityOccurrenceState.COMPLETED) {
                        List<AbstractDataItem> items = activity2state.computeIfAbsent(sanitized.getExternalId(), o -> new LinkedList<>());
                        items.add(sanitized);
                    }
                }
                if (maxIdx == actDefs.size()) {
                    isDone = true;
                } else {
                    startIdx = maxIdx;
                }
            }
            LOG.log(Level.INFO, "Retrieved activity initial states: " + activity2state.size());
        } catch (ArchiveException e) {
            LOG.log(Level.SEVERE, "Cannot retrieve activities from initialising archive: " + e.getMessage());
        }
    }

    private ActivityOccurrenceData sanitize(ActivityOccurrenceData aod) {
        ActivityOccurrenceReport lastReport = aod.getProgressReports().get(aod.getProgressReports().size());
        if(lastReport.getGenerationTime().compareTo(initTime) > 0) {
            // At least one report is exceeding the init time
            List<ActivityOccurrenceReport> shrinkedReports = new LinkedList<>(aod.getProgressReports());
            shrinkedReports.removeIf(rep -> rep.getGenerationTime().compareTo(initTime) > 0);
            return new ActivityOccurrenceData(aod.getInternalId(), aod.getGenerationTime(), aod.getExtension(), aod.getExternalId(), aod.getName(), aod.getPath(), aod.getType(), aod.getArguments(), aod.getProperties(), shrinkedReports, aod.getRoute(), aod.getSource());
        } else {
            return aod;
        }
    }

    @Override
    public List<AbstractDataItem> getState(int externalId, SystemEntityType type) {
        switch(type) {
            case PARAMETER:
                return parameter2state.getOrDefault(externalId, Collections.emptyList());
            case EVENT:
                return event2state.getOrDefault(externalId, Collections.emptyList());
            case ACTIVITY:
                return activity2state.getOrDefault(externalId, Collections.emptyList());
            default:
                // Not supported, no state restored
                return Collections.emptyList();
        }
    }

    public void dispose() {
        parameter2state.clear();
        event2state.clear();
        activity2state.clear();
        if(externalArchive) {
            try {
                initArchive.dispose();
            } catch (ArchiveException e) {
                LOG.log(Level.SEVERE, "Cannot dispose archive initialiser: " + e.getMessage(), e);
            }
        }
    }
}
