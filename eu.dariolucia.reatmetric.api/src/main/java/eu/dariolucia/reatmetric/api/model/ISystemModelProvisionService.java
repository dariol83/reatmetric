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


package eu.dariolucia.reatmetric.api.model;

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;

import java.util.List;

/**
 *
 * @author dario
 */
public interface ISystemModelProvisionService {

    void subscribe(ISystemModelSubscriber subscriber);
    
    void unsubscribe(ISystemModelSubscriber subscriber);
    
    SystemEntity getRoot() throws ReatmetricException;
    
    List<SystemEntity> getContainedEntities(SystemEntityPath se) throws ReatmetricException;
    
    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ReatmetricException;

    SystemEntity getSystemEntityOf(int externalId) throws ReatmetricException;

    int getExternalIdOf(SystemEntityPath path) throws ReatmetricException;

    SystemEntityPath getPathOf(int externalId) throws ReatmetricException;

    void enable(SystemEntityPath path) throws ReatmetricException;

    void disable(SystemEntityPath path) throws ReatmetricException;

    AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ReatmetricException;

    AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ReatmetricException;

}
