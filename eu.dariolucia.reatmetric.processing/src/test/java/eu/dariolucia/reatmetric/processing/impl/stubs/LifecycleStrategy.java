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
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class LifecycleStrategy {

    protected static final Logger LOG = Logger.getLogger(LifecycleStrategy.class.getName());

    public abstract void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model);

    protected void log(IActivityHandler.ActivityInvocation invocation, String message) {
        log(invocation, message, null);
    }

    protected void log(IActivityHandler.ActivityInvocation invocation, String message, Exception e) {
        LOG.log(Level.INFO, String.format("Activity Occurrence %d - %s - %s", invocation.getActivityOccurrenceId().asLong(), invocation.getPath(), message), e);
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextOccState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, null, reportState, nextOccState, null));
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, null, reportState, occState, null));
    }

    protected void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, String name, ActivityReportState reportState, ActivityOccurrenceState occState, ActivityOccurrenceState nextOccState, Instant executionTime, Object result) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, Instant.now(), occState, executionTime, reportState, nextOccState, result));
    }
}
