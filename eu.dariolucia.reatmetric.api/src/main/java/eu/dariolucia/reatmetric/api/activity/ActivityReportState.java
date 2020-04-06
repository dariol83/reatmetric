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

public enum ActivityReportState {
    EXPECTED, // The algorithm expects this stage to have happened, no confirmation of the stage received yet
    PENDING, // The algorithm is waiting for the confirmation of the stage
    OK, // The algorithm received confirmation that the stage was OK
    TIMEOUT, // The timeout linked to the activity phase expired
    FAIL, // The algorithm received confirmation that the stage was failed, but the activity might still proceed
    FATAL, // The algorithm received confirmation that the stage was failed, the activity shall be considered completed
    ERROR, // The verification expression failed its evaluation, the activity is completed
    UNKNOWN // The algorithm has no clue of what happened to this stage
}
