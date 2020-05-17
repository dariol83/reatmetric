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

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;

import java.time.Instant;

/**
 * An activity report is an indication of progress for an activity verification stage, provided by the processing
 * model or by the entity transferring or executing a specific activity occurrence.
 */
public final class ActivityOccurrenceReport extends AbstractDataItem {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the verification stage that indicates the creation of the activity occurrence. Used by the
     * processing model.
     */
    public static final String CREATION_REPORT_NAME = "Creation";
    /**
     * The name of the verification stage that indicates the forwarding of the activity occurrence to the indicated
     * activity handler. Used by the processing model.
     */
    public static final String FORWARDING_REPORT_NAME = "Forwarding to Activity Handler";
    /**
     * The name of the verification stage that indicates the release of the activity occurrence from the ReatMetric
     * system. It should be used by the activity handler implementing the activity transmission.
     */
    public static final String RELEASE_REPORT_NAME = "Release";
    /**
     * The name of the verification stage that indicates the verification of the activity occurrence execution, after
     * the associated indication from the received reports. Used by the processing model.
     */
    public static final String VERIFICATION_REPORT_NAME = "Verification";
    /**
     * The name of the verification stage indicating that the activity was purged as per related request. Used by the
     * processing model.
     */
    public static final String PURGE_REPORT_NAME = "Purge";

    private final String name; // Always set
    private final ActivityOccurrenceState state; // Always set
    private final Instant executionTime; // If not null, this report provides the activity occurrence execution time (estimated or final)
    private final ActivityReportState status;
    private final ActivityOccurrenceState stateTransition; // Always set, can be equal to state
    private final Object result; // If not null, this report provides the activity occurrence execution result (partial or final)

    /**
     * The constructor of the activity report.
     *
     * @param internalId the internal (unique) ID of the report
     * @param generationTime the report generation time
     * @param extension an extension object (can be null)
     * @param name the name of the stage, this report is referring to (not null)
     * @param state the state (as defined by the activity lifecycle), this report belongs to, check {@link ActivityOccurrenceState} (cannot be null)
     * @param executionTime the execution time of the activity occurrence, if known (can be null)
     * @param status the status of the stage provided by this report (not null)
     * @param stateTransition the new progressive activity occurrence state, that this report shall trigger (not null, can be equal to state)
     * @param result the result of the activity occurrence, if delivered by this report (can be null)
     */
    public ActivityOccurrenceReport(IUniqueId internalId, Instant generationTime, Object extension, String name, ActivityOccurrenceState state, Instant executionTime, ActivityReportState status, ActivityOccurrenceState stateTransition, Object result) {
        super(internalId, generationTime, extension);
        if(state == null || stateTransition == null) {
            throw new NullPointerException("state or stateTransition set to null");
        }
        if(name == null) {
            throw new NullPointerException("Report name set to null");
        }
        this.name = name;
        this.state = state;
        this.executionTime = executionTime;
        this.status = status;
        this.stateTransition = stateTransition;
        this.result = result;
    }

    /**
     * The name of the stage, this report is linked to.
     *
     * @return the name of the stage
     */
    public String getName() {
        return name;
    }

    /**
     * The activity state {@link ActivityOccurrenceState} the reported stage belongs to.
     *
     * @return the {@link ActivityOccurrenceState}, this report belongs to
     */
    public ActivityOccurrenceState getState() {
        return state;
    }

    /**
     * The activity occurrence execution time as provided/estimated by this report.
     *
     * @return the activity occurrence execution time (can be null)
     */
    public Instant getExecutionTime() {
        return executionTime;
    }

    /**
     * The status of the verification stage linked to this report.
     *
     * @return the status provided by the report
     */
    public ActivityReportState getStatus() {
        return status;
    }

    /**
     * The indication to the next state of the activity occurrence, upon processing of this report.
     *
     * @return the next state of the activity occurrence
     */
    public ActivityOccurrenceState getStateTransition() {
        return stateTransition;
    }

    /**
     * The result of the activity occurrence, as provided by this report.
     *
     * @return the activity occurrence result (can be null)
     */
    public Object getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ActivityOccurrenceReport{" +
                "name='" + name + '\'' +
                ", state=" + state +
                ", executionTime=" + executionTime +
                ", status=" + status +
                ", stateTransition=" + stateTransition +
                ", result=" + result +
                ", generationTime=" + generationTime +
                ", id=" + internalId +
                "}";
    }
}
