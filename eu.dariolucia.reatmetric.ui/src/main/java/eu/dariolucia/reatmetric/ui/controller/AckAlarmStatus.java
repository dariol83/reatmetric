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

package eu.dariolucia.reatmetric.ui.controller;

import eu.dariolucia.reatmetric.api.model.AlarmState;

/**
 * This enumeration is used to identify items in non nominal state, with and without acknowledgement.
 */
public enum AckAlarmStatus {
    NOMINAL(AlarmState.NOMINAL, true),
    NOMINAL_NOT_ACKED(AlarmState.NOMINAL, false),
    WARNING_ACKED(AlarmState.WARNING, true),
    WARNING_NOT_ACKED(AlarmState.WARNING, false),
    ALARM_ACKED(AlarmState.ALARM, true),
    ALARM_NOT_ACKED(AlarmState.ALARM, false);

    private final AlarmState state;
    private final boolean acked;

    AckAlarmStatus(AlarmState state, boolean acked) {
        this.state = state;
        this.acked = acked;
    }

    public static AckAlarmStatus deriveStatus(AlarmState alarmState, boolean pendingAcknowledgement) {
        switch(alarmState) {
            case ERROR:
            case ALARM:
                return pendingAcknowledgement ? ALARM_NOT_ACKED : ALARM_ACKED;
            case WARNING:
                return pendingAcknowledgement ? WARNING_NOT_ACKED : WARNING_ACKED;
            case NOMINAL:
            case NOT_APPLICABLE:
            case NOT_CHECKED:
            case UNKNOWN:
            case VIOLATED:
                return pendingAcknowledgement ? NOMINAL_NOT_ACKED : NOMINAL;
            default:
                throw new IllegalArgumentException("Alarm state " + alarmState + " not supported");
        }
    }

    public static AckAlarmStatus merge(AckAlarmStatus result, AckAlarmStatus ackAlarmStatus) {
        boolean isAcked = result.acked && ackAlarmStatus.acked;
        AlarmState merged = result.state.ordinal() < ackAlarmStatus.state.ordinal() ? result.state : ackAlarmStatus.state;
        return deriveStatus(merged, !isAcked);
    }
}
