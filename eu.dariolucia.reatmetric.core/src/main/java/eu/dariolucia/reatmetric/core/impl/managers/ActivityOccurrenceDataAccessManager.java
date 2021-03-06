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

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.util.Objects;

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
        if(filter == null) {
            filter = new ActivityOccurrenceDataFilter(null, null, null, null, null, null, null);
        }
        return new ActivityOccurrenceDataAccessSubscriber(this, subscriber, filter, model);
    }

    @Override
    public ActivityDescriptor getDescriptor(SystemEntityPath path) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(path);
        if(descriptor instanceof ActivityDescriptor) {
            return (ActivityDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided activity path " + path + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }

    @Override
    public ActivityDescriptor getDescriptor(int externalId) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(externalId);
        if(descriptor instanceof ActivityDescriptor) {
            return (ActivityDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided activity ID " + externalId + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }
}
