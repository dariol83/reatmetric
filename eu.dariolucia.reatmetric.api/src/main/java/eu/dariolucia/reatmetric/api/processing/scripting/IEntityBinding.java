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

package eu.dariolucia.reatmetric.api.processing.scripting;

import java.time.Instant;

/**
 * Interface representing a binding to a system entity.
 * Provides access to entity metadata such as ID, path, and timestamps.
 */
public interface IEntityBinding {

    /**
     * Returns the ID of the entity.
     *
     * @return the entity ID
     */
    long id();

    /**
     * Returns the path of the entity.
     *
     * @return the entity path
     */
    String path();

    /**
     * Returns the generation time of the entity.
     *
     * @return the generation time
     */
    Instant generationTime();

    /**
     * Returns the reception time of the entity.
     *
     * @return the reception time
     */
    Instant receptionTime();
}
