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

package eu.dariolucia.reatmetric.api.processing.input;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;

public final class ActivityProgress extends AbstractInputDataItem {

    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, null, status, state, null);
    }

    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, Instant executionTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, executionTime, status, state, null);
    }

    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, ActivityOccurrenceState nextState, Object result) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, executionTime, status, nextState, result);
    }

    private final int activityId;
    private final IUniqueId occurrenceId;
    private final String name;
    private final Instant generationTime;
    private final ActivityOccurrenceState state;
    private final Instant executionTime; // If not null, this report provides the activity occurrence execution time (estimated or final)
    private final ActivityReportState status;
    private final ActivityOccurrenceState nextState;
    private final Object result; // If not null, this report provides the activity occurrence execution result (partial or final)

    private ActivityProgress(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, ActivityOccurrenceState nextState, Object result) {
        this.activityId = activityId;
        this.occurrenceId = occurrenceId;
        this.name = name;
        this.generationTime = generationTime;
        this.state = state;
        this.executionTime = executionTime;
        this.status = status;
        this.nextState = nextState;
        this.result = result;
    }

    public int getActivityId() {
        return activityId;
    }

    public IUniqueId getOccurrenceId() {
        return occurrenceId;
    }

    public String getName() {
        return name;
    }

    public Instant getGenerationTime() {
        return generationTime;
    }

    public ActivityOccurrenceState getState() {
        return state;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public ActivityReportState getStatus() {
        return status;
    }

    public ActivityOccurrenceState getNextState() {
        return nextState;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ActivityProgress{" +
                "activityId=" + activityId +
                ", occurrenceId=" + occurrenceId +
                ", name='" + name + '\'' +
                ", generationTime=" + generationTime +
                ", state=" + state +
                ", executionTime=" + executionTime +
                ", status=" + status +
                ", nextState=" + nextState +
                ", result=" + result +
                "}";
    }
}
