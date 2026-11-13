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

/**
 * Abstract base class for activity arguments.
 * Activity arguments are used to pass data to activities during their execution.
 */
public abstract class AbstractActivityArgument implements Serializable {

    /**
     * The name of the activity argument.
     */
    private final String name;

    /**
     * Constructor for an activity argument.
     *
     * @param name the name of the argument, cannot be null
     */
    public AbstractActivityArgument(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the activity argument.
     *
     * @return the name (cannot be null)
     */
    public String getName() {
        return name;
    }
}
