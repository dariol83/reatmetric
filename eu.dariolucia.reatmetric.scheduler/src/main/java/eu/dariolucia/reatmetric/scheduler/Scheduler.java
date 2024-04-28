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

import eu.dariolucia.reatmetric.api.activity.*;
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
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.scheduler.definition.BotProcessingDefinition;
import eu.dariolucia.reatmetric.scheduler.definition.SchedulerConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Scheduler implements IScheduler, IInternalResolver {

    private static final Logger LOG = Logger.getLogger(Scheduler.class.getName());

    private final SchedulerConfiguration configuration;
    private final IScheduledActivityDataArchive archive;
    private final IActivityExecutionService executionService;
    private final IEventDataProvisionService eventService;
    private final IActivityOccurrenceDataProvisionService activityService;
    private final IParameterDataProvisionService parameterService;
    private final List<ISchedulerSubscriber> schedulerSubscribers = new CopyOnWriteArrayList<>();
    private final Map<IScheduledActivityDataSubscriber, ScheduledActivityDataFilter> subscriberIndex = new LinkedHashMap<>();
    private final AtomicLong sequencer = new AtomicLong(0);

    /**
     * A timer that reports events into the eventQueue, based on time-based, absolute constraints.
     */
    private final Timer timer = new Timer("Reatmetric Scheduler Timer", true);
    /**
     * The dispatcher thread.
     */
    private final ExecutorService dispatcher = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "Reatmetric Scheduler Dispatcher Thread");
        t.setDaemon(true);
        return t;
    });
    /**
     * The notifier thread.
     */
    private final ExecutorService notifier = Executors.newSingleThreadExecutor((r) -> {
        Thread t = new Thread(r, "Reatmetric Scheduler Notifier Thread");
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
    private final List<SystemEntityPath> subscribedEvents = new LinkedList<>();
    /**
     * Keeps track of the scheduler enablement state
     */
    private volatile boolean enabled;
    /**
     * Parameter states involved in the bot processing
     */
    private final Map<String, ParameterData> cachedParameterMap = new ConcurrentHashMap<>();

    private final IActivityOccurrenceDataSubscriber activitySubscriber = dataItems -> dispatcher.submit(() -> activityUpdate(dataItems));

    private final IEventDataSubscriber eventSubscriber = dataItems -> dispatcher.submit(() -> eventUpdate(dataItems));

    private final IParameterDataSubscriber parameterSubscriber = dataItems -> dispatcher.submit(() -> parameterUpdate(dataItems));

    private EventDataFilter currentEventFilter = null;

    public Scheduler(String configurationLocation, IArchive archive, IActivityExecutionService activityExecutor, IEventDataProvisionService eventMonService, IActivityOccurrenceDataProvisionService activityMonService, IParameterDataProvisionService parameterService) {
        SchedulerConfiguration tempConfiguration = null;
        if(configurationLocation != null) {
            try {
                tempConfiguration = SchedulerConfiguration.load(new FileInputStream(configurationLocation));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot load scheduler configuration at " + configurationLocation + ": " + e.getMessage(), e);
            }
        }
        this.configuration = tempConfiguration;

        if (archive != null) {
            this.archive = archive.getArchive(IScheduledActivityDataArchive.class);
        } else {
            this.archive = null;
        }
        this.executionService = activityExecutor;
        this.eventService = eventMonService;
        this.activityService = activityMonService;
        this.parameterService = parameterService;
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

    @Override
    public void dispose() {
        LOG.info("Disposing scheduler");
        dispatcher.submit(this::cleanUp);
        dispatcher.shutdown();
        try {
            dispatcher.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Scheduler disposed");
        }
    }

    private void cleanUp() {
        // Unsubscribe to processing model
        try {
            this.activityService.unsubscribe(activitySubscriber);
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Activity Service remote exception: " + e.getMessage(), e);
        }
        if (currentEventFilter != null) {
            try {
                eventService.unsubscribe(eventSubscriber);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Event Service remote exception: " + e.getMessage(), e);
            }
            currentEventFilter = null;
        }
        //
        if(configuration != null) {
            Set<String> paramsToSubscribe = new HashSet<>();
            for (BotProcessingDefinition bpd : configuration.getBots()) {
                paramsToSubscribe.addAll(bpd.getMonitoredParameters());
            }
            if(!paramsToSubscribe.isEmpty()) {
                try {
                    parameterService.unsubscribe(parameterSubscriber);
                } catch (RemoteException e) {
                    LOG.log(Level.SEVERE, "Parameter Service remote exception: " + e.getMessage(), e);
                }
            }
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
        // Start disabled or enabled
        if(configuration == null || configuration.isSchedulerEnabled()) {
            enable();
        } else {
            disable();
        }
        // Subscribe to get info about all activities
        try {
            this.activityService.subscribe(activitySubscriber, null);
        } catch (RemoteException e) {
            LOG.log(Level.SEVERE, "Activity Service remote exception: " + e.getMessage(), e);
        }
        // Initialise from archive
        if (archive != null) {
            dispatcher.submit(this::initFromArchive);
        }
        // Subscribe for parameters defined in the bot definitions
        if(configuration != null) {
            Set<String> paramsToSubscribe = new HashSet<>();
            for (BotProcessingDefinition bpd : configuration.getBots()) {
                paramsToSubscribe.addAll(bpd.getMonitoredParameters());
            }
            if(!paramsToSubscribe.isEmpty()) {
                ParameterDataFilter filter = new ParameterDataFilter(null, paramsToSubscribe.stream().map(SystemEntityPath::fromString).collect(Collectors.toList()), null, null, null,null);
                try {
                    // This will generate a callback with all the initial values of the required parameters
                    parameterService.subscribe(parameterSubscriber, filter);
                } catch (RemoteException e) {
                    LOG.log(Level.SEVERE, "Parameter Service remote exception: " + e.getMessage(), e);
                }
            }
        }
    }

    private void initFromArchive() {
        // Retrieve all scheduled activities with state RUNNING, WAITING, SCHEDULED
        // Transition the RUNNING and WAITING activities in:
        // RUNNING -> UNKNOWN
        // WAITING -> ABORTED
        // Transition activities that are SCHEDULED and scheduled time is in the past --> ABORTED
        // See method restoreActivitiesFromList
        try {
            // This part retrieves everything but not the event-based scheduled activities, for which the generation time is set
            // to EPOCH.
            List<ScheduledActivityData> scheduledItems = archive.retrieve(Instant.now().plusSeconds(3600 * 24 * 365), // End time: one year in the future
                    new ScheduledActivityDataFilter(null, null, null, null,
                            Arrays.asList(SchedulingState.SCHEDULED, SchedulingState.WAITING, SchedulingState.RUNNING), null),
                    Instant.now().minusSeconds(36 * 3600L)); // Start time: 36 hours in the past

            restoreActivitiesFromList(scheduledItems);

            // Now it is the time of the event-based activities, start and end time set to EPOCH
            scheduledItems = archive.retrieve(Instant.EPOCH,
                    new ScheduledActivityDataFilter(null, null, null, null,
                            Collections.singletonList(SchedulingState.SCHEDULED), null),
                    Instant.EPOCH);

            restoreActivitiesFromList(scheduledItems);

        } catch (ArchiveException | SchedulingException e) {
            LOG.log(Level.SEVERE, "Cannot restore scheduler state from archive: " + e.getMessage(), e);
        }
    }

    private void restoreActivitiesFromList(List<ScheduledActivityData> scheduledItems) throws SchedulingException {
        Instant now = Instant.now();
        // Partition: first schedule all the AbsoluteTimeSchedulingTrigger and the others are in another group
        List<ScheduledActivityData> absoluteScheduled = scheduledItems.stream().filter(i -> i.getTrigger() instanceof AbsoluteTimeSchedulingTrigger).collect(Collectors.toList());
        List<ScheduledActivityData> relativeScheduled = scheduledItems.stream().filter(i -> i.getTrigger() instanceof RelativeTimeSchedulingTrigger).collect(Collectors.toList());
        List<ScheduledActivityData> otherScheduled = scheduledItems.stream().filter(i -> !(i.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) &&
                !(i.getTrigger() instanceof RelativeTimeSchedulingTrigger)).collect(Collectors.toList());
        // Sort the relative-scheduled items
        relativeScheduled = buildSortedRelativeScheduled(relativeScheduled);
        // Restore items
        restoreActivities(absoluteScheduled, now);
        restoreActivities(otherScheduled, now);
        restoreActivities(relativeScheduled, now);
    }

    /**
     * The idea of this method is to build a list of scheduled activities, which would be able to resolve their predecessors
     * as they will be added to the scheduler. Therefore, the list will first have the activities without predecessors
     * (or with predecessors of other types). And only then, the activities with predecessors also of type relative-time,
     * in the correct order.
     * @param relativeScheduled the list to order
     * @return the ordered list
     */
    private List<ScheduledActivityData> buildSortedRelativeScheduled(List<ScheduledActivityData> relativeScheduled) {
        List<ScheduledActivityData> relativeScheduledToReturn = new ArrayList<>(relativeScheduled.size());
        Queue<ScheduledActivityData> toProcess = new LinkedList<>(relativeScheduled);
        int pushBackIterations = 0;
        while(!toProcess.isEmpty()) {
            // Get the top
            ScheduledActivityData d = toProcess.remove();
            RelativeTimeSchedulingTrigger trigger = (RelativeTimeSchedulingTrigger) d.getTrigger();
            // Get the referenced activities
            boolean backInQueue = false;
            for(String pred : trigger.getPredecessors()) {
                // Look for predecessor in the queue: if it is there, then put the item back in the queue
                for(ScheduledActivityData sad : toProcess) {
                    if(sad.getExternalId().equals(pred)) {
                        toProcess.add(d);
                        backInQueue = true;
                        ++pushBackIterations;
                        break;
                    }
                }
                if(backInQueue) {
                    break;
                }
            }
            if(!backInQueue) {
                relativeScheduledToReturn.add(d);
                pushBackIterations = 0;
            }
            if(pushBackIterations > toProcess.size() + 1) {
                LOG.warning("Cycle detected when restoring scheduled items with relative time");
                // Cycle, stop and copy all remaining data in the list to return.
                // It should not happen.
                relativeScheduledToReturn.addAll(toProcess);
                break;
            }
        }
        return relativeScheduledToReturn;
    }

    private void restoreActivities(List<ScheduledActivityData> scheduledItems, Instant now) throws SchedulingException {
        for (ScheduledActivityData item : scheduledItems) {
            if (item.getState() == SchedulingState.SCHEDULED) {
                if(isToBeScheduled(item, now)) {
                    LOG.info("Restoring scheduled activity: " + item);
                    // Create ScheduledTask
                    ScheduledTask st = new ScheduledTask(this, timer, dispatcher, item);
                    id2scheduledTask.put(st.getId(), st);
                    // Prepare execution event depending on trigger (absolute, relative, event)
                    st.armTrigger();
                } else {
                    LOG.warning("Scheduled activity not restored (ABORTED): " + item);
                    // ABORTED, it will never start
                    storeAndDistribute(new ScheduledActivityData(item.getInternalId(),
                            item.getGenerationTime(),
                            item.getRequest(),
                            item.getActivityOccurrence(),
                            item.getResources(),
                            item.getSource(),
                            item.getExternalId(),
                            item.getTrigger(),
                            item.getLatestInvocationTime(),
                            item.getStartTime(),
                            item.getDuration(),
                            item.getConflictStrategy(),
                            SchedulingState.ABORTED,
                            item.getExtension()));
                }
            } else if (item.getState() == SchedulingState.RUNNING) {
                LOG.warning("Scheduled running activity restored with status UNKNOWN: " + item);
                storeAndDistribute(new ScheduledActivityData(item.getInternalId(),
                        item.getGenerationTime(),
                        item.getRequest(),
                        item.getActivityOccurrence(),
                        item.getResources(),
                        item.getSource(),
                        item.getExternalId(),
                        item.getTrigger(),
                        item.getLatestInvocationTime(),
                        item.getStartTime(),
                        item.getDuration(),
                        item.getConflictStrategy(),
                        SchedulingState.UNKNOWN,
                        item.getExtension()));
            } else if (item.getState() == SchedulingState.WAITING) {
                LOG.warning("Scheduled waiting activity restored with status ABORTED: " + item);
                storeAndDistribute(new ScheduledActivityData(item.getInternalId(),
                        item.getGenerationTime(),
                        item.getRequest(),
                        item.getActivityOccurrence(),
                        item.getResources(),
                        item.getSource(),
                        item.getExternalId(),
                        item.getTrigger(),
                        item.getLatestInvocationTime(),
                        item.getStartTime(),
                        item.getDuration(),
                        item.getConflictStrategy(),
                        SchedulingState.ABORTED,
                        item.getExtension()));
            }
        }
    }

    private boolean isToBeScheduled(ScheduledActivityData item, Instant now) {
        if(this.configuration.isRunPastScheduledActivities()) {
            return true;
        } else if(item.getTrigger() instanceof EventBasedSchedulingTrigger) {
            // Always schedule
            return true;
        } else if(item.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            Instant expectedStart = ((AbsoluteTimeSchedulingTrigger) item.getTrigger()).getReleaseTime();
            return expectedStart.equals(now) || expectedStart.isAfter(now);
        } else if(item.getTrigger() instanceof NowSchedulingTrigger) {
            // NowSchedulingTrigger are inaccurate, they were supposed to start right away, so at this stage do not restore
            return false;
        } else if(item.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            RelativeTimeSchedulingTrigger trigger = (RelativeTimeSchedulingTrigger) item.getTrigger();
            // Check that predecessors are all in the schedule and with right state
            for(String extId : trigger.getPredecessors()) {
                boolean found = false;
                for(ScheduledTask st : id2scheduledTask.values()) {
                    if(Objects.equals(st.getCurrentData().getExternalId(), extId)) {
                        found = true;
                        // If the status is SCHEDULED --> OK, else return false
                        if(st.getCurrentData().getState() != SchedulingState.SCHEDULED) {
                            return false;
                        }
                        break;
                    }
                }
                if(!found) {
                    // Well, in the past or non-existing --> do not schedule
                    return false;
                }
            }
            return true;
        } else {
            // ???
            return false;
        }
    }

    @Override
    public void subscribe(ISchedulerSubscriber subscriber) {
        dispatcher.submit(() -> {
            schedulerSubscribers.add(subscriber);
            // The bots now
            List<BotStateData> data = new LinkedList<>();
            if(configuration != null) {
                data.addAll(configuration.getBots().stream().map(BotProcessingDefinition::getCurrentState).collect(Collectors.toList()));
            }
            notifier.submit(() -> {
                try {
                    subscriber.schedulerEnablementChanged(isEnabled());
                    if(!data.isEmpty()) {
                        subscriber.botStateUpdated(data);
                    }
                } catch (RemoteException e) {
                    LOG.log(Level.SEVERE, "Subscriber remote exception, dropping it");
                    unsubscribe(subscriber);
                }
            });
        });
    }

    @Override
    public void unsubscribe(ISchedulerSubscriber subscriber) {
        dispatcher.submit(() -> {
            schedulerSubscribers.remove(subscriber);
        });
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
        if (enabled != isEnabled) {
            enabled = isEnabled;
            for (ISchedulerSubscriber ss : schedulerSubscribers) {
                notifier.submit(() -> {
                    try {
                        ss.schedulerEnablementChanged(enabled);
                    } catch (Exception e) {
                        unsubscribe(ss);
                    }
                });
            }
        }
        LOG.log(Level.INFO, "Scheduler " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        LOG.info("Request to schedule activity " + request.getRequest().getPath() + " (" + request.getExternalId() + ") with trigger " + request.getTrigger() + " received");
        try {
            return dispatcher.submit(() -> scheduleTask(request, conflictStrategy, null)).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "Scheduling request for activity " + request.getRequest().getPath() + " (" + request.getExternalId() + ") encountered a problem: " + e.getMessage(), e);
            throw new SchedulingException(e);
        }
    }

    void internalScheduleRequest(List<SchedulingRequest> requests, CreationConflictStrategy conflictStrategy) {
        dispatcher.submit(() -> {
            for(SchedulingRequest request : requests) {
                try {
                    scheduleTask(request, conflictStrategy, null);
                } catch (SchedulingException e) {
                    LOG.log(Level.WARNING, "Cannot schedule request " + request.getExternalId() + ": " + e.getMessage(), e);
                }
            }
        });
    }

    void internalScheduleRequest(SchedulingRequest request, CreationConflictStrategy conflictStrategy) {
        dispatcher.submit(() -> scheduleTask(request, conflictStrategy, null));
    }

    /**
     * To be called from the dispatcher thread.
     *
     * @param request          the request to schedule
     * @param conflictStrategy the strategy resolution at creation time
     * @param originalId       id to use, can be null
     * @return the state of the created scheduled task or null if no task is created due to {@link CreationConflictStrategy#SKIP_NEW}
     * @throws SchedulingException if the operation is aborted due to {@link CreationConflictStrategy#ABORT}
     */
    private ScheduledActivityData scheduleTask(SchedulingRequest request, CreationConflictStrategy conflictStrategy, IUniqueId originalId) throws SchedulingException {
        // Check if an external ID exists already
        for(ScheduledTask st : id2scheduledTask.values()) {
            if(Objects.equals(st.getCurrentData().getExternalId(),request.getExternalId())) {
                throw new SchedulingException("Supplied external ID is already assigned to one scheduled activity: " + request.getExternalId());
            }
        }
        // Check if the creation conflict strategy allows for the scheduling
        List<ScheduledTask> conflictingTasks = computeConflicts(Collections.singletonList(request));
        if (conflictingTasks.isEmpty() || conflictStrategy == CreationConflictStrategy.ADD_ANYWAY) {
            // Add the request as-is
            ScheduledActivityData toReturn = addTask(request, originalId);
            // Update scheduled relative-time activities
            updateRelativeTimeStartTime(toReturn.getExternalId());
            return toReturn;
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
                ScheduledActivityData toReturn = addTask(request, originalId);
                updateRelativeTimeStartTime(toReturn.getExternalId());
                return toReturn;
            } else {
                throw new IllegalArgumentException("CreationConflictStrategy " + conflictStrategy + " not handled, software bug");
            }
        }
    }

    /**
     * To be called in the dispatcher thread.
     *
     * @param externalId the modified task external ID
     */
    private void updateRelativeTimeStartTime(String externalId) {
        Set<IUniqueId> alreadyProcessedTasks = new HashSet<>();
        Queue<ScheduledTask> updatedTasks = new LinkedList<>();
        ScheduledTask toStart = lookUpScheduledTaskByExternalId(externalId);
        updatedTasks.add(toStart);
        while(!updatedTasks.isEmpty()) {
            ScheduledTask toCheck = updatedTasks.poll();
            if(alreadyProcessedTasks.contains(toCheck.getId())) {
                continue;
            }
            alreadyProcessedTasks.add(toCheck.getId());
            // Now check
            for(ScheduledTask task : id2scheduledTask.values()) {
                if(task.getCurrentData().getState() == SchedulingState.SCHEDULED && task.getCurrentData().getTrigger() instanceof RelativeTimeSchedulingTrigger) {
                    if(((RelativeTimeSchedulingTrigger) task.getCurrentData().getTrigger()).getPredecessors().contains(toCheck.getCurrentData().getExternalId())) {
                        // Task potentially affected, start time to be recomputed
                        boolean updated = task.updateStartTime();
                        if(updated) {
                            updatedTasks.add(task);
                        }
                    }
                }
            }
        }
    }

    private ScheduledActivityData addTask(SchedulingRequest request, IUniqueId originalId) throws SchedulingException {
        // Create ScheduledTask
        ScheduledTask st = new ScheduledTask(this, timer, dispatcher, request, originalId);
        id2scheduledTask.put(st.getId(), st);
        // Prepare execution event depending on trigger (absolute, relative, event, now)
        st.armTrigger();
        // Store and distribute
        ScheduledActivityData data = st.getCurrentData();
        storeAndDistribute(data);
        // Return scheduled activity data
        return data;
    }

    private void storeAndDistribute(ScheduledActivityData data) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Distributing and storing " + data);
        }
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
                if (filter == null || filter.test(data)) {
                    try {
                        sub.dataItemsReceived(Collections.singletonList(data));
                    } catch (RemoteException e) {
                        LOG.log(Level.SEVERE, "Error when notifying activity data subscriber, dropping it", e);
                        unsubscribe(sub);
                    }
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
            if(sr.getTrigger() instanceof EventBasedSchedulingTrigger) {
                // No conflict by definition
                continue;
            }
            Pair<Instant, Duration> timeInfo = computeTimeInformation(false, sr);
            for (ScheduledTask task : this.id2scheduledTask.values()) {
                if (task.conflictsWith(sr, timeInfo) && !conflictingIds.contains(task.getCurrentData().getInternalId())) {
                    toReturn.add(task);
                    conflictingIds.add(task.getCurrentData().getInternalId());
                }
            }
        }
        return toReturn;
    }

    public Pair<Instant, Duration> computeTimeInformation(boolean isInitRequest, SchedulingRequest sr) {
        if (sr.getTrigger() instanceof EventBasedSchedulingTrigger) {
            if(isInitRequest) {
                return Pair.of(Instant.EPOCH, sr.getExpectedDuration());
            } else {
                throw new IllegalArgumentException("Event-based trigger cannot have a refreshed start time");
            }
        } else if (sr.getTrigger() instanceof AbsoluteTimeSchedulingTrigger) {
            AbsoluteTimeSchedulingTrigger trigger = (AbsoluteTimeSchedulingTrigger) sr.getTrigger();
            Duration duration = sr.getExpectedDuration();
            return Pair.of(trigger.getReleaseTime(), duration);
        } else if (sr.getTrigger() instanceof NowSchedulingTrigger) {
            Duration duration = sr.getExpectedDuration();
            return Pair.of(Instant.now(), duration);
        } else if (sr.getTrigger() instanceof RelativeTimeSchedulingTrigger) {
            Instant triggerTime = computePredecessorsLatestEndTime(((RelativeTimeSchedulingTrigger) sr.getTrigger()).getPredecessors());
            triggerTime = triggerTime.plusSeconds(((RelativeTimeSchedulingTrigger) sr.getTrigger()).getDelayTime());
            Duration duration = sr.getExpectedDuration();
            return Pair.of(triggerTime, duration);
        } else {
            throw new IllegalArgumentException("Object " + sr.getTrigger() + " not handled");
        }
    }

    private Instant computePredecessorsLatestEndTime(Set<String> predecessors) {
        Instant latestEndTime = Instant.now();
        for (String id : predecessors) {
            ScheduledTask task = lookUpScheduledTaskByExternalId(id);
            if (task == null) {
                // No task, continue
                continue;
            }
            if (latestEndTime == null || task.getCurrentData().getEndTime().isAfter(latestEndTime)) {
                latestEndTime = task.getCurrentData().getEndTime();
            }
        }
        return latestEndTime;
    }

    private ScheduledTask lookUpScheduledTaskByExternalId(String externalId) {
        for (ScheduledTask st : id2scheduledTask.values()) {
            if (st.getRequest().getExternalId().equals(externalId)) {
                return st;
            }
        }
        return null;
    }

    @Override
    public List<ScheduledActivityData> schedule(List<SchedulingRequest> requests, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        LOG.info("Request to schedule " + requests.size() + " activities received");
        try {
            return dispatcher.submit(() -> {
                List<ScheduledActivityData> toReturn = new LinkedList<>();
                List<ScheduledTask> conflictingTasks = computeConflicts(requests);
                if (!conflictingTasks.isEmpty() && conflictStrategy == CreationConflictStrategy.ABORT) {
                    throw new SchedulingException("Conflict detected with provided scheduling requests: " + conflictingTasks);
                } else {
                    // Check if an external ID exists already
                    Set<String> toBeAdded = requests.stream().map(SchedulingRequest::getExternalId).collect(Collectors.toSet());
                    if(toBeAdded.size() != requests.size()) {
                        throw new SchedulingException("One supplied external ID is duplicated in the request");
                    }
                    for(ScheduledTask st : id2scheduledTask.values()) {
                        if(toBeAdded.contains(st.getCurrentData().getExternalId())) {
                            throw new SchedulingException("Supplied external ID is already assigned to one scheduled activity: " + st.getCurrentData().getExternalId());
                        }
                    }
                    for (SchedulingRequest sr : requests) {
                        ScheduledActivityData data = scheduleTask(sr, conflictStrategy, null);
                        if (data != null) {
                            toReturn.add(data);
                        }
                    }
                }
                return toReturn;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "Scheduling request for activities encountered a problem: " + e.getMessage(), e);
            throw new SchedulingException(e);
        }
    }

    @Override
    public List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException {
        try {
            return dispatcher.submit(this::internalGetCurrentScheduledActivities).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "Schedule state retrieval request encountered a problem: " + e.getMessage(), e);
            throw new SchedulingException(e);
        }
    }

    private List<ScheduledActivityData> internalGetCurrentScheduledActivities() {
        return id2scheduledTask.values().stream().map(ScheduledTask::getCurrentData).sorted((o1, o2) -> {
            int compareResult = o1.getStartTime().compareTo(o2.getStartTime());
            if (compareResult == 0) {
                return (int) (o1.getInternalId().asLong() - o2.getInternalId().asLong());
            } else {
                return compareResult;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException {
        LOG.info("Request to update scheduled activity " + originalId + " with new request for activity " + newRequest.getRequest().getPath() + " (" + newRequest.getExternalId() + ") with trigger " + newRequest.getTrigger() + " received");
        try {
            return dispatcher.submit(() -> {
                ScheduledTask st = id2scheduledTask.get(originalId);
                if (st == null) {
                    throw new SchedulingException("Task " + originalId + " not found, cannot update");
                }
                if (st.getCurrentData().getState() != SchedulingState.SCHEDULED) {
                    throw new SchedulingException("Task " + originalId + " not in scheduled state, cannot update");
                }
                // Get the conflicting tasks of the new request
                List<ScheduledTask> conflicts = computeConflicts(Collections.singletonList(newRequest));
                // Remove self
                conflicts.remove(st);
                if (!conflicts.isEmpty() && conflictStrategy == CreationConflictStrategy.ABORT) {
                    throw new SchedulingException("Conflict detected with provided scheduling requests: " + conflicts);
                } else {
                    removeTask(originalId);
                    return scheduleTask(newRequest, conflictStrategy, originalId);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "Scheduling update request for activity " + originalId + " encountered a problem: " + e.getMessage(), e);
            throw new SchedulingException(e);
        }
    }

    @Override
    public void remove(IUniqueId scheduledId) throws SchedulingException {
        LOG.info("Request to remove scheduled activity " + scheduledId + " received");
        try {
            dispatcher.submit(() -> removeTask(scheduledId)).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.log(Level.SEVERE, "Scheduling remove request for activity " + scheduledId + " encountered a problem: " + e.getMessage(), e);
            throw new SchedulingException(e);
        }
    }

    @Override
    public void remove(ScheduledActivityDataFilter filter) throws SchedulingException {
        LOG.info("Request to remove scheduled activities with filter " + filter + " received");
        try {
            dispatcher.submit(() -> {
                Set<IUniqueId> toRemove = new HashSet<>();
                for (Map.Entry<IUniqueId, ScheduledTask> entry : id2scheduledTask.entrySet()) {
                    if (filter == null || filter.test(entry.getValue().getCurrentData())) {
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
    void removeTask(IUniqueId scheduledId) {
        // remove from the current internal set
        ScheduledTask st = id2scheduledTask.remove(scheduledId);
        if (st != null) {
            LOG.info(String.format("Removing scheduled task %s (%s)", st.getRequest().getRequest().getPath().asString(), st.getRequest().getExternalId()));
            st.abortTask();
            // Update or remove in the archive
            ScheduledActivityData sad = st.getCurrentData();
            storeAndDistribute(sad);
            // Update relative trigger of relative-time scheduled tasks
            reEvaluateRelativeTimeTriggers(st.getRequest().getExternalId());
        }
    }

    private void reEvaluateRelativeTimeTriggers(String externalId) {
        dispatcher.submit(() -> {
            for(ScheduledTask st :id2scheduledTask.values()) {
                if(st.getCurrentData().getState() == SchedulingState.SCHEDULED && st.isRelatedTo(externalId)) {
                    try {
                        st.armTrigger();
                    } catch (SchedulingException e) {
                        LOG.log(Level.SEVERE, "Re-evaluation of trigger for scheduled activity " + st.getId() + " failed: " + e.getMessage(), e);
                    }
                }
            }
        });
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
                if (runningTask.isPresent()) {
                    throw new SchedulingException("Task " + runningTask.get().getId() + " already in state " + runningTask.get().getCurrentData().getState() + ", cannot replace the schedule period");
                }
                // If you are here, there is no scheduled task from the provided source that started, so they can be all removed. Let's check for conflicts.
                List<ScheduledTask> conflictingTasks = computeConflicts(requests);
                // Remove the tasks of this source, they will go away
                conflictingTasks.removeIf(o -> o.getCurrentData().getSource().equals(source));
                // Final check
                if (!conflictingTasks.isEmpty() && conflictStrategy == CreationConflictStrategy.ABORT) {
                    throw new SchedulingException("Conflict detected with provided scheduling requests: " + conflictingTasks);
                }
                // OK, at this stage the import can be handled.
                // Remove the tasks from source
                for (ScheduledTask st : tasks) {
                    if (st.getCurrentData().getSource().equals(source)) {
                        removeTask(st.getId());
                    }
                }
                // Add new tasks
                for (SchedulingRequest sr : requests) {
                    ScheduledActivityData data = scheduleTask(sr, conflictStrategy, null);
                    if (data != null) {
                        toReturn.add(data);
                    }
                }
                return toReturn;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SchedulingException(e);
        }
    }

    @Override
    public void enableBot(String name) throws SchedulingException {
        final BotProcessingDefinition fToSet = findBotDefinition(name);
        dispatcher.submit(() -> setBotEnablement(fToSet, true));
    }

    @Override
    public void disableBot(String name) throws SchedulingException {
        final BotProcessingDefinition fToSet = findBotDefinition(name);
        dispatcher.submit(() -> setBotEnablement(fToSet, false));
    }

    private BotProcessingDefinition findBotDefinition(String name) throws SchedulingException {
        if (this.configuration == null) {
            throw new SchedulingException("Cannot enable/disable bot " + name + ": no bots defined");
        }
        BotProcessingDefinition toSet = null;
        for (BotProcessingDefinition bps : this.configuration.getBots()) {
            if (bps.getName().equals(name)) {
                toSet = bps;
                break;
            }
        }
        if (toSet == null) {
            throw new SchedulingException("Cannot enable/disable bot " + name + ": bot not found");
        }
        return toSet;
    }

    /**
     * To be called by the dispatcher thread.
     *
     * @param bot the bot
     * @param status the new enablement status
     */
    private void setBotEnablement(BotProcessingDefinition bot, boolean status) {
        BotStateData newState = bot.updateEnablement(status);
        if(newState != null) {
            notifyBotState(Collections.singletonList(newState));
        }
    }

    private void notifyBotState(List<BotStateData> states) {
        for (ISchedulerSubscriber subscriber : schedulerSubscribers) {
            notifier.submit(() -> {
                try {
                    if (!states.isEmpty()) {
                        subscriber.botStateUpdated(states);
                    }
                } catch (RemoteException e) {
                    LOG.log(Level.SEVERE, "Subscriber remote exception, dropping it");
                    unsubscribe(subscriber);
                }
            });
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
            List<ScheduledActivityData> data = internalGetCurrentScheduledActivities().stream().filter(o -> filter == null || filter.isClear() || filter.test(o)).collect(Collectors.toList());
            try {
                subscriber.dataItemsReceived(data);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Remote exception when notifying scheduled data subscriber, dropping it", e);
                unsubscribe(subscriber);
            }
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

    @Override
    public List<ScheduledActivityData> retrieve(Instant startTime, Instant endTime, ScheduledActivityDataFilter filter) throws ReatmetricException, RemoteException {
        if (archive == null) {
            throw new ReatmetricException("Archive not available");
        }
        return archive.retrieve(startTime, endTime, filter);
    }

    IUniqueId getNextId() {
        return new LongUniqueId(sequencer.incrementAndGet());
    }

    /**
     * To be called from the dispatcher thread.
     */
    void updateEventFilter(SystemEntityPath newEvent, boolean remove) {
        if (remove) {
            subscribedEvents.remove(newEvent);
        } else {
            subscribedEvents.add(newEvent);
        }
        if (subscribedEvents.isEmpty()) {
            currentEventFilter = null;
            try {
                eventService.unsubscribe(eventSubscriber);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Remote exception on event service unsubscribe", e);
            }
        } else {
            currentEventFilter = new EventDataFilter(null, subscribedEvents, null, null, null, null, null);
            try {
                eventService.subscribe(eventSubscriber, currentEventFilter);
            } catch (RemoteException e) {
                LOG.log(Level.SEVERE, "Remote exception on event service subscription", e);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    boolean areAllCompleted(Set<String> predecessors) {
        for (String id : predecessors) {
            ScheduledTask st = lookUpScheduledTaskByExternalId(id);
            if (st != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * To be called from the dispatcher thread.
     */
    boolean registerResources(Set<String> resources) {
        if (Collections.disjoint(this.currentlyUsedResources, resources)) {
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
        try {
            return executionService.startActivity(request);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void releaseResources(Set<String> resources) {
        this.currentlyUsedResources.removeAll(resources);
        // Check waiting tasks
        for (ScheduledTask st : id2scheduledTask.values()) {
            if (st.getCurrentData().getState() == SchedulingState.WAITING) {
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
        for (EventData ed : dataItems) {
            for (ScheduledTask st : id2scheduledTask.values()) {
                st.newEventOccurrence(ed);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void activityUpdate(List<ActivityOccurrenceData> dataItems) {
        for (ActivityOccurrenceData aod : dataItems) {
            ScheduledTask st = activityId2scheduledTask.get(aod.getInternalId());
            if (st != null) {
                st.activityStatusUpdate(aod);
            }
        }
    }

    /**
     * To be called from the dispatcher thread.
     */
    void parameterUpdate(List<ParameterData> dataItems) {
        if(!dataItems.isEmpty()) {
            List<SchedulingRequest> requests = new LinkedList<>();
            for(ParameterData pd : dataItems) {
                cachedParameterMap.put(pd.getPath().asString(), pd);
            }
            List<BotStateData> updatedStates = new LinkedList<>();
            for(BotProcessingDefinition bpd : configuration.getBots()) {
                if(bpd.isAffectedBy(dataItems)) {
                    requests.addAll(bpd.evaluate(this));
                    updatedStates.add(bpd.getCurrentState());
                }
            }
            if(!requests.isEmpty()) {
                internalScheduleRequest(requests, CreationConflictStrategy.ADD_ANYWAY);
            }
            notifyBotState(updatedStates);
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
    void abortActivity(int activityId, IUniqueId activityOccurrenceId) {
        try {
            this.executionService.abortActivity(activityId, activityOccurrenceId);
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot abort activity occurrence " + activityOccurrenceId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ParameterData getParameterData(String path) {
        return cachedParameterMap.get(path);
    }

    @Override
    public ActivityDescriptor resolveDescriptor(String path) {
        try {
            return activityService.getDescriptor(SystemEntityPath.fromString(path));
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.WARNING, "Cannot retrieve activity descriptor for path " + path + ": " + e.getMessage(), e);
            return null;
        }
    }
}
