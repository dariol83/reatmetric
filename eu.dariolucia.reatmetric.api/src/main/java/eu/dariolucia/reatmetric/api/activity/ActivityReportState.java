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
 * The state of a report is an indication of the current status of the related verification stage.
 */
public enum ActivityReportState {
    /**
     * The stage state is unknown and no better prediction can be done
     */
    UNKNOWN,
    /**
     * The stage should be concluded, but no confirmation of the stage was received yet
     */
    EXPECTED,
    /**
     * The stage is reported as currently open and the system is waiting for its confirmation
     */
    PENDING,
    /**
     * The timeout linked to the stage expired
     */
    TIMEOUT,
    /**
     * The stage is reported as successfully executed
     */
    OK,
    /**
     * The stage is reported as failed, but this failure is not fatal for the execution of the activity occurrence
     */
    FAIL,
    /**
     * The stage is reported as failed, the activity occurrence shall be considered completed
     */
    FATAL,
    /**
     * This specific state is reported in relation to a verification expression, linked to the {@link ActivityOccurrenceState#VERIFICATION}
     * when the expression cannot be evaluated
     */
    ERROR
}
