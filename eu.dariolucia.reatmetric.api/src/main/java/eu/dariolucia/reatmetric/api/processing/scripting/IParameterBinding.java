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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.parameters.Validity;

/**
 * Interface representing a binding to a parameter entity.
 * Extends IEntityBinding with parameter-specific metadata and values.
 */
public interface IParameterBinding extends IEntityBinding {

    /**
     * Returns the raw value of the parameter.
     *
     * @return the raw parameter value
     */
    Object rawValue();

    /**
     * Returns the engineering value of the parameter.
     *
     * @return the engineering parameter value
     */
    Object value();

    /**
     * Returns the alarm state of the parameter.
     *
     * @return the alarm state
     */
    AlarmState alarmState();

    /**
     * Returns whether the parameter is in alarm.
     *
     * @return true if the parameter is in alarm, false otherwise
     */
    boolean inAlarm();

    /**
     * Returns whether the parameter value is valid.
     *
     * @return true if the value is valid, false otherwise
     */
    boolean valid();

    /**
     * Returns the validity of the parameter value.
     *
     * @return the validity
     */
    Validity validity();

    /**
     * Returns the container ID of the parameter.
     *
     * @return the container ID, may be null
     */
    Long containerId();

    /**
     * Returns the route of the parameter.
     *
     * @return the route, may be null
     */
    String route();

}
