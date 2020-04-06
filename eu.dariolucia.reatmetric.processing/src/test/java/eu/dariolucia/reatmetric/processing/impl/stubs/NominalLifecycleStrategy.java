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

import java.util.function.Supplier;

public class NominalLifecycleStrategy extends LifecycleStrategy {

    protected int transmissionStateCount = 3;
    protected int transmissionTime = 1000;
    protected int executionStateCount = 3;
    protected int executionTime = 3000;
    protected Supplier<Object> resultSupplier = () -> null;

    public NominalLifecycleStrategy() {
    }

    public NominalLifecycleStrategy(int transmissionStateCount, int transmissionTime, int executionStateCount, int executionTime) {
        this(transmissionStateCount, transmissionTime, executionStateCount, executionTime, () -> null);
    }

    public NominalLifecycleStrategy(int transmissionStateCount, int transmissionTime, int executionStateCount, int executionTime, Supplier<Object> resultSupplier) {
        this.transmissionStateCount = transmissionStateCount;
        this.transmissionTime = transmissionTime;
        this.executionStateCount = executionStateCount;
        this.executionTime = executionTime;
        this.resultSupplier = resultSupplier;
    }

    @Override
    public void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
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
                announce(activityInvocation, model, "T" + i, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, i != transmissionStateCount - 1 ? ActivityOccurrenceState.TRANSMISSION : ActivityOccurrenceState.EXECUTION);
            }
            log(activityInvocation, "Transmission completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            return;
        }
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
