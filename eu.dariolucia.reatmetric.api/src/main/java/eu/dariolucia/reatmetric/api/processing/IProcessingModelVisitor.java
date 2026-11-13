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
import eu.dariolucia.reatmetric.api.model.SystemEntity;

/**
 * Interface for visiting the elements of a processing model.
 * Implementations can traverse the model and receive notifications for system entities and data items.
 */
public interface IProcessingModelVisitor {

    /**
     * Determines whether the visitor should descend into the specified system entity.
     *
     * @param path the system entity to potentially descend into
     * @return true if the visitor should descend, false otherwise
     */
    boolean shouldDescend(SystemEntity path);

    /**
     * Called when starting to visit a system entity.
     *
     * @param path the system entity being visited
     */
    void startVisit(SystemEntity path);

    /**
     * Called when visiting a data item.
     *
     * @param item the data item being visited
     */
    void onVisit(AbstractDataItem item);

    /**
     * Called when finishing the visit of a system entity.
     *
     * @param path the system entity that was visited
     */
    void endVisit(SystemEntity path);

}
