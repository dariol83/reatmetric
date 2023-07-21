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

/**
 * The conflict strategy to be adopted when an activity should start according to its scheduling
 * trigger, but it cannot start due to missing resources.
 *
 * If not started, an activity is always probed to start when the latest invocation time is reached.
 * If it cannot start by then, the activity is aborted.
 */
public enum ConflictStrategy {
    /**
     * Wait until the latest invocation time (if present) or indefinitely (if not present).
     * Start as soon as the resources are freed.
     */
    WAIT,
    /**
     * Do not invoke the activity and forget about its execution. The activity is basically skipped.
     */
    DO_NOT_START_AND_FORGET,
    /**
     * Abort ALL activities that are holding up the required resources, and then start the activity.
     */
    ABORT_OTHER_AND_START;
}
