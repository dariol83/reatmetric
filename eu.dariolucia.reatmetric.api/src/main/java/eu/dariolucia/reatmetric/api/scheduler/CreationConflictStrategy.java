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

package eu.dariolucia.reatmetric.api.scheduler;

public enum CreationConflictStrategy {
    /**
     * Abort the complete operation if there is a resource conflict: the schedule is unmodified
     */
    ABORT,
    /**
     * Do not add the new activity if a resource conflict exists
     */
    SKIP_NEW,
    /**
     * Remove the resource-conflicting scheduled items before adding the new activity
     */
    REMOVE_PREVIOUS,
    /**
     * Add the activity anyway, with the risk of having problems later
     */
    ADD_ANYWAY
}
