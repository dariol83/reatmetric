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
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.*;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.processing.definition.ActivityProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.EventProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
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

    private static final int REPORTING_TASK_CAPACITY = 1000;
    private static final int COMMAND_TASK_CAPACITY = 10000;

    public static final String PROCESSING_MODEL_NAME = "Processing Model";
    public static final String DEBUG_INPUT_QUEUE_SIZE = "Input queue size";
    public static final String DEBUG_OUTPUT_DATA_ITEMS = "Output data items";
    public static final String DEBUG_OUTPUT_DATA_ITEMS_UNIT = "data items/second";
    public static final String DEBUG_INPUT_DATA_ITEMS = "Input data items";
    public static final String DEBUG_INPUT_DATA_ITEMS_UNIT = "data items/second";

    public static final int COMMAND_DISPATCHING_QUEUE = 0;
    public static final int REPORTING_DISPATCHING_QUEUE = 1;

    private final ProcessingDefinition processingDefinition;

    private final IProcessingModelOutput output;

    private final Map<Class<? extends AbstractDataItem>, AtomicLong> updateSequencerMap = new ConcurrentHashMap<>();

    private final GraphModel graphModel;

    private final BlockingQueue<ProcessingTask> reportingUpdateTaskQueue = new ArrayBlockingQueue<>(REPORTING_TASK_CAPACITY);
    private final ArrayBlockingQueue<ProcessingTask> activityUpdateTaskQueue = new ArrayBlockingQueue<>(COMMAND_TASK_CAPACITY * 2);

    private final ExecutorService taskProcessors = ThreadUtil.newThreadExecutor(Runtime.getRuntime().availableProcessors(), "Reatmetric Processing - Task Processor");

    private final ExecutorService tmDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - TM Dispatcher");
    private final ExecutorService activityDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Dispatcher");

    private final ExecutorService notifier = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Notifier");

    private final ExecutorService activityOccurrenceDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Occurrence Dispatcher");

    private final Timer operationScheduler = new Timer("Reatmetric Processing - Scheduler", true);

    private final WorkingSet workingSet = new WorkingSet();

    private final List<IActivityHandler> activityHandlersList = new CopyOnWriteArrayList<>();
    private final Map<String, IActivityHandler> route2activityHandler = new ConcurrentHashMap<>();

    private final Consumer<List<AbstractDataItem>> outputRedirector;

    private final IProcessingModelInitialiser initialiser;

    private final Timer performanceSampler = new Timer("Reatmetric Processing - Sampler", true);
    private final AtomicReference<List<DebugInformation>> lastStats = new AtomicReference<>(Arrays.asList(
            DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_QUEUE_SIZE, 0, REPORTING_TASK_CAPACITY, ""),
            DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_DATA_ITEMS, 0, null, DEBUG_INPUT_DATA_ITEMS_UNIT),
            DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_OUTPUT_DATA_ITEMS, 0, null, DEBUG_OUTPUT_DATA_ITEMS_UNIT)
    ));
    private Instant lastSampleGenerationTime;
    private volatile long dataItemOutput = 0;
    private final AtomicLong dataItemInput = new AtomicLong(0);

    /**
     * The set of running activity processors, i.e. those processors that have at least one activity occurrence currently
     * loaded.
     */
    private final Set<ActivityProcessor> activeActivityProcessors = new HashSet<>();

    /**
     * Set of parameter IDs that are reported as currently dirty.
     */
    private final Set<Integer> dirtyParametersSet = new HashSet<>();

    public ProcessingModelImpl(ProcessingDefinition processingDefinition, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialSequencerMap, IProcessingModelInitialiser initialiser) throws ProcessingModelException {
        this.processingDefinition = processingDefinition;
        // Start off the preloader thread: one off, using internal executor for parallel preloading
        new Thread(this::preloadDefinitions, "Reatmetric Processing - Preloader").start();
        //
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
        tmDispatcher.submit(() -> doDispatch(tmDispatcher, reportingUpdateTaskQueue));
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

    private void preloadDefinitions() {
        LOG.log(Level.INFO, "Processing definition preloading started");
        ExecutorService service = Executors.newFixedThreadPool(4);
        for(ParameterProcessingDefinition d : processingDefinition.getParameterDefinitions()) {
            service.submit(() -> {
                try {
                    d.preload();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Preloading of " + d.getLocation() + " (" + d.getId() + " failed: " + e.getMessage(), e);
                }
            });
        }
        for(EventProcessingDefinition d : processingDefinition.getEventDefinitions()) {
            service.submit(() -> {
                try {
                    d.preload();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Preloading of " + d.getLocation() + " (" + d.getId() + " failed: " + e.getMessage(), e);
                }
            });
        }
        for(ActivityProcessingDefinition d : processingDefinition.getActivityDefinitions()) {
            service.submit(() -> {
                try {
                    d.preload();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Preloading of " + d.getLocation() + " (" + d.getId() + " failed: " + e.getMessage(), e);
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing here
        }
        LOG.log(Level.INFO, "Processing definition preloading completed");
    }

    private void sample() {
        synchronized (performanceSampler) {
            Instant genTime = Instant.now();
            if (lastSampleGenerationTime == null) {
                lastSampleGenerationTime = genTime;
                dataItemOutput = 0;
                dataItemInput.set(0);
            } else {
                long numItemsOutput = dataItemOutput;
                dataItemOutput = 0;
                long numItemsInput = dataItemInput.getAndSet(0);
                int millis = (int) (genTime.toEpochMilli() - lastSampleGenerationTime.toEpochMilli());
                lastSampleGenerationTime = genTime;
                double outputItemsPerSecond = (numItemsOutput / (millis/1000.0));
                double inputItemsPerSecond = (numItemsInput / (millis/1000.0));
                List<DebugInformation> toSet = Arrays.asList(
                        DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_QUEUE_SIZE, reportingUpdateTaskQueue.size(), REPORTING_TASK_CAPACITY, ""),
                        DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_INPUT_DATA_ITEMS, (int) inputItemsPerSecond, null, DEBUG_INPUT_DATA_ITEMS_UNIT),
                        DebugInformation.of(PROCESSING_MODEL_NAME, DEBUG_OUTPUT_DATA_ITEMS, (int) outputItemsPerSecond, null, DEBUG_OUTPUT_DATA_ITEMS_UNIT)
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
                // If the queue is the command queue, notify for potential waiters
                if (queue == this.activityUpdateTaskQueue) {
                    synchronized (this.activityUpdateTaskQueue) {
                        this.activityUpdateTaskQueue.notifyAll();
                    }
                }
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
        return scheduleTask(operations, dispatchingQueue, false, false);
    }

    public ProcessingTask scheduleTask(List<AbstractModelOperation<?>> operations, int dispatchingQueue, boolean internalRequest, boolean includeWeaklyConsistent) {
        // Create the processing task
        ProcessingTask taskToRun = new ProcessingTask(new ProcessingTask.Job(operations, outputRedirector, workingSet), includeWeaklyConsistent);
        // Add the task to be done to the queue
        switch(dispatchingQueue) {
            case COMMAND_DISPATCHING_QUEUE:
                // Blocking queue -> this blocking call can introduce a potential deadlock if the queue becomes full, the call is stuck and the
                // call comes from the Processing Model internal classes. This is the reason why the internalRequest flag must be checked:
                // if such flag is set to true, then the request must be honoured.
                // The approach below reserves half of the queue size for mixed operations (internal/external) and the other half for
                // internal operations only. Yes, it can still block, but only for internal operations (timers and propagations),
                // which naturally decrease over time if no further activity operations can be requested, which is the case until
                // the queue utilisation is > 50%. Total queue size is COMMAND_TASK_CAPACITY x 2.
                synchronized (activityUpdateTaskQueue) {
                    try {
                        // Non-internal request might wait a bit
                        while (!internalRequest && activityUpdateTaskQueue.remainingCapacity() < COMMAND_TASK_CAPACITY) {
                            activityUpdateTaskQueue.wait();
                        }
                        // If I reach this point, I have some breathing space, so I should be able to add without risking to be blocked.
                        // The sync block in fact prevents that to happen :)
                        activityUpdateTaskQueue.put(taskToRun);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        LOG.log(Level.FINE, "Thread interrupted while inserting element inside the activity update queue", e);
                    }
                }
                break;
            default:
                dataItemInput.addAndGet(operations.size());
                // Blocking queue -> introduces backpressure on the reporting chain
                try {
                    reportingUpdateTaskQueue.put(taskToRun);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    LOG.log(Level.FINE, "Thread interrupted while inserting element inside the reporting update queue", e);
                }
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
        reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, ActivityOccurrenceReport.FORWARDING_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null));
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
                reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, ActivityOccurrenceReport.FORWARDING_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE, null));
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
        // Check if the route is not null
        if(route == null) {
            throw new ProcessingModelException("No route defined in the request");
        }
        // Check if the route exist
        IActivityHandler handler = route2activityHandler.get(route);
        if(handler == null) {
            // Maybe it is a dynamic route, let's update the map
            refreshActivityRouteMap();
            // Try again
            handler = route2activityHandler.get(route);
        }
        if(handler == null) {
            throw new ProcessingModelException("Cannot find activity handler for route " + route);
        }
        if(!handler.getSupportedActivityTypes().contains(type)) {
            throw new ProcessingModelException("The selected activity handler does not support processing of activity type " + type);
        }
        return handler;
    }

    private void refreshActivityRouteMap() {
        for(IActivityHandler h : this.activityHandlersList) {
            List<String> routes = h.getSupportedRoutes();
            for(String route : routes) {
                // Let's update the map, in case of dynamically added routes
                if(!this.route2activityHandler.containsKey(route)) {
                    this.route2activityHandler.put(route, h);
                }
            }
        }
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

    public void newDirtyParameter(int systemEntityId) {
        boolean wasEmpty;
        synchronized (this.dirtyParametersSet) {
            wasEmpty = this.dirtyParametersSet.isEmpty();
            this.dirtyParametersSet.add(systemEntityId);
        }
        if(wasEmpty) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    refreshDirtyParameters();
                }
            };
            // Schedule task to run in 1 second (hardcoded)
            scheduleAt(Instant.now().plusSeconds(1), task);
        }
    }

    private void refreshDirtyParameters() {
        Set<Integer> parametersToRefresh;
        synchronized (this.dirtyParametersSet) {
            parametersToRefresh = new HashSet<>(this.dirtyParametersSet);
            this.dirtyParametersSet.clear();
        }
        if(LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "Refreshing dirty parameters " + parametersToRefresh);
        }
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = parametersToRefresh.stream().map(WeaklyConsistentRefreshOperation::new).collect(Collectors.toList());
        // Schedule task
        scheduleTask(operations, REPORTING_DISPATCHING_QUEUE, false, true);
    }

    @Override
    public void injectParameters(List<ParameterSample> sampleList) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = sampleList.stream().map(ParameterSampleProcessOperation::new).collect(Collectors.toList());
        // Schedule task
        scheduleTask(operations, REPORTING_DISPATCHING_QUEUE);
    }

    @Override
    public void raiseEvent(EventOccurrence event) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new RaiseEventOperation(event));
        // Schedule task
        scheduleTask(operations, REPORTING_DISPATCHING_QUEUE);
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
        scheduleTask(operations, REPORTING_DISPATCHING_QUEUE);
    }

    @Override
    public void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = activityOccurrenceIds.stream().map(PurgeActivityOperation::new).collect(Collectors.toList());
        // Schedule task
        scheduleTask(operations, COMMAND_DISPATCHING_QUEUE);
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) {
        if(activityOccurrenceId == null) {
            throw new IllegalArgumentException("Null activity occurrence ID");
        }
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new AbortActivityOperation(activityId, activityOccurrenceId));
        // Schedule task
        scheduleTask(operations, COMMAND_DISPATCHING_QUEUE);
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
        AbstractSystemEntityProcessor<?,?,?> processor = getProcessor(request.getId());
        if(processor == null) {
            throw new ProcessingModelException(String.format("Set request for parameter %d: parameter does not exist", request.getId()));
        }
        if(!(processor instanceof ParameterProcessor)) {
            throw new ProcessingModelException(String.format("Set request for parameter %d returned a different type of system entity: %s", request.getId(), processor.getEntityState().getType()));
        }
        // Start the request
        AbstractInputDataItem activityRequest = ((ParameterProcessor)processor).generateSetRequest(request);
        if(activityRequest instanceof ActivityRequest) {
            return startActivity((ActivityRequest) activityRequest);
        } else if(activityRequest instanceof ParameterSample) {
            injectParameters(Collections.singletonList((ParameterSample) activityRequest));
            return null;
        } else {
            throw new ProcessingModelException(String.format("Set request for parameter %d returned a different type of input: %s", request.getId(), activityRequest));
        }
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
        ProcessingTask pt = scheduleTask(operations, COMMAND_DISPATCHING_QUEUE);

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
        this.activityHandlersList.add(handler);
        for(String route : handler.getSupportedRoutes()) {
            if(this.route2activityHandler.containsKey(route)) {
                throw new ProcessingModelException("Duplicated route: " + route + " is supported by handler " + handler + " but also by handler " + this.route2activityHandler.get(route));
            }
            this.route2activityHandler.put(route, handler);
        }
        handler.registerModel(this);
    }

    @Override
    public void deregisterActivityHandler(IActivityHandler handler) {
        handler.deregisterModel(this);
        for(String route : handler.getSupportedRoutes()) {
            this.route2activityHandler.remove(route);
        }
        this.activityHandlersList.remove(handler);
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability() {
        return getRouteAvailability(null);
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability(String type) {
        List<ActivityRouteState> states = new LinkedList<>();
        for(IActivityHandler h : this.activityHandlersList) {
            if(type != null && !h.getSupportedActivityTypes().contains(type)) {
                continue;
            }
            List<String> routes = h.getSupportedRoutes();
            for(String route : routes) {
                // Let's update the map, in case of dynamically added routes
                if(!this.route2activityHandler.containsKey(route)) {
                    this.route2activityHandler.put(route, h);
                }
                boolean available;
                try {
                    available = h.getRouteAvailability(route);
                    states.add(new ActivityRouteState(route, available ? ActivityRouteAvailability.AVAILABLE : ActivityRouteAvailability.UNAVAILABLE));
                } catch (ActivityHandlingException e) {
                    if(LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, String.format("Retrieving status of route %s on handler %s raised an error: %s", route, h, e.getMessage()), e);
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
        scheduleTask(operations, COMMAND_DISPATCHING_QUEUE);
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

    @Override
    public void mirror(List<AbstractDataItem> items) {
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = new LinkedList<>();
        for(AbstractDataItem adi : items) {
            if(adi instanceof ParameterData) {
                operations.add(new ParameterMirrorOperation((ParameterData) adi));
            } else if(adi instanceof EventData) {
                operations.add(new EventMirrorOperation((EventData) adi));
            } else if(adi instanceof ActivityOccurrenceData) {
                operations.add(new ActivityMirrorOperation((ActivityOccurrenceData) adi));
            }
        }
        // Schedule task
        scheduleTask(operations, REPORTING_DISPATCHING_QUEUE);
    }

    /*
     * IBindingResolver implementation
     */

    @Override
    public IEntityBinding resolve(int systemEntityId) {
        return graphModel.getBinding(systemEntityId);
    }

}
