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

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.*;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.operations.*;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.visitors.DataCollectorVisitor;
import eu.dariolucia.reatmetric.processing.util.ThreadUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcessingModelImpl implements IBindingResolver, IProcessingModel {

    private static final Logger LOG = Logger.getLogger(ProcessingModelImpl.class.getName());

    private static final int UPDATE_TASK_CAPACITY = 1000;

    public static final String PROCESSING_MODEL_NAME = "Processing Model";
    public static final String DEBUG_INPUT_QUEUE_SIZE = "Input queue size";
    public static final String DEBUG_OUTPUT_DATA_ITEMS = "Output data items";
    public static final String DEBUG_OUTPUT_DATA_ITEMS_UNIT = "data items/second";

    public static final String FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME = "Forwarding to Activity Handler";

    public static final int USER_DISPATCHING_QUEUE = 0;
    public static final int TM_DISPATCHING_QUEUE = 1;

    private final ProcessingDefinition processingDefinition;

    private final IProcessingModelOutput output;

    private final Map<Class<? extends AbstractDataItem>, AtomicLong> updateSequencerMap = new ConcurrentHashMap<>();

    private final GraphModel graphModel;

    private final BlockingQueue<ProcessingTask> tmUpdateTaskQueue = new ArrayBlockingQueue<>(UPDATE_TASK_CAPACITY);
    private final BlockingQueue<ProcessingTask> activityUpdateTaskQueue = new ArrayBlockingQueue<>(UPDATE_TASK_CAPACITY);

    private final ExecutorService taskProcessors = ThreadUtil.newThreadExecutor(Runtime.getRuntime().availableProcessors(), "Reatmetric Processing - Task Processor");

    private final ExecutorService tmDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - TM Dispatcher");
    private final ExecutorService activityDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Dispatcher");

    private final ExecutorService notifier = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Notifier");

    private final ExecutorService activityOccurrenceDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Occurrence Dispatcher");

    private final Timer operationScheduler = new Timer("Reatmetric Processing - Scheduler", true);

    private final WorkingSet workingSet = new WorkingSet();

    private final Map<String, IActivityHandler> activityHandlers = new ConcurrentHashMap<>();

    private final Consumer<List<AbstractDataItem>> outputRedirector;

    private final IProcessingModelInitialiser initialiser;

    private final Timer performanceSampler = new Timer("Reatmetric Processing - Sampler", true);
    private final AtomicReference<List<DebugInformation>> lastStats = new AtomicReference<>(Arrays.asList(
            DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_QUEUE_SIZE, 0, UPDATE_TASK_CAPACITY, ""),
            DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_OUTPUT_DATA_ITEMS, 0, null, DEBUG_OUTPUT_DATA_ITEMS_UNIT)
    ));
    private Instant lastSampleGenerationTime;
    private long dataItemOutput = 0;

    /**
     * The set of running activity processors, i.e. those processors that have at least one activity occurrence currently
     * loaded.
     */
    private final Set<ActivityProcessor> activeActivityProcessors = new HashSet<>();

    public ProcessingModelImpl(ProcessingDefinition processingDefinition, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialSequencerMap, IProcessingModelInitialiser initialiser) throws ProcessingModelException {
        this.processingDefinition = processingDefinition;
        this.output = output;
        this.initialiser = initialiser;
        // Initialise the sequencer
        if(initialSequencerMap != null) {
            for(Map.Entry<Class<? extends AbstractDataItem>, Long> entry : initialSequencerMap.entrySet()) {
                updateSequencerMap.put(entry.getKey(), new AtomicLong(entry.getValue()));
            }
        }
        // Build the graph model and compute the topological sort
        graphModel = new GraphModel(processingDefinition, this);
        graphModel.build();
        // Activate the dispatchers
        tmDispatcher.submit(() -> doDispatch(tmDispatcher, tmUpdateTaskQueue));
        activityDispatcher.submit(() -> doDispatch(activityDispatcher, activityUpdateTaskQueue));
        // Create redirector that uses the asynchronous notifier
        outputRedirector = createOutputRedirector();
        // Create performance samples
        performanceSampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
    }

    private void sample() {
        synchronized (performanceSampler) {
            Instant genTime = Instant.now();
            if (lastSampleGenerationTime == null) {
                lastSampleGenerationTime = genTime;
                dataItemOutput = 0;
            } else {
                long numItems = dataItemOutput;
                dataItemOutput = 0;
                int millis = (int) (genTime.toEpochMilli() - lastSampleGenerationTime.toEpochMilli());
                lastSampleGenerationTime = genTime;
                double itemsPerSecond = (numItems / (millis/1000.0));
                List<DebugInformation> toSet = Arrays.asList(
                        DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_QUEUE_SIZE, tmUpdateTaskQueue.size(), UPDATE_TASK_CAPACITY, ""),
                        DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_OUTPUT_DATA_ITEMS, (int) itemsPerSecond, null, DEBUG_OUTPUT_DATA_ITEMS_UNIT)
                );
                lastStats.set(toSet);
            }
        }
    }

    public IProcessingModelInitialiser getInitialiser() {
        return initialiser;
    }

    private Consumer<List<AbstractDataItem>> createOutputRedirector() {
        return items -> notifier.submit(() -> {
            synchronized (performanceSampler) {
                dataItemOutput += items.size();
            }
            output.notifyUpdate(items);
        });
    }

    private void doDispatch(ExecutorService executor, BlockingQueue<ProcessingTask> queue) {
        while(!executor.isShutdown()) {
            try {
                // Get the task
                ProcessingTask toProcess = queue.take();
                // Prepare the task
                toProcess.prepareTask(graphModel);
                // Check if the working set allows the processing of the items (blocking call)
                this.workingSet.add(toProcess.getAffectedItems());
                // Ready to be processed, submit the task
                this.taskProcessors.submit(toProcess);
            } catch(Exception e) {
                LOG.log(Level.SEVERE, "Exception when dispatching processing tasks: " + e.getMessage(), e);
            }
        }
    }

    public AbstractSystemEntityProcessor getProcessor(int processorId) {
        return graphModel.getProcessor(processorId);
    }

    public long getNextId(Class<? extends AbstractDataItem> type) {
        return updateSequencerMap.computeIfAbsent(type, o -> new AtomicLong(0)).incrementAndGet();
    }

    public ProcessingTask scheduleTask(List<AbstractModelOperation<?>> operations, int dispatchingQueue) {
        // Create the processing task
        ProcessingTask taskToRun = new ProcessingTask(new ProcessingTask.Job(operations, outputRedirector, workingSet));
        // Add the task to be done to the queue
        switch(dispatchingQueue) {
            case USER_DISPATCHING_QUEUE:
                activityUpdateTaskQueue.offer(taskToRun);
                break;
            default:
                tmUpdateTaskQueue.offer(taskToRun);
                break;
        }
        // Done
        return taskToRun;
    }

    public void scheduleAt(Instant executionDate, TimerTask task) {
        this.operationScheduler.schedule(task, new Date(executionDate.toEpochMilli()));
    }

    public void forwardActivityToHandler(IUniqueId occurrenceId, int activityId, Instant creationTime, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, String route, String source) throws ProcessingModelException {
        // Check if the route exist
        IActivityHandler handler = checkHandlerAvailability(route, type);
        // Assume positive dispatch at this stage
        reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null));
        // All fine, schedule the dispatch
        this.activityOccurrenceDispatcher.execute(() -> {
            try {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Forwarding activity occurrence %s of activity %s to the activity handler on route %s", occurrenceId, path, route));
                }
                handler.executeActivity(new IActivityHandler.ActivityInvocation(occurrenceId, activityId, creationTime, path, type, arguments, properties, route, source));
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Activity occurrence %s forwarded", occurrenceId));
                }
            } catch (ActivityHandlingException e) {
                LOG.log(Level.SEVERE, String.format("Failure forwarding activity occurrence %s of activity %s to the activity handler on route %s: %s", occurrenceId, path, route, e.getMessage()), e);
                // Overwrite status
                reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE, null));
            }
        });
    }

    public void forwardAbortToHandler(IUniqueId occurrenceId, int activityId, String route, String type) throws ProcessingModelException {
        IActivityHandler handler = checkHandlerAvailability(route, type);
        // All fine, schedule the dispatch
        this.activityOccurrenceDispatcher.execute(() -> {
            try {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Forwarding abort of activity occurrence %s to the activity handler on route %s", occurrenceId, route));
                }
                handler.abortActivity(activityId, occurrenceId);
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Abort of activity occurrence %s forwarded", occurrenceId));
                }
            } catch (ActivityHandlingException e) {
                LOG.log(Level.SEVERE, String.format("Failure forwarding abort of activity occurrence %s to the activity handler on route %s: %s", occurrenceId, route, e.getMessage()), e);
            }
        });
    }

    public IActivityHandler checkHandlerAvailability(String route, String type) throws ProcessingModelException {
        // Check if the route exist
        IActivityHandler handler = activityHandlers.get(route);
        if(handler == null) {
            throw new ProcessingModelException("Cannot find activity handler for route " + route);
        }
        if(!handler.getSupportedActivityTypes().contains(type)) {
            throw new ProcessingModelException("The selected activity handler does not support processing of activity type " + type);
        }
        return handler;
    }

    public void registerActiveActivityProcessor(ActivityProcessor ap) {
        synchronized (this.activeActivityProcessors) {
            this.activeActivityProcessors.add(ap);
        }
    }

    public void deregisterInactiveActivityProcessor(ActivityProcessor ap) {
        synchronized (this.activeActivityProcessors) {
            this.activeActivityProcessors.remove(ap);
        }
    }

    public ProcessingDefinition getDefinitions() {
        return this.processingDefinition;
    }

    @Override
    public void injectParameters(List<ParameterSample> sampleList) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = sampleList.stream().map(ParameterSampleProcessOperation::new).collect(Collectors.toList());
        // Schedule task
        scheduleTask(operations, TM_DISPATCHING_QUEUE);
    }

    @Override
    public void raiseEvent(EventOccurrence event) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new RaiseEventOperation(event));
        // Schedule task
        scheduleTask(operations, TM_DISPATCHING_QUEUE);
    }

    @Override
    public IUniqueId startActivity(ActivityRequest request) throws ProcessingModelException {
        if(request == null) {
            throw new IllegalArgumentException("Null request");
        }
        // Build the activity start operation
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new StartActivityOperation(request));
        // Schedule task
        return scheduleActivityOperation(request, operations, "start");
    }

    @Override
    public IUniqueId createActivity(ActivityRequest request, ActivityProgress progress) throws ProcessingModelException {
        if(request == null) {
            throw new IllegalArgumentException("Null request");
        }
        if(progress == null) {
            throw new IllegalArgumentException("Null initial state");
        }
        // Build the activity create operation
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new CreateActivityOperation(request, progress));
        // Schedule task
        return scheduleActivityOperation(request, operations, "creation");
    }

    @Override
    public void reportActivityProgress(ActivityProgress progress) {
        if(progress == null) {
            throw new IllegalArgumentException("Null progress");
        }
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new ReportActivityProgressOperation(progress));
        // Schedule task
        scheduleTask(operations, TM_DISPATCHING_QUEUE);
    }

    @Override
    public void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = activityOccurrenceIds.stream().map(PurgeActivityOperation::new).collect(Collectors.toList());
        // Schedule task
        scheduleTask(operations, USER_DISPATCHING_QUEUE);
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) {
        if(activityOccurrenceId == null) {
            throw new IllegalArgumentException("Null activity occurrence ID");
        }
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new AbortActivityOperation(activityId, activityOccurrenceId));
        // Schedule task
        scheduleTask(operations, USER_DISPATCHING_QUEUE);
    }

    @Override
    public List<ActivityOccurrenceData> getActiveActivityOccurrences() {
        synchronized (this.activeActivityProcessors) {
            return this.activeActivityProcessors.stream()
                    .map(ActivityProcessor::getActiveActivityOccurrences) // Get the List of ActivityOccurrenceData
                    .flatMap(Collection::stream) // Flatten the list
                    .collect(Collectors.toList()); // Collect everything in a single list
        }
    }

    @Override
    public IUniqueId setParameterValue(SetParameterRequest request) throws ProcessingModelException {
        if(request == null) {
            throw new IllegalArgumentException("Null request");
        }
        // Locate the parameter process and ask it to build the activity request
        AbstractSystemEntityProcessor processor = getProcessor(request.getId());
        if(processor == null) {
            throw new ProcessingModelException("Set request for parameter " + request.getId() + ": parameter does not exist");
        }
        if(!(processor instanceof ParameterProcessor)) {
            throw new ProcessingModelException("Set request for parameter " + request.getId() + " returned a different type of system entity: " + processor.getEntityState().getType());
        }
        // Start the request
        ActivityRequest activityRequest = ((ParameterProcessor)processor).generateSetRequest(request);
        return startActivity(activityRequest);
    }

    @Override
    public void visit(IProcessingModelVisitor visitor) {
        graphModel.navigate(visitor);
    }

    @Override
    public List<AbstractDataItem> get(AbstractDataItemFilter<?> filter) {
        DataCollectorVisitor visitor = new DataCollectorVisitor(filter);
        graphModel.navigate(visitor);
        return visitor.getCollectedData();
    }

    @Override
    public List<AbstractDataItem> getByPath(List<SystemEntityPath> paths) throws ProcessingModelException {
        return graphModel.getByPath(paths);
    }

    @Override
    public List<AbstractDataItem> getById(List<Integer> ids) throws ProcessingModelException {
        return graphModel.getById(ids);
    }

    private IUniqueId scheduleActivityOperation(ActivityRequest request, List<AbstractModelOperation<?>> operations, String type) throws ProcessingModelException {
        // Schedule task
        ProcessingTask pt = scheduleTask(operations, USER_DISPATCHING_QUEUE);

        List<AbstractDataItem> executionResult;
        // Wait for the activity creation
        try {
            executionResult = pt.get();
        } catch (InterruptedException e) {
            throw new ProcessingModelException("Creation of activity occurrence for activity " + type + " request " + request.getId() + " interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingModelException) {
                throw (ProcessingModelException) e.getCause();
            } else {
                throw new ProcessingModelException("Creation of activity occurrence for activity " + type + " request " + request.getId() + " failed: " + e.getMessage(), e);
            }
        }
        // Return the unique ID of the activity occurrence
        for (AbstractDataItem adi : executionResult) {
            if (adi instanceof ActivityOccurrenceData) {
                return adi.getInternalId();
            }
        }
        // Error
        throw new ProcessingModelException("Creation of activity occurrence for activity " + type + " request " + request.getId() + " not detected");
    }

    @Override
    public void registerActivityHandler(IActivityHandler handler) throws ProcessingModelException {
        for(String route : handler.getSupportedRoutes()) {
            if(this.activityHandlers.containsKey(route)) {
                throw new ProcessingModelException("Duplicated route: " + route + " is supported by handler " + handler + " but also by handler " + this.activityHandlers.get(route));
            }
            this.activityHandlers.put(route, handler);
        }
        handler.registerModel(this);
    }

    @Override
    public void deregisterActivityHandler(IActivityHandler handler) {
        for(String route : handler.getSupportedRoutes()) {
            this.activityHandlers.remove(route);
        }
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability() {
        return getRouteAvailability(null);
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability(String type) {
        List<ActivityRouteState> states = new LinkedList<>();
        for(IActivityHandler h : this.activityHandlers.values()) {
            if(type != null && !h.getSupportedActivityTypes().contains(type)) {
                continue;
            }
            List<String> routes = h.getSupportedRoutes();
            for(String route : routes) {
                boolean available;
                try {
                    available = h.getRouteAvailability(route);
                    states.add(new ActivityRouteState(route, available ? ActivityRouteAvailability.AVAILABLE : ActivityRouteAvailability.UNAVAILABLE));
                } catch (ActivityHandlingException e) {
                    if(LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, String.format("Retrieving status of route %s on handler %s raised an error: %s", route, h.toString(), e.getMessage()), e);
                    }
                    states.add(new ActivityRouteState(route, ActivityRouteAvailability.UNKNOWN));
                }
            }
        }
        return states;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return this.lastStats.get();
    }

    @Override
    public void enable(SystemEntityPath path) throws ProcessingModelException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info("Enabling system entity " + path);
        }
        doEnable(path, Status.ENABLED);
    }

    @Override
    public void disable(SystemEntityPath path) throws ProcessingModelException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info("Disabling system entity " + path);
        }
        doEnable(path, Status.DISABLED);
    }

    @Override
    public void ignore(SystemEntityPath path) throws ProcessingModelException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info("Ignoring system entity " + path);
        }
        doEnable(path, Status.IGNORED);
    }

    private void doEnable(SystemEntityPath path, Status toBeApplied) throws ProcessingModelException {
        // Map the path to the entity ID
        int id = getExternalIdOf(path);
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new EnableDisableOperation(id, toBeApplied));
        // Schedule task
        scheduleTask(operations, USER_DISPATCHING_QUEUE);
    }

    @Override
    public SystemEntity getRoot() throws ProcessingModelException {
        return graphModel.getRoot();
    }

    @Override
    public List<SystemEntity> getContainedEntities(SystemEntityPath path) throws ProcessingModelException {
        return graphModel.getContainedEntities(getExternalIdOf(path));
    }

    @Override
    public SystemEntity getSystemEntityAt(SystemEntityPath path) throws ProcessingModelException {
        return getSystemEntityOf(graphModel.getIdOf(path));
    }

    @Override
    public SystemEntity getSystemEntityOf(int externalId) throws ProcessingModelException {
        return graphModel.getSystemEntityOf(externalId);
    }

    @Override
    public int getExternalIdOf(SystemEntityPath path) throws ProcessingModelException {
        return graphModel.getIdOf(path);
    }

    @Override
    public SystemEntityPath getPathOf(int externalId) throws ProcessingModelException {
        return graphModel.getPathOf(externalId);
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptorOf(int externalId) throws ProcessingModelException {
        return graphModel.getDescriptorOf(externalId);
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ProcessingModelException {
        return graphModel.getDescriptorOf(path);
    }

    /*
     * IBindingResolver implementation
     */

    @Override
    public IEntityBinding resolve(int systemEntityId) {
        return graphModel.getBinding(systemEntityId);
    }

}
