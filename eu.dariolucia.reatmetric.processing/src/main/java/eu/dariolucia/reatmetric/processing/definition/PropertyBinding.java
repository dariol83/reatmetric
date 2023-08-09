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

package eu.dariolucia.reatmetric.processing.definition;

/**
 * This enumeration describes the type of property that shall be bound to {@link SymbolDefinition}.
 */
public enum PropertyBinding {
    /**
     * The location of the system entity (as string)
     */
    PATH,
    /**
     * The generation time (as Java {@link java.time.Instant})
     */
    GEN_TIME,
    /**
     * The reception time (as Java {@link java.time.Instant})
     */
    RCT_TIME,
    /**
     * The route (as string)
     */
    ROUTE,
    /**
     * The raw/source value (only for parameters)
     */
    SOURCE_VALUE,
    /**
     * The engineering value (only for parameters)
     */
    ENG_VALUE,
    /**
     * The alarm state (as {@link eu.dariolucia.reatmetric.api.model.AlarmState}, only for parameters)
     */
    ALARM_STATE,
    /**
     * The validity (as {@link eu.dariolucia.reatmetric.api.parameters.Validity}, only for parameters)
     */
    VALIDITY,
    /**
     * The qualifier (as string, only for events)
     */
    QUALIFIER,
    /**
     * The source (as string)
     */
    SOURCE,
    /**
     * The bound object (i.e. {@link eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding} for parameters,
     * {@link eu.dariolucia.reatmetric.api.processing.scripting.IEventBinding} for events
     */
    OBJECT
}
