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

package eu.dariolucia.reatmetric.scheduler;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduledTask {

    private static final Logger LOG = Logger.getLogger(ScheduledTask.class.getName());

    private final Scheduler scheduler;
    private final Timer timer;
    private final ExecutorService dispatcher;
    private final IUniqueId taskId;

    private SchedulingRequest request;
    private ScheduledActivityData currentData;

    /**
     * TimerTask for absolute time scheduling requests
     */
    private volatile TimerTask timingHandler;
    /**
     * TimerTask activated when the activity has a latest execution time set
     */
    private volatile TimerTask latestExecutionTimeHandler;
    /**
     * The activity occurrence ID of the activity once invoked
     */
    private IUniqueId activityId;
    /**
     * If true, then it means that this task acquired the declared resources, and they must be released when the task is over
     */
    private boolean resourcesAcquired = false;
    /**
     * For event-based activities, this information is needed to enforce the protection mechanism
     */
    private Instant lastEventTriggerInvocation = null;

    public ScheduledTask(Scheduler scheduler, Timer timer, ExecutorService dispatcher, SchedulingRequest request, IUniqueId originalId) {
        this.scheduler = scheduler;
        this.request = request;
        this.timer = timer;
        this.dispatcher = dispatcher;
        if(originalId != null) {
            this.taskId = originalId;
        } else {
            this.taskId = this.scheduler.getNextId();
        }
    }

    public ScheduledTask(Scheduler scheduler, Timer timer, ExecutorService dispatcher, ScheduledActivityData item) {
        this.scheduler = scheduler;
        this.timer = timer;
        this.dispatcher = dispatcher;
        this.taskId = item.getInternalId();
        this.currentData = item;
        this.request = new SchedulingRequest(item.getRequest(), item.getResources(), item.getSource(), item.getExternalId(), item.getTrigger(), item.getLatestInvocationTime(), item.getConflictStrategy(), item.getDuration());
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
                // Do nothing unless the task shall actually start: if no ID is in the scheduler map, it can start at the given delay, if set
                if(scheduler.areAllCompleted(((RelativeTimeSchedulingTrigger) request.getTrigger()).getPredecessors())) {
                    if(((RelativeTimeSchedulingTrigger) request.getTrigger()).getDelayTime() <= 0) {
                        runTask(false);
                    } else {
                        timingHandler = new TimerTask() {
                            @Override
                            public void run() {
                                if(this == timingHandler) {
                                    runTask(false);
                                }
                            }
                        };
                        timer.schedule(timingHandler, ((RelativeTimeSchedulingTrigger) request.getTrigger()).getDelayTime() * 1000);
                    }
                }
            } else if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
                scheduler.updateEventFilter(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent(),  false);
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
     * @param event the event to check
     */
    public void newEventOccurrence(EventData event) {
        Instant now = Instant.now();
        if(currentData.getState() == SchedulingState.SCHEDULED &&
                scheduler.isEnabled() &&
                (request.getTrigger() instanceof EventBasedSchedulingTrigger && ((EventBasedSchedulingTrigger) request.getTrigger()).getEvent() == event.getExternalId()) &&
                (request.getLatestInvocationTime() == null || request.getLatestInvocationTime().isAfter(now))) {
            // Check if the protection time is OK
            if(lastEventTriggerInvocation == null || lastEventTriggerInvocation.plusMillis(((EventBasedSchedulingTrigger) request.getTrigger()).getProtectionTime()).isBefore(now)) {
                lastEventTriggerInvocation = now;
                SchedulingRequest newRequest = generateImmediateRequest(now);
                CreationConflictStrategy newConflictStrategy = CreationConflictStrategy.ADD_ANYWAY; // It will be handled by the ConflictStrategy of the request
                scheduler.internalScheduleRequest(newRequest, newConflictStrategy);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param now time to start
     */
    private SchedulingRequest generateImmediateRequest(Instant now) {
        return new SchedulingRequest(request.getRequest(), request.getResources(), request.getSource(), request.getExternalId(),
                new AbsoluteTimeSchedulingTrigger(now), request.getLatestInvocationTime(), request.getConflictStrategy(), request.getExpectedDuration());
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param data the activity occurrence for this task
     */
    public void activityStatusUpdate(ActivityOccurrenceData data) {
        Instant now = Instant.now();
        if(data.getCurrentState() == ActivityOccurrenceState.COMPLETED) {
            // Activity completed: the state is finally set and the task is removed
            this.currentData = buildUpdatedSchedulingActivityData(currentData.getStartTime(),
                    Duration.between(currentData.getStartTime(), now),
                    activityId,
                    data.aggregateStatus() == ActivityReportState.OK ? SchedulingState.FINISHED_NOMINAL : SchedulingState.FINISHED_FAIL);
            scheduler.deregisterActivity(activityId);
            activityId = null;
            checkForTaskRemoval();
        } else {
            // Activity is in progress
            this.currentData = buildUpdatedSchedulingActivityData(currentData.getStartTime(),
                    Duration.between(currentData.getStartTime(), now),
                    activityId,
                    SchedulingState.RUNNING);
            scheduler.notifyTask(this);
        }
    }

    private ScheduledActivityData buildUpdatedSchedulingActivityData(Instant startTime, Duration duration, IUniqueId occurrence, SchedulingState state) {
        return new ScheduledActivityData(this.taskId,
                startTime,
                this.request.getRequest(),
                occurrence,
                this.request.getResources(),
                this.request.getSource(),
                this.request.getExternalId(),
                this.request.getTrigger(),
                this.request.getLatestInvocationTime(),
                startTime,
                duration,
                this.request.getConflictStrategy(),
                state,
                null);
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

    public void evaluateRun() {
        runTask(false);
    }

    private void runTask(boolean lastPossibleExecution) {
        if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
            throw new IllegalStateException("runTask invoked on an event based task, software bug");
        }
        dispatcher.submit(() -> {
            // Guard condition: you can reach this only if SCHEDULED or WAITING. If this happens in a different situation, error
            if(currentData.getState() != SchedulingState.SCHEDULED && currentData.getState() != SchedulingState.WAITING) {
                LOG.log(Level.SEVERE, "Scheduled task " + taskId + " with state " + currentData.getState() + " requested to executed, request ignored.");
                return;
            }
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
                            checkForTaskRemoval();
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
                        checkForTaskRemoval();
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
                        scheduler.registerActivity(this.activityId, this);
                        this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                this.activityId,
                                SchedulingState.RUNNING);
                        scheduler.notifyTask(this);
                    } catch (ReatmetricException e) {
                        // Fail and remove
                        this.currentData = buildUpdatedSchedulingActivityData(newStartTime,
                                null,
                                SchedulingState.FINISHED_FAIL);
                        checkForTaskRemoval();
                    }
                } else {
                    // Release the resources and report the task as DISABLED. This task is dead if not an event-based task.
                    this.currentData = buildUpdatedSchedulingActivityData(newStartTime, null,
                            SchedulingState.DISABLED);
                    checkForTaskRemoval();
                }
            }
        });
    }

    private void checkForTaskRemoval() {
        // An event-based task cannot be removed
        scheduler.removeTask(taskId);
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
        // Release resources
        if(resourcesAcquired) {
            scheduler.releaseResources(request.getResources());
            resourcesAcquired = false;
        }
        // Update the state
        if(currentData.getState() == SchedulingState.RUNNING) {
            // if running, request model to abort, mark as ABORTED
            scheduler.abortActivity(request.getRequest().getId(), activityId);
            this.currentData = buildUpdatedSchedulingActivityData(currentData.getStartTime(),
                    this.activityId,
                    SchedulingState.ABORTED);
        } else if(currentData.getState() == SchedulingState.WAITING || currentData.getState() == SchedulingState.SCHEDULED) {
            // if scheduled or waiting, disable trigger, mark as REMOVED
            stopLatestExecutionTimer();
            if(timingHandler != null) {
                timingHandler.cancel();
                timingHandler = null;
            }
            this.currentData = buildUpdatedSchedulingActivityData(currentData.getStartTime(),
                    this.activityId,
                    SchedulingState.REMOVED);
        } else {
            // if not any of the two above, the activity is already over, so do not do anything
            if(LOG.isLoggable(Level.FINE)) {
                LOG.fine("Task " + taskId + " already in state " + currentData.getState() + ", abort not performed");
            }
        }
        // Remove the activity occurrence ID from the set of interesting ones
        if(activityId != null) {
            scheduler.deregisterActivity(activityId);
            activityId = null;
        }
        // Remove the event from the list of interesting ones
        if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
            scheduler.updateEventFilter(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent(), true);
        }
    }


    @Override
    public String toString() {
        return "ScheduledTask{" +
                "taskId=" + taskId.asLong() +
                '}';
    }

}
