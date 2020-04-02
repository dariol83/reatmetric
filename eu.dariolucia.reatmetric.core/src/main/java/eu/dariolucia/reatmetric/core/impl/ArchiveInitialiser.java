/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;

import java.util.*;

public class ArchiveInitialiser implements IProcessingModelInitialiser {

    private final Date initTime;
    private final IArchive initArchive;

    public ArchiveInitialiser(Date time, String archiveLocation) throws ReatmetricException {
        this.initTime = time;
        ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
        if(archiveLoader.findFirst().isPresent()) {
            initArchive = archiveLoader.findFirst().get().buildArchive(archiveLocation);
            initArchive.connect();
        } else {
            throw new ReatmetricException("Initialisation archive configured to " + archiveLocation + ", but no archive factory deployed");
        }
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
        List<ActivityOccurrenceData> activities = arc.retrieve(initTime.toInstant(), new ActivityOccurrenceDataFilter(null, null, null, null, null, Collections.singletonList(externalId)), null);
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
        if(lastReport.getGenerationTime().compareTo(initTime.toInstant()) > 0) {
            // At least one report is exceeding the init time
            List<ActivityOccurrenceReport> shrinkedReports = new LinkedList<>(aod.getProgressReports());
            shrinkedReports.removeIf(rep -> rep.getGenerationTime().compareTo(initTime.toInstant()) > 0);
            return new ActivityOccurrenceData(aod.getInternalId(), aod.getGenerationTime(), aod.getExtension(), aod.getExternalId(), aod.getName(), aod.getPath(), aod.getType(), aod.getArguments(), aod.getProperties(), shrinkedReports, aod.getRoute(), aod.getSource());
        } else {
            return aod;
        }
    }

    private List<AbstractDataItem> retrieveParameterState(int externalId) throws ArchiveException {
        IParameterDataArchive arc = initArchive.getArchive(IParameterDataArchive.class);
        List<ParameterData> params = arc.retrieve(initTime.toInstant(), new ParameterDataFilter(null, null, null, null, null, Collections.singletonList(externalId)), null);
        IAlarmParameterDataArchive arc2 = initArchive.getArchive(IAlarmParameterDataArchive.class);
        List<AlarmParameterData> alarms = arc2.retrieve(initTime.toInstant(), new AlarmParameterDataFilter(null, null, null, Collections.singletonList(externalId)), null);
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
        List<EventData> events = arc.retrieve(initTime.toInstant(), new EventDataFilter(null, null, null, null, null, null, Collections.singletonList(externalId)), null);
        List<AbstractDataItem> toReturn = new ArrayList<>();
        if(events.size() > 0) {
            toReturn.add(events.get(0));
        }
        return toReturn;
    }
}
