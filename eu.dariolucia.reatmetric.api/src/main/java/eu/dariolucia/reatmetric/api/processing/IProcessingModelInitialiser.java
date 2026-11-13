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

package eu.dariolucia.reatmetric.api.processing;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.util.List;

/**
 * Interface for initializing the state of a processing model.
 * Implementations provide initial state for system entities.
 */
public interface IProcessingModelInitialiser {

    /**
     * Returns the initial state for a system entity.
     *
     * @param externalId the external ID of the system entity
     * @param type the type of the system entity
     * @return the list of data items representing the initial state
     * @throws ReatmetricException if the initial state cannot be retrieved
     */
    List<AbstractDataItem> getState(int externalId, SystemEntityType type) throws ReatmetricException;

}
