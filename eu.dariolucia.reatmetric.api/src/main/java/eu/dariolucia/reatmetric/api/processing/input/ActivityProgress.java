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

/**
 * Represents the progress of an activity occurrence execution.
 * This class provides information about the current state, status, and execution details of an activity.
 */
public final class ActivityProgress extends AbstractInputDataItem {

    /**
     * Creates an ActivityProgress with the minimum required information.
     *
     * @param activityId the ID of the activity
     * @param occurrenceId the unique identifier of the activity occurrence, cannot be null
     * @param name the name of the activity
     * @param generationTime the generation time of the progress report, cannot be null
     * @param state the current state of the activity occurrence, cannot be null
     * @param status the report status, cannot be null
     * @return a new ActivityProgress instance
     */
    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, null, status, state, null);
    }

    /**
     * Creates an ActivityProgress with execution time information.
     *
     * @param activityId the ID of the activity
     * @param occurrenceId the unique identifier of the activity occurrence, cannot be null
     * @param name the name of the activity
     * @param generationTime the generation time of the progress report, cannot be null
     * @param executionTime the execution time (estimated or final)
     * @param state the current state of the activity occurrence, cannot be null
     * @param status the report status, cannot be null
     * @return a new ActivityProgress instance
     */
    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, Instant executionTime, ActivityOccurrenceState state, ActivityReportState status) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, executionTime, status, state, null);
    }

    /**
     * Creates an ActivityProgress with all available information.
     *
     * @param activityId the ID of the activity
     * @param occurrenceId the unique identifier of the activity occurrence, cannot be null
     * @param name the name of the activity
     * @param generationTime the generation time of the progress report, cannot be null
     * @param state the current state of the activity occurrence, cannot be null
     * @param executionTime the execution time (estimated or final), can be null
     * @param status the report status, cannot be null
     * @param nextState the next state to transition to, can be null
     * @param result the execution result (partial or final), can be null
     * @return a new ActivityProgress instance
     */
    public static ActivityProgress of(int activityId, IUniqueId occurrenceId, String name, Instant generationTime, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, ActivityOccurrenceState nextState, Object result) {
        return new ActivityProgress(activityId, occurrenceId, name, generationTime, state, executionTime, status, nextState, result);
    }

    /**
     * The ID of the activity.
     */
    private final int activityId;
    
    /**
     * The unique identifier of the activity occurrence.
     */
    private final IUniqueId occurrenceId;
    
    /**
     * The name of the activity.
     */
    private final String name;
    
    /**
     * The generation time of the progress report.
     */
    private final Instant generationTime;
    
    /**
     * The current state of the activity occurrence.
     */
    private final ActivityOccurrenceState state;
    
    /**
     * The execution time (estimated or final). If not null, this report provides the activity occurrence execution time.
     */
    private final Instant executionTime;
    
    /**
     * The report status.
     */
    private final ActivityReportState status;
    
    /**
     * The next state to transition to.
     */
    private final ActivityOccurrenceState nextState;
    
    /**
     * The execution result (partial or final). If not null, this report provides the activity occurrence execution result.
     */
    private final Object result;

    /**
     * Private constructor for creating ActivityProgress instances.
     * Use the factory methods instead.
     *
     * @param activityId the ID of the activity
     * @param occurrenceId the unique identifier of the activity occurrence, cannot be null
     * @param name the name of the activity
     * @param generationTime the generation time of the progress report, cannot be null
     * @param state the current state of the activity occurrence, cannot be null
     * @param executionTime the execution time, can be null
     * @param status the report status, cannot be null
     * @param nextState the next state to transition to, can be null
     * @param result the execution result, can be null
     */
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

    /**
     * Returns the ID of the activity.
     *
     * @return the activity ID
     */
    public int getActivityId() {
        return activityId;
    }

    /**
     * Returns the unique identifier of the activity occurrence.
     *
     * @return the occurrence ID (cannot be null)
     */
    public IUniqueId getOccurrenceId() {
        return occurrenceId;
    }

    /**
     * Returns the name of the activity.
     *
     * @return the activity name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the generation time of the progress report.
     *
     * @return the generation time (cannot be null)
     */
    public Instant getGenerationTime() {
        return generationTime;
    }

    /**
     * Returns the current state of the activity occurrence.
     *
     * @return the current state (cannot be null)
     */
    public ActivityOccurrenceState getState() {
        return state;
    }

    /**
     * Returns the execution time (estimated or final).
     *
     * @return the execution time (can be null)
     */
    public Instant getExecutionTime() {
        return executionTime;
    }

    /**
     * Returns the report status.
     *
     * @return the report status (cannot be null)
     */
    public ActivityReportState getStatus() {
        return status;
    }

    /**
     * Returns the next state to transition to.
     *
     * @return the next state (can be null)
     */
    public ActivityOccurrenceState getNextState() {
        return nextState;
    }

    /**
     * Returns the execution result (partial or final).
     *
     * @return the execution result (can be null)
     */
    public Object getResult() {
        return result;
    }

    /**
     * Returns a string representation of this activity progress.
     *
     * @return a string representation of this object
     */
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
