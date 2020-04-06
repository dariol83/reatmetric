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

package eu.dariolucia.reatmetric.processing.impl.stubs;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * This lifecycle strategy reads a specific property (as per SCHEDULED_EXECUTION_TIME_KEY constant) to resume the execution
 * of the activity occurrence at the specified point in time.
 */
public class SchedulingLifecycleStrategy extends NominalLifecycleStrategy {

    public static final String SCHEDULED_EXECUTION_TIME_KEY = "scheduled-execution-time";

    public SchedulingLifecycleStrategy() {
        super();
    }

    public SchedulingLifecycleStrategy(int transmissionStateCount, int transmissionTime, int executionStateCount, int executionTime) {
        super(transmissionStateCount, transmissionTime, executionStateCount, executionTime, () -> null);
    }

    public SchedulingLifecycleStrategy(int transmissionStateCount, int transmissionTime, int executionStateCount, int executionTime, Supplier<Object> resultSupplier) {
        super(transmissionStateCount, transmissionTime, executionStateCount, executionTime, resultSupplier);
    }

    @Override
    public void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        String scheduledExecTime = activityInvocation.getProperties().get(SCHEDULED_EXECUTION_TIME_KEY);
        Instant absExecTime = null;
        if(scheduledExecTime != null && !scheduledExecTime.isBlank()) {
            absExecTime = Instant.parse(scheduledExecTime);
        }
        try {
            log(activityInvocation, "Release finalisation");
            announce(activityInvocation, model, "Final Release", ActivityReportState.OK, ActivityOccurrenceState.RELEASE, ActivityOccurrenceState.TRANSMISSION);
            log(activityInvocation, "Transmission started");
            for (int i = 0; i < transmissionStateCount; ++i) {
                announce(activityInvocation, model, "T" + i, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
            }
            int transmissionForEachState = transmissionTime / transmissionStateCount;
            for (int i = 0; i < transmissionStateCount; ++i) {
                Thread.sleep(transmissionForEachState);
                announce(activityInvocation, model, "T" + i, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, i != transmissionStateCount - 1 ? ActivityOccurrenceState.TRANSMISSION : ActivityOccurrenceState.SCHEDULING);
            }
            log(activityInvocation, "Transmission completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            return;
        }
        // Wait the amount specified in the property
        if(absExecTime != null) {
            announce(activityInvocation, model, "Scheduling Time", ActivityReportState.PENDING, ActivityOccurrenceState.SCHEDULING);
            long timeToSleep = absExecTime.toEpochMilli() - System.currentTimeMillis();
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                log(activityInvocation, "Scheduling time interrupted");
            }
        }
        // Move to EXECUTION
        announce(activityInvocation, model, "Scheduling Time", ActivityReportState.OK, ActivityOccurrenceState.SCHEDULING, ActivityOccurrenceState.EXECUTION, Instant.now(), null);
        // Execute
        try {
            log(activityInvocation, "Execution started");
            for (int i = 0; i < executionStateCount; ++i) {
                announce(activityInvocation, model, "E" + i, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION);
            }
            int executionForEachState = executionTime / executionStateCount;
            for (int i = 0; i < executionStateCount; ++i) {
                Thread.sleep(executionForEachState);
                announce(activityInvocation, model, "E" + i, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, i != executionStateCount - 1 ? ActivityOccurrenceState.EXECUTION : ActivityOccurrenceState.VERIFICATION, null, i == executionStateCount - 1 ? this.resultSupplier.get() : null);
            }
            log(activityInvocation, "Execution completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION);
        }
    }
}
