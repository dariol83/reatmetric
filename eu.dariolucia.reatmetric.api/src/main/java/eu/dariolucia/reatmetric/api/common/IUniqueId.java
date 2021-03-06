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


package eu.dariolucia.reatmetric.api.common;

import java.io.Serializable;

/**
 * This interface is used to represent a unique identifier (typically type-specific).
 */
public interface IUniqueId extends Serializable {

    boolean equals(Object o);

    int hashCode();

    String toString();

    /**
     * Return a long representation of the identifier.
     *
     * @return the long representation of the identifier
     */
    default long asLong() {
        return hashCode();
    }
}
