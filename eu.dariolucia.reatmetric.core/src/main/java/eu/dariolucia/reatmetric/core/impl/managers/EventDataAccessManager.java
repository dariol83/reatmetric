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
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.*;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import org.w3c.dom.events.Event;

import java.util.Objects;

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
        if(filter == null) {
            filter = new EventDataFilter(null, null, null, null, null, null, null);
        }
        return new EventDataAccessSubscriber(subscriber, filter, model);
    }

    @Override
    public EventDescriptor getDescriptor(SystemEntityPath path) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(path);
        if(descriptor instanceof EventDescriptor) {
            return (EventDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided event path " + path + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }

    @Override
    public EventDescriptor getDescriptor(int externalId) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(externalId);
        if(descriptor instanceof EventDescriptor) {
            return (EventDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided event ID " + externalId + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }
}
