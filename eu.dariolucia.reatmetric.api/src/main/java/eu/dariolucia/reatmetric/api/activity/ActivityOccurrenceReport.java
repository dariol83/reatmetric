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

public final class ActivityOccurrenceReport extends AbstractDataItem {

    public static final String CREATION_REPORT_NAME = "Creation";
    public static final String FORWARDING_REPORT_NAME = "Forwarding to Activity Handler";
    public static final String RELEASE_REPORT_NAME = "Release";
    public static final String VERIFICATION_REPORT_NAME = "Verification";
    public static final String PURGE_REPORT_NAME = "Purge";

    private final String name; // Always set
    private final ActivityOccurrenceState state; // Always set
    private final Instant executionTime; // If not null, this report provides the activity occurrence execution time (estimated or final)
    private final ActivityReportState status;
    private final ActivityOccurrenceState stateTransition; // Always set, can be equal to state
    private final Object result; // If not null, this report provides the activity occurrence execution result (partial or final)

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

    public String getName() {
        return name;
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

    public ActivityOccurrenceState getStateTransition() {
        return stateTransition;
    }

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
