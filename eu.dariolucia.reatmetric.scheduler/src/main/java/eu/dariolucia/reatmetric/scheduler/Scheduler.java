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

import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.impl.ScheduledTask;
import eu.dariolucia.reatmetric.scheduler.impl.SchedulingEvent;
import eu.dariolucia.reatmetric.scheduler.impl.TimeInformation;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scheduler implements IScheduler {

    private static final Logger LOG = Logger.getLogger(Scheduler.class.getName());

    private final IScheduledActivityDataArchive archive;
    private final IProcessingModel processingModel;

    /**
     * A timer that reports events into the eventQueue, based on time-based, absolute constraints.
     */
    private final Timer timer = new Timer("ReatMetric Scheduler Timer", true);
    /**
     * A sorted queue that contains scheduling events to be processed. The idea is that the time, spent to process
     * an event by the dispatcher, is very short.
     */
    private final Queue<SchedulingEvent> eventQueue = new LinkedList<>();
    /**
     * This thread picks up events from the queue and perform the required action.
     */
    private final Thread eventDispatcher;
    /**
     * This map allows quick access to the scheduled tasks by ID.
     */
    private final Map<IUniqueId, ScheduledTask> id2scheduledTask = new HashMap<>();
    /**
     * The set if resources currently taken by running scheduled tasks.
     */
    private final Set<String> currentlyUsedResources = new TreeSet<>();

    private volatile boolean running;
    private volatile boolean enabled;

    public Scheduler(IArchive archive, IProcessingModel model) {
        if(archive != null) {
            this.archive = archive.getArchive(IScheduledActivityDataArchive.class);
        } else {
            this.archive = null;
        }
        this.processingModel = model;
        this.eventDispatcher = new Thread(this::runDispatching, "ReatMetric Scheduler Dispatcher");
        this.eventDispatcher.setDaemon(true);
    }

    private void runDispatching() {
        while(running) {
            List<SchedulingEvent> toProcess = new ArrayList<>();
            synchronized (this) {
                while(running && (eventQueue.isEmpty() || !enabled)) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                if(!running) {
                    eventQueue.clear();
                    return;
                }
                toProcess.addAll(eventQueue);
                eventQueue.clear();
                notifyAll();
            }
            // Now process the events
            for(SchedulingEvent e : toProcess) {
                e.process();
            }
        }
    }

    public synchronized void dispose() {
        this.running = false;
        notifyAll();
    }

    @Override
    public void initialise() throws SchedulingException {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("initialise() invoked");
        }
        // TODO prepare internal state
        // TODO initialise from archive
        // TODO start disabled (depending on system property - to be defined)

        this.running = true;
        this.eventDispatcher.start();
    }

    @Override
    public void subscribe(ISchedulerSubscriber subscriber) {

    }

    @Override
    public void unsubscribe(ISchedulerSubscriber subscriber) {

    }

    @Override
    public synchronized void enable() throws SchedulingException {
        this.enabled = true;
        notifyAll();
    }

    @Override
    public synchronized void disable() throws SchedulingException {
        this.enabled = false;
        notifyAll();
    }

    @Override
    public synchronized boolean isEnabled() throws SchedulingException {
        return enabled;
    }

    @Override
    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        // Check if the creation conflict strategy allows for the scheduling
        List<ScheduledTask> conflictingTasks = null;
        try {
            conflictingTasks = computeConflicts(Collections.singletonList(request));
        } catch (ProcessingModelException e) {
            throw new SchedulingException(e);
        }
        if(conflictingTasks.isEmpty() || conflictStrategy == CreationConflictStrategy.ADD_ANYWAY) {
            // Add the request as-is
            return addTask(request);
        } else {
            // There is a conflict
            if(conflictStrategy == CreationConflictStrategy.ABORT) {
                // Abort the operation, leave the schedule untouched
                throw new SchedulingException("Conflict detected between the request and the following tasks: " + conflictingTasks + ", scheduling request aborted");
            } else if(conflictStrategy == CreationConflictStrategy.SKIP_NEW) {
                // Return null to indicate that the request is skipped
                return null;
            } else if(conflictStrategy == CreationConflictStrategy.REMOVE_PREVIOUS) {
                // Remove the conflicting tasks and add the new task
                removeTasks(conflictingTasks);
                return addTask(request);
            } else {
                throw new IllegalArgumentException("CreationConflictStrategy " + conflictStrategy + " not handled, software bug");
            }
        }
    }

    private ScheduledActivityData addTask(SchedulingRequest request) {
        // Create ScheduledTask
        // TODO
        // Prepare execution event depending on trigger (absolute, relative, event)
        // TODO
        // Store and distribute
        // TODO
        // Return scheduled activity data
        return null;
    }

    private void removeTasks(List<ScheduledTask> conflictingTasks) {
        // Remove the tasks from the map
        // TODO
        // Create the new ScheduledActivityData, store and distribute
        // TODO
    }

    private List<ScheduledTask> computeConflicts(List<SchedulingRequest> requests) throws ProcessingModelException {
        Set<IUniqueId> conflictingIds = new HashSet<>();
        List<ScheduledTask> toReturn = new LinkedList<>();
        // For each request, check if there are conflicts
        for(SchedulingRequest sr : requests) {
            TimeInformation timeInfo = computeTimeInformation(sr);
            for(ScheduledTask task : this.id2scheduledTask.values()) {
                if(task.conflictsWith(sr, timeInfo) && !conflictingIds.contains(task.getCurrentData().getInternalId())) {
                    toReturn.add(task);
                    conflictingIds.add(task.getCurrentData().getInternalId());
                }
            }
        }
        return toReturn;
    }

    public TimeInformation computeTimeInformation(SchedulingRequest sr) throws ProcessingModelException {
        if(sr.getTrigger() instanceof EventBasedSchedulingTrigger) {
            return new TimeInformation(Instant.EPOCH, Instant.MAX);
        } else if(sr.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger trigger = (AbsoluteTimeSchedulingTrigger) sr.getTrigger();
            Duration duration = computeDuration(sr);
            return new TimeInformation(trigger.getReleaseTime(), trigger.getReleaseTime().plus(duration));
        } else if(sr.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            Instant triggerTime = computePredecessorsLatestEndTime(((RelativeTimeSchedulingTrigger) sr.getTrigger()).getPredecessors());
            Duration duration = computeDuration(sr);
            return new TimeInformation(triggerTime, triggerTime.plus(duration));
        } else {
            throw new IllegalArgumentException("Object " + sr.getTrigger() + " not handled");
        }
    }

    private Instant computePredecessorsLatestEndTime(List<IUniqueId> predecessors) {
        Instant latestEndTime = null;
        for(IUniqueId id : predecessors) {
            ScheduledTask task = id2scheduledTask.get(id);
            TimeInformation ti = task.getTimeInformation();
            if(latestEndTime == null || ti.getEndTime().isAfter(latestEndTime)) {
                latestEndTime = ti.getEndTime();
            }
        }
        return latestEndTime;
    }

    private Duration computeDuration(SchedulingRequest sr) throws ProcessingModelException {
        if(sr.getExpectedDuration() != null) {
            return sr.getExpectedDuration();
        } else {
            AbstractSystemEntityDescriptor descriptor = processingModel.getDescriptorOf(sr.getRequest().getId());
            ActivityDescriptor activityDescriptor = (ActivityDescriptor) descriptor;
            return activityDescriptor.getExpectedDuration();
        }
    }

    @Override
    public List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        // Check if the creation conflict strategy allows for the scheduling
        try {
            List<ScheduledTask> conflictingTasks = computeConflicts(request);
        } catch (ProcessingModelException e) {
            throw new SchedulingException(e);
        }
        // TODO
        return null;
    }

    @Override
    public List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException {
        return null;
    }

    @Override
    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        return null;
    }

    @Override
    public boolean remove(IUniqueId scheduledId) throws SchedulingException {
        return false;
    }

    @Override
    public boolean remove(ScheduledActivityDataFilter filter) throws SchedulingException {
        return false;
    }

    @Override
    public List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) {
        return null;
    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant time, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }

    @Override
    public void subscribe(IScheduledActivityDataSubscriber subscriber, ScheduledActivityDataFilter filter) {

    }

    @Override
    public void unsubscribe(IScheduledActivityDataSubscriber subscriber) {

    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }

    @Override
    public List<ScheduledActivityData> retrieve(ScheduledActivityData startItem, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        return null;
    }
}
