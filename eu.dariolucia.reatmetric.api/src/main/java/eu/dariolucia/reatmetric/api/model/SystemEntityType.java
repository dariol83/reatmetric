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

/**
 * Enumeration of system entity types in the ReatMetric system.
 * System entities represent the different types of elements that can be monitored and controlled.
 *
 * @author dario
 */
public enum SystemEntityType {
    /**
     * A container entity that can contain other system entities.
     */
    CONTAINER,
    
    /**
     * A parameter entity that represents a telemetry or control parameter.
     */
    PARAMETER,
    
    /**
     * An event entity that represents an event occurrence.
     */
    EVENT,
    
    /**
     * An activity entity that represents an activity that can be executed.
     */
    ACTIVITY,
    
    /**
     * A report entity for future evolution (to be assessed).
     */
    REPORT
}
