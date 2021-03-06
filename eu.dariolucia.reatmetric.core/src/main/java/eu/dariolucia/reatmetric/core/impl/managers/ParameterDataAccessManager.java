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
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.util.Objects;

public class ParameterDataAccessManager extends AbstractAccessManager<ParameterData, ParameterDataFilter, IParameterDataSubscriber> implements IParameterDataProvisionService {

    public ParameterDataAccessManager(IParameterDataArchive archive) {
        super(archive);
    }

    @Override
    protected Class<? extends AbstractDataItem> getSupportedClass() {
        return ParameterData.class;
    }

    @Override
    protected String getName() {
        return "Parameter Access Manager";
    }

    @Override
    protected AbstractAccessSubscriber<ParameterData, ParameterDataFilter, IParameterDataSubscriber> createSubscriber(IParameterDataSubscriber subscriber, ParameterDataFilter filter, IProcessingModel model) {
        if(filter == null) {
            filter = new ParameterDataFilter(null, null, null, null, null, null);
        }
        return new ParameterDataAccessSubscriber(this, subscriber, filter, model);
    }

    @Override
    public ParameterDescriptor getDescriptor(SystemEntityPath path) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(path);
        if(descriptor instanceof ParameterDescriptor) {
            return (ParameterDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided parameter path " + path + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }

    @Override
    public ParameterDescriptor getDescriptor(int externalId) throws ReatmetricException {
        AbstractSystemEntityDescriptor descriptor = super.model.getDescriptorOf(externalId);
        if(descriptor instanceof ParameterDescriptor) {
            return (ParameterDescriptor) descriptor;
        } else {
            throw new ReatmetricException("Descriptor of provided parameter ID " + externalId + " cannot be retrieved. Found: " + Objects.requireNonNullElse(descriptor, "<not found>"));
        }
    }
}
