/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.events.*;
import eu.dariolucia.reatmetric.api.parameters.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class EventDataAccessManager extends AbstractAccessManager<EventData, EventDataFilter, IEventDataSubscriber> implements IEventDataProvisionService {

    public EventDataAccessManager(IEventDataArchive archive) {
        super(archive);
    }

    @Override
    protected Class<? extends AbstractDataItem> getSupportedClass() {
        return EventData.class;
    }

    @Override
    protected String getName() {
        return "Event Access Manager";
    }

    @Override
    protected AbstractAccessSubscriber<EventData, EventDataFilter, IEventDataSubscriber> createSubscriber(IEventDataSubscriber subscriber, EventDataFilter filter, IProcessingModel model) {
        return new EventDataAccessSubscriber(subscriber, filter, model);
    }
}
