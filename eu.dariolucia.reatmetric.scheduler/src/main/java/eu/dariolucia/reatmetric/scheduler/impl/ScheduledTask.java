package eu.dariolucia.reatmetric.scheduler.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.Scheduler;

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

    public boolean conflictsWith(SchedulingRequest request, Pair<Instant, Instant> requestTimeInfo) {
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
        return this.currentData.overlapsWith(requestTimeInfo.getFirst(), requestTimeInfo.getSecond());
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
                        if(this != timingHandler) {
                            return;
                        }
                        runTask();
                    }
                };
                timer.schedule(timingHandler, new Date(((AbsoluteTimeSchedulingTrigger) request.getTrigger()).getReleaseTime().toEpochMilli()));
            } else if(request.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
                // Do nothing unless the task shall actually start: if no ID is in the scheduler map, it can start
                if(scheduler.areAllCompleted(((RelativeTimeSchedulingTrigger) request.getTrigger()).getPredecessors())) {
                    runTask();
                }
            } else if(request.getTrigger() instanceof EventBasedSchedulingTrigger) {
                scheduler.updateEventFilter(((EventBasedSchedulingTrigger) request.getTrigger()).getEvent());
            } else {
                // TODO log or exception
            }
        } else {
            throw new SchedulingException("Cannot update trigger evaluation for task " + this.taskId + ", task is not scheduled anymore");
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    private void initialiseCurrentData() {
        Pair<Instant, Instant> timeWindow = scheduler.computeTimeInformation(this.request);
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

    private void runTask() {
        dispatcher.submit(() -> {
            // Try to run the activity if the scheduler is enabled, if all resources are available and if all constraints are satisfied
            // TODO
        });
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param isEnabled
     */
    public void informEnablementChange(boolean isEnabled) {
        // TODO
    }

    public IUniqueId getId() {
        return this.taskId;
    }

    /**
     * To be called from the dispatcher thread.
     */
    public void abortTask() {
        // TODO: if running, request model to abort, mark as ABORTED, return false
        // TODO: if scheduled, disable trigger, mark as REMOVED, return true
        // TODO: if not any of the two above, the activity is already over, so do not do anything, return false.
    }
}
