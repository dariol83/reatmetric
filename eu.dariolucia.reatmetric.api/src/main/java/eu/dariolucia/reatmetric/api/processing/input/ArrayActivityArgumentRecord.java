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

package eu.dariolucia.reatmetric.api.processing.input;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents a record in a {@link ArrayActivityArgument}. This is basically a single instantiation of the defined group.
 */
public class ArrayActivityArgumentRecord implements Serializable {

    /** The list of activity arguments in this record. */
    private final List<AbstractActivityArgument> elements;

    /**
     * Constructs a new ArrayActivityArgumentRecord.
     *
     * @param elements the list of activity arguments
     */
    public ArrayActivityArgumentRecord(List<AbstractActivityArgument> elements) {
        this.elements = List.copyOf(elements);
    }

    /**
     * Returns an unmodifiable list of the activity arguments in this record.
     *
     * @return the list of activity arguments
     */
    public List<AbstractActivityArgument> getElements() {
        return elements;
    }

}
