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

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.impl.ScheduledTask;

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
    private final IProcessingModel processingModel;

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
     * The set if resources currently taken by running scheduled tasks.
     */
    private final Set<String> currentlyUsedResources = new TreeSet<>();

    private volatile boolean enabled;

    public Scheduler(IArchive archive, IProcessingModel model) {
        if (archive != null) {
            this.archive = archive.getArchive(IScheduledActivityDataArchive.class);
        } else {
            this.archive = null;
        }
        this.processingModel = model;
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
    }

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
        // TODO unsubscribe to processing model
        // Clear subscriptions
        schedulerSubscribers.clear();
    }

    @Override
    public void initialise() throws SchedulingException {
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
    public void enable() throws SchedulingException {
        dispatcher.submit(() -> {
            setEnable(true);
        });
    }

    @Override
    public void disable() throws SchedulingException {
        dispatcher.submit(() -> {
            setEnable(false);
        });
    }

    private void setEnable(boolean isEnabled) {
        if (enabled != isEnabled) {
            enabled = isEnabled;
            for (ScheduledTask st : id2scheduledTask.values()) {
                st.informEnablementChange(isEnabled);
            }
        }
    }

    @Override
    public synchronized boolean isEnabled() throws SchedulingException {
        return enabled;
    }

    @Override
    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        try {
            return dispatcher.submit(() -> {
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
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
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
            Pair<Instant, Instant> timeInfo = computeTimeInformation(sr);
            for (ScheduledTask task : this.id2scheduledTask.values()) {
                if (task.conflictsWith(sr, timeInfo) && !conflictingIds.contains(task.getCurrentData().getInternalId())) {
                    toReturn.add(task);
                    conflictingIds.add(task.getCurrentData().getInternalId());
                }
            }
        }
        return toReturn;
    }

    public Pair<Instant, Instant> computeTimeInformation(SchedulingRequest sr) {
        if (sr.getTrigger() instanceof EventBasedSchedulingTrigger) {
            return Pair.of(Instant.EPOCH, Instant.MAX);
        } else if (sr.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger trigger = (AbsoluteTimeSchedulingTrigger) sr.getTrigger();
            Duration duration = sr.getExpectedDuration();
            return Pair.of(trigger.getReleaseTime(), trigger.getReleaseTime().plus(duration));
        } else if (sr.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            Instant triggerTime = computePredecessorsLatestEndTime(((RelativeTimeSchedulingTrigger) sr.getTrigger()).getPredecessors());
            Duration duration = sr.getExpectedDuration();
            return Pair.of(triggerTime, triggerTime.plus(duration));
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
    public List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        // TODO
        throw new UnsupportedOperationException();
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
    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(IUniqueId scheduledId) throws SchedulingException {
        try {
            dispatcher.submit(() -> {
                removeTask(scheduledId);
            }).get();
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

    private void removeTask(IUniqueId scheduledId) {
        // remove from the current internal set
        ScheduledTask st = id2scheduledTask.remove(scheduledId);
        if (st != null) {
            st.abortTask();
            // remove from the archive, in case
            ScheduledActivityData sad = st.getCurrentData();
            storeAndDistribute(sad);
        }
    }

    @Override
    public List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) {
        // TODO
        throw new UnsupportedOperationException();
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

    public IUniqueId getNextId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param event
     */
    public void updateEventFilter(int event) {
        // TODO: subscribe or update subscription for events
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param predecessors
     * @return
     */
    public boolean areAllCompleted(List<IUniqueId> predecessors) {
        for (IUniqueId id : predecessors) {
            if (id2scheduledTask.containsKey(id)) {
                return false;
            }
        }
        return true;
    }

}
