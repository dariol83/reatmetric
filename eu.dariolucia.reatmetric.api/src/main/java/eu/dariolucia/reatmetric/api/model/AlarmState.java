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
 * This enumeration represents the possible alarm states that checks and connectors can deliver.
 */
public enum AlarmState {
    /**
     * An alarm state, for which immediate intervention is required.
     */
    ALARM(true, "Alarm"),
    /**
     * A warning state, for which an immediate check is advised to avoid issues
     * in the future.
     */
    WARNING(true, "Warning"),
    /**
     * A violated state, meaning that a specific check found a non-compliant condition, but the number of detected
     * violations is not enough to raise a warning or an alarm.
     */
    VIOLATED(false, "Violated"),
    /**
     * The function responsible to evaluate the alarm state could not complete due to a software/configuration error.
     */
    ERROR(true, "Error"),
    /**
     * The nominal state: the alarm state condition was evaluated and everything was found in nominal conditions.
     */
    NOMINAL(false, "Nominal"),
    /**
     * The alarm state condition is not present or could not be run due to the current state of the related object.
     */
    NOT_CHECKED(false, "Unchecked"),
    /**
     * The alarm state condition, whether present or not, or evaluated or not, has been explicitly masked, and it is ignored.
     */
    IGNORED(false, "Ignored"),
    /**
     * The alarm state is not applicable for the related object.
     */
    NOT_APPLICABLE(false, "N/A"),
    /**
     * The system is not capable to derive any alarm state information for the related object.
     */
    UNKNOWN(false, "Unknown");

    private final boolean alarm;
    private final String printableName;

    AlarmState(boolean alarm, String printableName) {
        this.alarm = alarm;
        this.printableName = printableName;
    }

    public boolean isAlarm() {
        return alarm;
    }

    public String getPrintableName() {
        return printableName;
    }
}
