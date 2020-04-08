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
 *
 * @author dario
 */
public enum AlarmState {
    ALARM(true, "Alarm"),
    WARNING(true, "Warning"),
    VIOLATED(false, "Violated"),
    ERROR(true, "Error"),
    NOMINAL(false, "Nominal"),
    NOT_CHECKED(false, "Unchecked"),
    NOT_APPLICABLE(false, "N/A"),
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
