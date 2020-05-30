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
 * This enumeration defines the possible states of an activity occurrence lifecycle. The lifecycle has the following
 * two strict rules:
 * <ul>
 * <li>Lifecycle is progressive and cannot be rolled back (i.e. states have a total ordering)</li>
 * <li>States can be skipped</li>
 * </ul>
 * Transition from one state to the other is driven by the processing of {@link eu.dariolucia.reatmetric.api.processing.input.ActivityProgress}
 * indications, each of them generating the related {@link ActivityOccurrenceReport} information.
 */
public enum ActivityOccurrenceState {
    /**
     * The initial state of an activity occurrence once its execution is requested to the ReatMetric system. Typically,
     * the activity occurrence remains in this state until it is provided to the identified {@link eu.dariolucia.reatmetric.api.processing.IActivityHandler}
     * for transmission/implementation.
     */
    CREATION("Creation"),
    /**
     * The state of an activity occurrence while it is under processing by the selected {@link eu.dariolucia.reatmetric.api.processing.IActivityHandler}.
     */
    RELEASE("Release"),
    /**
     * The state of an activity occurrence while it is under transmission to reach the controlled system for its final execution. If
     * the activity occurrence is implemented within the ReatMetric system, this state could be missing and a direct transition
     * to the EXECUTION state can be expected.
     */
    TRANSMISSION("Transmission"),
    /**
     * The state of an activity occurrence when it reached the controlled system but it was scheduled for later execution.
     * If remote scheduling is not supported (i.e. immediate execution is expected/exclusively supported), this state
     * could be missing.
     */
    SCHEDULING("Scheduling"),
    /**
     * The state of an activity occurrence under execution. After its completion, it is mandatory that the transition to
     * the state VERIFICATION is reported by the last {@link eu.dariolucia.reatmetric.api.processing.input.ActivityProgress}
     * reported for this state.
     */
    EXECUTION("Execution"),
    /**
     * The state of an activity occurrence when the processing model is performing the post-execution verification. This
     * state is reached only if a post-execution verification expression is defined in the processing model for the activity
     * definition.
     */
    VERIFICATION("Verification"),
    /**
     * The state of an activity occurrence when its lifecycle is completed. The {@link ActivityOccurrenceState} in the
     * processing model is no longer updated and the activity occurrence is removed from the processing model.
     */
    COMPLETED("Completed");

    String formatString;

    ActivityOccurrenceState(String formatString) {
        this.formatString = formatString;
    }

    /**
     * The human readable format for the state.
     *
     * @return a human readable string
     */
    public String getFormatString() {
        return formatString;
    }
}
