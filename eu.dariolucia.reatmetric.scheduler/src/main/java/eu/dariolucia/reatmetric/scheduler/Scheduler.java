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
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataProvisionService;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataSubscriber;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataProvisionService;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Scheduler implements IScheduler {

    private static final Logger LOG = Logger.getLogger(Scheduler.class.getName());

    private final IScheduledActivityDataArchive archive;
    private final IActivityExecutionService executionService;
    private final IEventDataProvisionService eventService;
    private final IActivityOccurrenceDataProvisionService activityService;

    private final List<ISchedulerSubscriber> schedulerSubscribers = new CopyOnWriteArrayList<>();

    private final Map<IScheduledActivityDataSubscriber, ScheduledActivityDataFilter> subscriberIndex = new ConcurrentHashMap<>();

    private final AtomicLong sequencer = new AtomicLong(0);

    /**
     * A timer that reports events into the eventQueue, based on time-based, absolute constraints.
     */
    private final Timer timer = new Timer("ReatMetric Scheduler Timer", true);
    /**
     * The dispatcher thread.
     */
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "ReatMetric Scheduler Dispatcher Thread");
        t.setDaemon(true);
        return t;
    });
    /**
     * The notifier thread.
     */
    private final ExecutorService notifier = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "ReatMetric Scheduler Notifier Thread");
        t.setDaemon(true);
        return t;
    });
    /**
     * This map allows quick access to the scheduled tasks by ID.
     */
    private final Map<IUniqueId, ScheduledTask> id2scheduledTask = new HashMap<>();
    /**
     * This map allows quick access to the task handling a specific activity occurrence execution.
     */
    private final Map<IUniqueId, ScheduledTask> activityId2scheduledTask = new HashMap<>();
    /**
     * The set if resources currently taken by running scheduled tasks.
     */
    private final Set<String> currentlyUsedResources = new TreeSet<>();
    /**
     * For event-based activities.
     */
    private final List<Integer> subscribedEvents = new LinkedList<>();

    private volatile boolean enabled;

    private final IActivityOccurrenceDataSubscriber activitySubscriber = dataItems -> dispatcher.submit(() -> activityUpdate(dataItems));
    private final IEventDataSubscriber eventSubscriber = dataItems -> dispatcher.submit(() -> eventUpdate(dataItems));
    private EventDataFilter currentEventFilter = null;

    public Scheduler(IArchive archive, IActivityExecutionService activityExecutor, IEventDataProvisionService eventMonService, IActivityOccurrenceDataProvisionService activityMonService) {
        if (archive != null) {
            this.archive = archive.getArchive(IScheduledActivityDataArchive.class);
        } else {
            this.archive = null;
        }
        this.executionService = activityExecutor;
        this.eventService = eventMonService;
        this.activityService = activityMonService;
        IUniqueId lastStoredUniqueId = null;
        try {
            lastStoredUniqueId = this.archive != null ? this.archive.retrieveLastId() : null;
        } catch (ArchiveException e) {
            LOG.log(Level.WARNING, "Archive not present, cannot retrieve latest stored ID");
        }
        if (lastStoredUniqueId == null) {
            this.sequencer.set(0);
        } else {
            this.sequencer.set(lastStoredUniqueId.asLong());
        }
        // Subscribe to get info about all activities
        this.activityService.subscribe(activitySubscriber, null);
    }

    @Override
    public void dispose() {
        dispatcher.submit(this::cleanUp);
        dispatcher.shutdown();
        try {
            dispatcher.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }

    private void cleanUp() {
        // Unsubscribe to processing model
        this.activityService.unsubscribe(activitySubscriber);
        if(currentEventFilter != null) {
            eventService.unsubscribe(eventSubscriber);
            currentEventFilter = null;
        }
        // Clear subscriptions
        schedulerSubscribers.clear();
        subscriberIndex.clear();
    }

    @Override
    public void initialise() {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("initialise() invoked");
        }
        // TODO initialise from archive
        // TODO start disabled (depending on system property - to be defined)
    }

    @Override
    public void subscribe(ISchedulerSubscriber subscriber) {
        schedulerSubscribers.add(subscriber);
    }

    @Override
    public void unsubscribe(ISchedulerSubscriber subscriber) {
        schedulerSubscribers.remove(subscriber);
    }

    @Override
    public void enable() {
        dispatcher.submit(() -> setEnable(true));
    }

    @Override
    public void disable() {
        dispatcher.submit(() -> setEnable(false));
    }

    private void setEnable(boolean isEnabled) {
        if(enabled != isEnabled) {
            enabled = isEnabled;
            notifier.submit(() -> schedulerSubscribers.forEach(o -> o.schedulerEnablementChanged(enabled)));
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        try {
            return dispatcher.submit(() -> scheduleTask(request, conflictStrategy)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    void internalScheduleRequest(SchedulingRequest request, CreationConflictStrategy conflictStrategy) {
        dispatcher.submit(() -> scheduleTask(request, conflictStrategy));
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param request the request to schedule
     * @param conflictStrategy the strategy resolution at creation time
     * @return the state of the created scheduled task or null if no task is created due to {@link CreationConflictStrategy#SKIP_NEW}
     * @throws SchedulingException if the operation is aborted due to {@link CreationConflictStrategy#ABORT}
     */
    private ScheduledActivityData scheduleTask(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        // Check if the creation conflict strategy allows for the scheduling
        List<ScheduledTask> conflictingTasks = computeConflicts(Collections.singletonList(request));
        if (conflictingTasks.isEmpty() || conflictStrategy == CreationConflictStrategy.ADD_ANYWAY) {
            // Add the request as-is
            return addTask(request);
        } else {
            // There is a conflict
            if (conflictStrategy == CreationConflictStrategy.ABORT) {
                // Abort the operation, leave the schedule untouched
                throw new SchedulingException("Conflict detected between the request and the following tasks: " + conflictingTasks + ", scheduling request aborted");
            } else if (conflictStrategy == CreationConflictStrategy.SKIP_NEW) {
                // Return null to indicate that the request is skipped
                return null;
            } else if (conflictStrategy == CreationConflictStrategy.REMOVE_PREVIOUS) {
                // Remove the conflicting tasks and add the new task
                removeTasks(conflictingTasks);
                return addTask(request);
            } else {
                throw new IllegalArgumentException("CreationConflictStrategy " + conflictStrategy + " not handled, software bug");
            }
        }
    }

    private ScheduledActivityData addTask(SchedulingRequest request) throws SchedulingException {
        // Create ScheduledTask
        ScheduledTask st = new ScheduledTask(this, timer, dispatcher, request);
        id2scheduledTask.put(st.getId(), st);
        // Prepare execution event depending on trigger (absolute, relative, event)
        st.updateTrigger();
        // Store and distribute
        ScheduledActivityData data = st.getCurrentData();
        storeAndDistribute(data);
        // Return scheduled activity data
        return data;
    }

    private void storeAndDistribute(ScheduledActivityData data) {
        if (this.archive != null) {
            try {
                if (data.getState() != SchedulingState.REMOVED) {
                    this.archive.store(data);
                } else {
                    this.archive.remove(data.getInternalId());
                }
            } catch (ArchiveException e) {
                LOG.log(Level.SEVERE, "Cannot store scheduled activity data " + data.getInternalId() + " inside the archive: " + e.getMessage(), e);
            }
        }
        for (Map.Entry<IScheduledActivityDataSubscriber, ScheduledActivityDataFilter> entry : subscriberIndex.entrySet()) {
            IScheduledActivityDataSubscriber sub = entry.getKey();
            ScheduledActivityDataFilter filter = entry.getValue();
            notifier.submit(() -> {
                if (filter.test(data)) {
                    sub.dataItemsReceived(Collections.singletonList(data));
                }
            });
        }
    }

    private void removeTasks(List<ScheduledTask> conflictingTasks) {
        for (ScheduledTask st : conflictingTasks) {
            removeTask(st.getId());
        }
    }

    private List<ScheduledTask> computeConflicts(List<SchedulingRequest> requests) {
        Set<IUniqueId> conflictingIds = new HashSet<>();
        List<ScheduledTask> toReturn = new LinkedList<>();
        // For each request, check if there are conflicts
        for (SchedulingRequest sr : requests) {
            Pair<Instant, Duration> timeInfo = computeTimeInformation(sr);
            for (ScheduledTask task : this.id2scheduledTask.values()) {
                if (task.conflictsWith(sr, timeInfo) && !conflictingIds.contains(task.getCurrentData().getInternalId())) {
                    toReturn.add(task);
                    conflictingIds.add(task.getCurrentData().getInternalId());
                }
            }
        }
        return toReturn;
    }

    public Pair<Instant, Duration> computeTimeInformation(SchedulingRequest sr) {
        if (sr.getTrigger() instanceof EventBasedSchedulingTrigger) {
            return Pair.of(sr.getLatestInvocationTime(), sr.getExpectedDuration());
        } else if (sr.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger trigger = (AbsoluteTimeSchedulingTrigger) sr.getTrigger();
            Duration duration = sr.getExpectedDuration();
            return Pair.of(trigger.getReleaseTime(), duration);
        } else if (sr.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            Instant triggerTime = computePredecessorsLatestEndTime(((RelativeTimeSchedulingTrigger) sr.getTrigger()).getPredecessors());
            Duration duration = sr.getExpectedDuration();
            return Pair.of(triggerTime, duration);
        } else {
            throw new IllegalArgumentException("Object " + sr.getTrigger() + " not handled");
        }
    }

    private Instant computePredecessorsLatestEndTime(List<IUniqueId> predecessors) {
        Instant latestEndTime = null;
        for (IUniqueId id : predecessors) {
            ScheduledTask task = id2scheduledTask.get(id);
            if (latestEndTime == null || task.getCurrentData().getEndTime().isAfter(latestEndTime)) {
                latestEndTime = task.getCurrentData().getEndTime();
            }
        }
        return latestEndTime;
    }

    @Override
    public List<ScheduledActivityData> schedule(List<SchedulingRequest> requests, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        try {
            return dispatcher.submit(() -> {
                List<ScheduledActivityData> toReturn = new LinkedList<>();
                List<ScheduledTask> conflictingTasks = computeConflicts(requests);
                if(!conflictingTasks.isEmpty() && conflictStrategy == CreationConflictStrategy.ABORT) {
                    throw new SchedulingException("Conflict detected with provided scheduling requests: " + conflictingTasks);
                } else {
                    for(SchedulingRequest sr : requests) {
                        ScheduledActivityData data = scheduleTask(sr, conflictStrategy);
                        if(data != null) {
                            toReturn.add(data);
                        }
                    }
                }
                return toReturn;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    @Override
    public List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException {
        try {
            return dispatcher.submit(() -> id2scheduledTask.values().stream().map(ScheduledTask::getCurrentData).sorted((o1, o2) -> {
                int compareResult = o1.getStartTime().compareTo(o2.getStartTime());
                if (compareResult == 0) {
                    return (int) (o1.getInternalId().asLong() - o2.getInternalId().asLong());
                } else {
                    return compareResult;
                }
            }).collect(Collectors.toList())).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    @Override
    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(IUniqueId scheduledId) throws SchedulingException {
        try {
            dispatcher.submit(() -> removeTask(scheduledId)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    @Override
    public void remove(ScheduledActivityDataFilter filter) throws SchedulingException {
        try {
            dispatcher.submit(() -> {
                Set<IUniqueId> toRemove = new HashSet<>();
                for (Map.Entry<IUniqueId, ScheduledTask> entry : id2scheduledTask.entrySet()) {
                    if (filter.test(entry.getValue().getCurrentData())) {
                        toRemove.add(entry.getKey());
                    }
                }
                for (IUniqueId id : toRemove) {
                    removeTask(id);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    /**
     * To be called by the dispatcher thread.
     *
     * @param scheduledId the scheduled task to remove
     */
    public void removeTask(IUniqueId scheduledId) {
        // remove from the current internal set
        ScheduledTask st = id2scheduledTask.remove(scheduledId);
        if (st != null) {
            st.abortTask();
            // update or remove in the archive
            ScheduledActivityData sad = st.getCurrentData();
            storeAndDistribute(sad);
        }
    }

    @Override
    public List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        try {
            return dispatcher.submit(() -> {
                List<ScheduledActivityData> toReturn = new LinkedList<>();
                // First, retrieve all scheduled activities from startTime to endTime
                List<ScheduledTask> tasks = retrieveTasksFrom(startTime, endTime);
                // Now, check if there is one task belonging to source, that is not SCHEDULED. If so, abort with exception.
                Optional<ScheduledTask> runningTask = tasks.stream().filter(o -> o.getCurrentData().getSource().equals(source)).filter(o -> !o.getCurrentData().getState().equals(SchedulingState.SCHEDULED)).findFirst();
                if(runningTask.isPresent()) {
                    throw new SchedulingException("Task " + runningTask.get().getId() + " already in state " + runningTask.get().getCurrentData().getState() + ", cannot replace the schedule period");
                }
                // If you are here, there is no scheduled task from the provided source that started, so they can be all removed. Let's check for conflicts.
                List<ScheduledTask> conflictingTasks = computeConflicts(requests);
                // Remove the tasks of this source, they will go away
                conflictingTasks.removeIf(o -> o.getCurrentData().getSource().equals(source));
                // Final check
                if(!conflictingTasks.isEmpty() && conflictStrategy == CreationConflictStrategy.ABORT) {
                    throw new SchedulingException("Conflict detected with provided scheduling requests: " + conflictingTasks);
                }
                // OK, at this stage the import can be handled.
                // Remove the tasks from source
                for(ScheduledTask st : tasks) {
                    if(st.getCurrentData().getSource().equals(source)) {
                        removeTask(st.getId());
                    }
                }
                // Add new tasks
                for(SchedulingRequest sr : requests) {
                    ScheduledActivityData data = scheduleTask(sr, conflictStrategy);
                    if(data != null) {
                        toReturn.add(data);
                    }
                }
                return toReturn;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    private List<ScheduledTask> retrieveTasksFrom(Instant startTime, Instant endTime) {
        return id2scheduledTask.values().stream()
                .filter(o -> (o.getCurrentData().getStartTime().isAfter(startTime) || o.getCurrentData().getStartTime().equals(startTime)) &&
                        (o.getCurrentData().getStartTime().isBefore(endTime) || o.getCurrentData().getStartTime().equals(endTime)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant time, ScheduledActivityDataFilter filter) throws ReatmetricException {
        if (archive == null) {
            throw new ReatmetricException("Archive not available");
        }
        return archive.retrieve(time, filter, time.minusSeconds(3600 * 36));
    }

    @Override
    public void subscribe(IScheduledActivityDataSubscriber subscriber, ScheduledActivityDataFilter filter) {
        dispatcher.submit(() -> {
            this.subscriberIndex.put(subscriber, filter);
        });
    }

    @Override
    public void unsubscribe(IScheduledActivityDataSubscriber subscriber) {
        dispatcher.submit(() -> {
            this.subscriberIndex.remove(subscriber);
        });
    }

    @Override
    public List<ScheduledActivityData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        if (archive == null) {
            throw new ReatmetricException("Archive not available");
        }
        return archive.retrieve(startTime, numRecords, direction, filter);
    }

    @Override
    public List<ScheduledActivityData> retrieve(ScheduledActivityData startItem, int numRecords, RetrievalDirection direction, ScheduledActivityDataFilter filter) throws ReatmetricException {
        if (archive == null) {
            throw new ReatmetricException("Archive not available");
        }
        return archive.retrieve(startItem, numRecords, direction, filter);
    }

    IUniqueId getNextId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    /**
     * To be called from the dispatcher thread.
     */
    void updateEventFilter(Integer newEvent, boolean remove) {
        if(remove) {
            subscribedEvents.remove(newEvent);
        } else {
            subscribedEvents.add(newEvent);
        }
        if(subscribedEvents.isEmpty()) {
            currentEventFilter = null;
            eventService.unsubscribe(eventSubscriber);
        } else {
            currentEventFilter = new EventDataFilter(null, null, null, null, null, null, subscribedEvents);
            eventService.subscribe(eventSubscriber, currentEventFilter);
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    boolean areAllCompleted(List<IUniqueId> predecessors) {
        for (IUniqueId id : predecessors) {
            if (id2scheduledTask.containsKey(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * To be called from the dispatcher thread.
     */
    boolean registerResources(Set<String> resources) {
        if(Collections.disjoint(this.currentlyUsedResources, resources)) {
            this.currentlyUsedResources.addAll(resources);
            return true;
        } else {
            return false;
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    IUniqueId startActivity(ActivityRequest request) throws ReatmetricException {
        return executionService.startActivity(request);
    }

    /**
     * To be called from the dispatcher thread.
     */
    void releaseResources(Set<String> resources) {
        this.currentlyUsedResources.removeAll(resources);
        // Check waiting tasks
        for(ScheduledTask st : id2scheduledTask.values()) {
            if(st.getCurrentData().getState() == SchedulingState.WAITING) {
                st.evaluateRun();
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void notifyTask(ScheduledTask scheduledTask) {
        storeAndDistribute(scheduledTask.getCurrentData());
    }

    /**
     * To be called from the dispatcher thread.
     */
    void abortConflictingTasksWith(ScheduledTask scheduledTask) {
        List<ScheduledTask> conflictingTasks = computeConflicts(Collections.singletonList(scheduledTask.getRequest()));
        // Remove this task
        conflictingTasks.remove(scheduledTask);
        // Remove all tasks that are still in SCHEDULED or WAITING
        conflictingTasks.removeIf(o -> o.getCurrentData().getState() == SchedulingState.SCHEDULED || o.getCurrentData().getState() == SchedulingState.WAITING);
        // Remove remaining tasks
        removeTasks(conflictingTasks);
    }

    /**
     * To be called from the dispatcher thread.
     */
    void eventUpdate(List<EventData> dataItems) {
        for(EventData ed : dataItems) {
            for (ScheduledTask st : id2scheduledTask.values()) {
                st.newEventOccurrence(ed);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void activityUpdate(List<ActivityOccurrenceData> dataItems) {
        for(ActivityOccurrenceData aod : dataItems) {
            ScheduledTask st = activityId2scheduledTask.get(aod.getInternalId());
            if(st != null) {
                st.activityStatusUpdate(aod);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void deregisterActivity(IUniqueId activityId) {
        this.activityId2scheduledTask.remove(activityId);
    }

    /**
     * To be called from the dispatcher thread.
     */
    void registerActivity(IUniqueId activityId, ScheduledTask scheduledTask) {
        this.activityId2scheduledTask.put(activityId, scheduledTask);
    }

    /**
     * To be called from the dispatcher thread.
     */
    void abortActivity(IUniqueId activityId) {
        // TODO: introduce abort operation in all the TC chain
    }
}
