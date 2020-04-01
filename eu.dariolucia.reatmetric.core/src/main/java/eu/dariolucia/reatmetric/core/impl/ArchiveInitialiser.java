/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ServiceLoader;

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
    public List<AbstractDataItem> getState(int externalId, SystemEntityType type) {
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

    private List<AbstractDataItem> retrieveEventState(int externalId) {
        IEventDataArchive arc = initArchive.getArchive(IEventDataArchive.class);
        return arc.retrieve(Instant.ofEpochMilli(initTime.getTime()), externalId, null); // TODO: need direct access retrieval
    }
}
