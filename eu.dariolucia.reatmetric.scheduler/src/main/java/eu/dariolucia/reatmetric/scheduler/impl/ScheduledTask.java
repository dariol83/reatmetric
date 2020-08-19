package eu.dariolucia.reatmetric.scheduler.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

public class ScheduledTask {

    private SchedulingRequest request;
    private volatile TimerTask timingHandler;
    private ScheduledActivityData currentData;

    private final Scheduler scheduler;
    private final Timer timer;
    private final ExecutorService dispatcher;
    private final IUniqueId taskId;

    private IUniqueId activityId;
    private volatile TimerTask latestExecutionTimeHandler;
    private boolean resourcesAcquired = false;

    public ScheduledTask(Scheduler scheduler, Timer timer, ExecutorService dispatcher, SchedulingRequest request) {
        this.scheduler = scheduler;
        this.request = request;
        this.timer = timer;
        this.dispatcher = dispatcher;
        this.taskId = this.scheduler.getNextId();
    }

    public SchedulingRequest getRequest() {
        return request;
    }

    public ScheduledActivityData getCurrentData() {
        return currentData;
    }

    public boolean conflictsWith(SchedulingRequest request, Pair<Instant, Duration> requestTimeInfo) {
        // An event-driven task can never conflict
        if(this.request.getTrigger() instanceof EventBasedSchedulingTrigger ||
            request.getTrigger() instanceof EventBasedSchedulingTrigger) {
            return false;
        }
        // If the resource sets are disjoint, the tasks cannot conflict
        if(Collections.disjoint(request.getResources(), this.request.getResources())) {
            return false;
        }
        // At this stage, there is a conflict in the set of declared resources.
        // To declare a conflict, there must be an overlap in the two expected execution times AND the task should not be
        // already completed (should never be the case).
        return this.currentData.overlapsWith(requestTimeInfo.getFirst(), requestTimeInfo.getFirst().plus(requestTimeInfo.getSecond()));
    }

    /**
     * To be called from the dispatcher thread.
     */
    public void updateTrigger() throws SchedulingException {
        if(currentData == null) {
            initialiseCurrentData();
        }
        if(currentData.getState() == SchedulingState.SCHEDULED) {
            // Reset the trigger
            if(timingHandler != null) {
                timingHandler.cancel();
                timingHandler = null;
            }
            // Depending on the trigger
            if (request.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
                timingHandler = new TimerTask() {
                    @Override
                    public void run() {
                        if(this == timingHandler) {
                            runTask(false);
                        }
                    }
                };
                timer.schedule(timingHandler, new Date(((AbsoluteTimeSchedulingTrigger) request.getTrigger()).getReleaseTime().toEpochMilli()));
            } else if(request.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
                // Do nothing unless the task shall actually start: if no ID is in the scheduler map, it can start
                if(scheduler.areAllCompleted(((RelativeTimeSchedulingTrigger) request.getTrigger()).getPredecessors())) {
                    runTask(false);
                }
            } else if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
                scheduler.updateEventFilter(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent());
            } else {
                throw new SchedulingException("Cannot update trigger evaluation for task " + this.taskId + ", trigger type " + request.getTrigger() + " not recognised");
            }
        } else {
            throw new SchedulingException("Cannot update trigger evaluation for task " + this.taskId + ", task is not scheduled anymore");
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    private void initialiseCurrentData() {
        Pair<Instant, Duration> timeWindow = scheduler.computeTimeInformation(this.request);
        this.currentData = new ScheduledActivityData(this.taskId, timeWindow.getFirst(), this.request.getRequest(), null, this.request.getResources(), this.request.getSource(), this.request.getExternalId(), this.request.getTrigger(), this.request.getLatestInvocationTime(),
                timeWindow.getFirst(), timeWindow.getSecond(), this.request.getConflictStrategy(), SchedulingState.SCHEDULED, null);
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param event
     */
    public void newEventOccurrence(EventData event) {
        // TODO
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param data
     */
    public void activityStatusUpdate(ActivityOccurrenceData data) {
        // TODO
    }

    private ScheduledActivityData buildUpdatedSchedulingActivityData(Instant newTime, IUniqueId newOccurrence, SchedulingState state) {
        return new ScheduledActivityData(this.taskId,
                newTime,
                this.request.getRequest(),
                newOccurrence,
                this.request.getResources(),
                this.request.getSource(),
                this.request.getExternalId(),
                this.request.getTrigger(),
                this.request.getLatestInvocationTime(),
                newTime,
                this.request.getExpectedDuration(),
                this.request.getConflictStrategy(),
                state,
                null);
    }

    private void runTask(boolean lastPossibleExecution) {
        dispatcher.submit(() -> {
            // Try to run the activity if the scheduler is enabled, if all resources are available and if all constraints are satisfied
            Instant newStartTime = Instant.now();
            // First, check the resources
            if(!scheduler.registerResources(request.getResources())) {
                // Not all the resources are available: check the conflict strategy ....
                switch(request.getConflictStrategy()) {
                    case WAIT: {
                        //
                        if(lastPossibleExecution) {
                            // Expired, therefore task ignored and move on ...
                            this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                    null,
                                    SchedulingState.IGNORED);
                            scheduler.removeTask(taskId);
                        } else {
                            // Wait for some update in the resource status
                            this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                    null,
                                    SchedulingState.WAITING);
                            startLatestExecutionTimer();
                            scheduler.notifyTask(this);
                        }
                    }
                    break;
                    case DO_NOT_START_AND_FORGET: {
                        // Mark as ignored and move on ...
                        this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                null,
                                SchedulingState.IGNORED);
                        scheduler.removeTask(taskId);
                    }
                    break;
                    case ABORT_OTHER_AND_START: {
                        scheduler.abortConflictingTasksWith(this);
                        runTask(lastPossibleExecution);
                    }
                    break;
                }
            } else {
                this.resourcesAcquired = true;
                // The resources are available and now assigned to this task - Check scheduler status
                if(scheduler.isEnabled()) {
                    // Start the task now
                    stopLatestExecutionTimer();
                    try {
                        this.activityId = this.scheduler.startActivity(this.request.getRequest());
                        this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                this.activityId,
                                SchedulingState.RUNNING);
                        scheduler.notifyTask(this);
                    } catch (ProcessingModelException e) {
                        // Fail and remove
                        this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                null,
                                SchedulingState.FINISHED_FAIL);
                        scheduler.removeTask(taskId);
                    }
                } else {
                    // Release the resources and report the task as DISABLED. This task is dead.
                    this.currentData = buildUpdatedSchedulingActivityData(newStartTime, null, SchedulingState.DISABLED);
                    scheduler.removeTask(taskId);
                }
            }
        });
    }

    private void stopLatestExecutionTimer() {
        if(this.latestExecutionTimeHandler != null) {
            this.latestExecutionTimeHandler.cancel();
            this.latestExecutionTimeHandler = null;
        }
    }

    private void startLatestExecutionTimer() {
        if(this.latestExecutionTimeHandler == null && this.request.getLatestInvocationTime() != null) {
            this.latestExecutionTimeHandler = new TimerTask() {
                @Override
                public void run() {
                    if(latestExecutionTimeHandler == this) {
                        runTask(true);
                    }
                }
            };
            this.timer.schedule(this.latestExecutionTimeHandler, new Date(this.request.getLatestInvocationTime().toEpochMilli()));
        }
    }

    public IUniqueId getId() {
        return this.taskId;
    }

    /**
     * To be called from the dispatcher thread.
     */
    public void abortTask() {
        if(resourcesAcquired) {
            scheduler.releaseResources(request.getResources());
        }
        // TODO: if running, request model to abort, release resources, mark as ABORTED
        // TODO: if scheduled or waiting, disable trigger, mark as REMOVED
        // TODO: if not any of the two above, the activity is already over, so do not do anything
    }


    @Override
    public String toString() {
        return "ScheduledTask{" +
                "taskId=" + taskId.asLong() +
                '}';
    }
}
