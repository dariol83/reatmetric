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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

public class EventDataAccessSubscriber extends AbstractAccessSubscriber<EventData, EventDataFilter, IEventDataSubscriber> {

    public EventDataAccessSubscriber(EventDataAccessManager manager, IEventDataSubscriber subscriber, EventDataFilter filter, IProcessingModel model) {
        super(manager, subscriber, filter, model);
    }

    @Override
    protected Pair<Integer, Long> computeId(EventData item) {
        return Pair.of(item.getExternalId(), 0L);
    }

    @Override
    protected String getName() {
        return "Event Access Subscriber";
    }

}
