/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.operations.*;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.visitors.DataCollectorVisitor;
import eu.dariolucia.reatmetric.processing.util.ThreadUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcessingModelImpl implements IBindingResolver, IProcessingModel {

    private static final Logger LOG = Logger.getLogger(ProcessingModelImpl.class.getName());

    private static final int UPDATE_TASK_CAPACITY = 1000;

    public static final String FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME = "Forwarding to Activity Handler";

    public static final int USER_DISPATCHING_QUEUE = 0;
    public static final int TM_DISPATCHING_QUEUE = 1;

    private final ProcessingDefinition processingDefinition;

    private final IProcessingModelOutput output;

    private final Map<Class<? extends AbstractDataItem>, AtomicLong> updateSequencerMap = new ConcurrentHashMap<>();

    private final GraphModel graphModel;

    private final BlockingQueue<ProcessingTask> tmUpdateTaskQueue = new ArrayBlockingQueue<>(UPDATE_TASK_CAPACITY);
    private final BlockingQueue<ProcessingTask> activityUpdateTaskQueue = new ArrayBlockingQueue<>(UPDATE_TASK_CAPACITY);

    private final ExecutorService taskProcessors = ThreadUtil.newCachedThreadExecutor("Reatmetric Processing - Task Processor");

    private final ExecutorService tmDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - TM Dispatcher");
    private final ExecutorService activityDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Dispatcher");

    private final ExecutorService notifier = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Notifier");

    private final ExecutorService activityOccurrenceDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing - Activity Occurrence Dispatcher");

    private final Timer operationScheduler = new Timer("Reatmetric Processing - Scheduler", true);

    private final WorkingSet workingSet = new WorkingSet();

    private final Map<String, IActivityHandler> activityHandlers = new ConcurrentHashMap<>();

    private final Consumer<List<AbstractDataItem>> outputRedirector;

    /**
     * The set of running activity processors, i.e. those processors that have at least one activity occurrence currently
     * loaded.
     */
    private final Set<ActivityProcessor> activeActivityProcessors = new HashSet<>();

    public ProcessingModelImpl(ProcessingDefinition processingDefinition, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialSequencerMap) throws ProcessingModelException {
        this.processingDefinition = processingDefinition;
        this.output = output;
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
    }

    private Consumer<List<AbstractDataItem>> createOutputRedirector() {
        return items -> notifier.submit(() -> output.notifyUpdate(items));
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
        return updateSequencerMap.computeIfAbsent(type, o -> new AtomicLong(0)).getAndIncrement();
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
                reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null));
            } catch (ActivityHandlingException e) {
                LOG.log(Level.SEVERE, String.format("Failure forwarding activity occurrence %s of activity %s to the activity handler on route %s", occurrenceId, path, route), e);
                reportActivityProgress(ActivityProgress.of(activityId, occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE, null));
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
        // Build the activity start operation
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new StartActivityOperation(request));
        // Schedule task
        return scheduleActivityOperation(request, operations, "start");
    }

    @Override
    public IUniqueId createActivity(ActivityRequest request, ActivityProgress progress) throws ProcessingModelException {
        // Build the activity create operation
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new CreateActivityOperation(request, progress));
        // Schedule task
        return scheduleActivityOperation(request, operations, "creation");
    }

    @Override
    public void reportActivityProgress(ActivityProgress progress) {
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
    public List<ActivityOccurrenceData> getActiveActivityOccurrences() {
        synchronized (this.activeActivityProcessors) {
            return this.activeActivityProcessors.stream()
                    .map(ActivityProcessor::getActiveActivityOccurrences) // Get the List of ActivityOccurrenceData
                    .flatMap(Collection::stream) // Flatten the list
                    .collect(Collectors.toList()); // Collect everything in a single list
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
    public void enable(SystemEntityPath path) throws ProcessingModelException {
        doEnable(path, true);
    }

    @Override
    public void disable(SystemEntityPath path) throws ProcessingModelException {
        doEnable(path, false);
    }

    private void doEnable(SystemEntityPath path, boolean b) throws ProcessingModelException {
        // Map the path to the entity ID
        int id = getExternalIdOf(path);
        // Build the list of operations to be performed
        List<AbstractModelOperation<?>> operations = Collections.singletonList(new EnableDisableOperation(id, b));
        // Schedule task
        scheduleTask(operations, USER_DISPATCHING_QUEUE);
    }

    @Override
    public SystemEntity getRoot() {
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

    /*
     * IBindingResolver implementation
     */

    @Override
    public IEntityBinding resolve(int systemEntityId) {
        return graphModel.getBinding(systemEntityId);
    }

}
