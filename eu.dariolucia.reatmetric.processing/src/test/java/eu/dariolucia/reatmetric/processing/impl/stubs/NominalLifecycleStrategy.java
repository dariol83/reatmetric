/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.stubs;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.processing.IActivityHandler;
import eu.dariolucia.reatmetric.processing.IProcessingModel;

public class NominalLifecycleStrategy extends LifecycleStrategy {

    private int transmissionStateCount = 3;
    private int transmissionTime = 1000;
    private int executionStateCount = 3;
    private int executionTime = 3000;

    public NominalLifecycleStrategy() {
    }

    public NominalLifecycleStrategy(int transmissionStateCount, int transmissionTime, int executionStateCount, int executionTime) {
        this.transmissionStateCount = transmissionStateCount;
        this.transmissionTime = transmissionTime;
        this.executionStateCount = executionStateCount;
        this.executionTime = executionTime;
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
                announce(activityInvocation, model, "E" + i, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, i != executionStateCount - 1 ? ActivityOccurrenceState.EXECUTION : ActivityOccurrenceState.VERIFICATION);
            }
            log(activityInvocation, "Execution completed");
        } catch(Exception e) {
            log(activityInvocation, "Exception raised", e);
            announce(activityInvocation, model, "Error", ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION);
        }
    }

}
