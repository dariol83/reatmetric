package eu.dariolucia.reatmetric.scheduler.impl;

import eu.dariolucia.reatmetric.api.scheduler.EventBasedSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.ScheduledActivityData;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.Scheduler;

import java.util.Collections;
import java.util.TimerTask;

public class ScheduledTask {

    private SchedulingRequest request;
    private TimerTask timingHandler;
    private ScheduledActivityData currentData;

    private TimeInformation timeInformation;

    private final Scheduler scheduler;

    public ScheduledTask(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public SchedulingRequest getRequest() {
        return request;
    }

    public ScheduledActivityData getCurrentData() {
        return currentData;
    }

    public TimeInformation getTimeInformation() {
        return timeInformation;
    }

    public boolean conflictsWith(SchedulingRequest request, TimeInformation requestTimeInfo) {
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
        // To declare a conflict, there must be an overlap in the two expected execution times.
        if(!this.timeInformation.overlapsWith(requestTimeInfo)) {
            return false;
        } else {
            return true;
        }
    }
}
