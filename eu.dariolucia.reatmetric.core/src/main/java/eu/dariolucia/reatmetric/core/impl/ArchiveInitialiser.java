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

import java.time.Instant;
import java.util.*;

public class ArchiveInitialiser implements IProcessingModelInitialiser {

    private final Instant maxLookBackTime;
    private final Instant initTime;
    private final IArchive initArchive;
    private final boolean externalArchive;

    public ArchiveInitialiser(IArchive processingArchive, AbstractInitialisationConfiguration configuration) throws ReatmetricException {
        if(configuration instanceof TimeInitialisationConfiguration) {
            this.externalArchive = true;
            this.initTime = ((TimeInitialisationConfiguration) configuration).getTime().toInstant();
            ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
            if (archiveLoader.findFirst().isPresent()) {
                initArchive = archiveLoader.findFirst().get().buildArchive(((TimeInitialisationConfiguration) configuration).getArchiveLocation());
                initArchive.connect();
            } else {
                throw new ReatmetricException("Initialisation archive configured to " + ((TimeInitialisationConfiguration) configuration).getArchiveLocation() + ", but no archive factory deployed");
            }
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
    }

    @Override
    public List<AbstractDataItem> getState(int externalId, SystemEntityType type) throws ReatmetricException {
        switch(type) {
            case PARAMETER:
                return retrieveParameterState(externalId);
            case EVENT:
                return retrieveEventState(externalId);
            case ACTIVITY:
                return retrieveActivityOccurranceStates(externalId);
            default:
                // Not supported, no state restored
                return Collections.emptyList();
        }
    }

    private List<AbstractDataItem> retrieveActivityOccurranceStates(int externalId) throws ArchiveException {
        IActivityOccurrenceDataArchive arc = initArchive.getArchive(IActivityOccurrenceDataArchive.class);
        List<ActivityOccurrenceData> activities = arc.retrieve(initTime, new ActivityOccurrenceDataFilter(null, null, null, null, null, Collections.singletonList(externalId)), maxLookBackTime);
        // Post processing: for each activity occurrence, remove the reports having verification time > initTime and rebuild the activity occurrence object
        List<AbstractDataItem> toReturn = new ArrayList<>(activities.size());
        for(ActivityOccurrenceData aod : activities) {
            // Reports are sorted by generation time
            ActivityOccurrenceData sanitized = aod;
            if(!aod.getProgressReports().isEmpty()) {
                // Remove old reports
                sanitized = sanitize(aod);
            }
            // Check status
            if(sanitized.getCurrentState() != ActivityOccurrenceState.COMPLETION) {
                toReturn.add(sanitized);
            }
        }
        return toReturn;
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

    private List<AbstractDataItem> retrieveParameterState(int externalId) throws ArchiveException {
        IParameterDataArchive arc = initArchive.getArchive(IParameterDataArchive.class);
        List<ParameterData> params = arc.retrieve(initTime, new ParameterDataFilter(null, null, null, null, null, Collections.singletonList(externalId)), maxLookBackTime);
        IAlarmParameterDataArchive arc2 = initArchive.getArchive(IAlarmParameterDataArchive.class);
        List<AlarmParameterData> alarms = arc2.retrieve(initTime, new AlarmParameterDataFilter(null, null, null, Collections.singletonList(externalId)), maxLookBackTime);
        List<AbstractDataItem> toReturn = new ArrayList<>();
        if(params.size() > 0) {
            toReturn.add(params.get(0));
            if(alarms.size() > 0) {
                toReturn.add(alarms.get(0));
            }
        }
        return toReturn;
    }

    private List<AbstractDataItem> retrieveEventState(int externalId) throws ArchiveException {
        IEventDataArchive arc = initArchive.getArchive(IEventDataArchive.class);
        List<EventData> events = arc.retrieve(initTime, new EventDataFilter(null, null, null, null, null, null, Collections.singletonList(externalId)), maxLookBackTime);
        List<AbstractDataItem> toReturn = new ArrayList<>();
        if(events.size() > 0) {
            toReturn.add(events.get(0));
        }
        return toReturn;
    }

    public void dispose() {
        if(externalArchive) {
            try {
                initArchive.dispose();
            } catch (ArchiveException e) {
                // TODO log
                e.printStackTrace();
            }
        }
    }
}
