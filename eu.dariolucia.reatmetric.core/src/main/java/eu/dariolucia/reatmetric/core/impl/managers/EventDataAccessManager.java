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
