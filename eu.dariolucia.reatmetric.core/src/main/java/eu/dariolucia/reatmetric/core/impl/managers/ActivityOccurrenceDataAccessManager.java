/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class ActivityOccurrenceDataAccessManager extends AbstractAccessManager<ActivityOccurrenceData, ActivityOccurrenceDataFilter, IActivityOccurrenceDataSubscriber> implements IActivityOccurrenceDataProvisionService {

    public ActivityOccurrenceDataAccessManager(IActivityOccurrenceDataArchive archive) {
        super(archive);
    }

    @Override
    protected Class<? extends AbstractDataItem> getSupportedClass() {
        return ActivityOccurrenceData.class;
    }

    @Override
    protected String getName() {
        return "Activity Occurrence Access Manager";
    }

    @Override
    protected AbstractAccessSubscriber<ActivityOccurrenceData, ActivityOccurrenceDataFilter, IActivityOccurrenceDataSubscriber> createSubscriber(IActivityOccurrenceDataSubscriber subscriber, ActivityOccurrenceDataFilter filter, IProcessingModel model) {
        return new ActivityOccurrenceDataAccessSubscriber(subscriber, filter, model);
    }
}
