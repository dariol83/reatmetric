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

import java.util.Objects;

/**
 * The standard implementation of the {@link IUniqueId} interface in the ReatMetric system.
 *
 * This class is immutable.
 */
public class LongUniqueId implements IUniqueId {

    /**
     * The unique identifier value.
     */
    private final long id;

    /**
     * Constructor for a LongUniqueId.
     *
     * @param id the unique identifier value
     */
    public LongUniqueId(long id) {
        this.id = id;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LongUniqueId that = (LongUniqueId) o;
        return id == that.id;
    }

    /**
     * Returns a hash code value for this unique ID.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of this unique ID.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return String.valueOf(id);
    }

    /**
     * Returns the long representation of this identifier.
     *
     * @return the long value of this identifier
     */
    @Override
    public long asLong() {
        return id;
    }
}
