/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.impl.managers;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceDataFilter;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataSubscriber;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.UniqueItem;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class ActivityOccurrenceDataAccessSubscriber extends AbstractAccessSubscriber<ActivityOccurrenceData, ActivityOccurrenceDataFilter, IActivityOccurrenceDataSubscriber> {

    private static final IUniqueId INVALID_ID = new LongUniqueId(0xFFFFFFFFFFFFFFFFL);

    public ActivityOccurrenceDataAccessSubscriber(IActivityOccurrenceDataSubscriber subscriber, ActivityOccurrenceDataFilter filter, IProcessingModel model) {
        super(subscriber, filter, model);
    }

    @Override
    protected Pair<Integer, Long> computeId(ActivityOccurrenceData item) {
        return Pair.of(item.getExternalId(), item.getInternalId().asLong());
    }

    @Override
    protected IUniqueId computeUniqueCounter(ActivityOccurrenceData pd) {
        return pd.getProgressReports().stream().max((a,b) -> Long.compare(a.getInternalId().asLong(), b.getInternalId().asLong())).map(UniqueItem::getInternalId).orElse(INVALID_ID);
    }

    @Override
    protected String getName() {
        return "Activity Occurrence Access Subscriber";
    }

}
