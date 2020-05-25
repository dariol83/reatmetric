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

package eu.dariolucia.reatmetric.api.activity;

import java.util.List;

/**
 * The descriptor of an activity argument array.
 *
 * Objects of this class are immutable.
 */
public class ActivityArrayArgumentDescriptor extends AbstractActivityArgumentDescriptor {

    private final String expansionArgument;
    private final List<AbstractActivityArgumentDescriptor> elements;

    /**
     * Constructor of an array argument descriptor.
     *
     * @param name the name of the argument
     * @param description the description of the argument
     * @param expansionArgument the name of the argument containing the number of array records
     * @param elements the descriptors of the elements composing one record
     */
    public ActivityArrayArgumentDescriptor(String name, String description, String expansionArgument, List<AbstractActivityArgumentDescriptor> elements) {
        super(name, description);
        this.elements = List.copyOf(elements);
        this.expansionArgument = expansionArgument;
    }

    /**
     * Return the name of the expansion argument, i.e. the argument whose actual value is the number of repetitions of the array elements (i.e. the records).
     *
     * @return the name of the expansion argument
     */
    public String getExpansionArgument() {
        return expansionArgument;
    }

    /**
     * Return the list of descriptors defining one array record.
     *
     * @return the list of element descriptors
     */
    public List<AbstractActivityArgumentDescriptor> getElements() {
        return elements;
    }
}
