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

/**
 * The descriptor of an activity argument.
 *
 * Objects of this class are immutable.
 */
public class AbstractActivityArgumentDescriptor {

    private final String name;
    private final String description;

    /**
     * Constructor of the activity argument descriptor.
     *
     * @param name the name of the argument
     * @param description the description of the argument
     */
    public AbstractActivityArgumentDescriptor(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Return the name of the argument.
     *
     * @return the name of the argument
     */
    public String getName() {
        return name;
    }

    /**
     * Return the description of the argument.
     *
     * @return the description of the argument
     */
    public String getDescription() {
        return description;
    }
}
