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
