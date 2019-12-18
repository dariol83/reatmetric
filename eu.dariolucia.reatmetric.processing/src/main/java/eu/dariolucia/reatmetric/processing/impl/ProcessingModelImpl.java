/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.*;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.definition.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.operations.*;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.processing.util.ThreadUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessingModelImpl implements IBindingResolver, IProcessingModel {

    private static final Logger LOG = Logger.getLogger(ProcessingModelImpl.class.getName());

    private static final int UPDATE_TASK_CAPACITY = 1000;
    public static final String FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME = "Forwarding to Activity Handler";

    private final ProcessingDefinition processingDefinition;

    private final IProcessingModelOutput output;

    private final Map<Class<? extends AbstractDataItem>, AtomicLong> updateSequencerMap = new ConcurrentHashMap<>();

    private final GraphModel graphModel;

    private final BlockingQueue<ProcessingTask> updateTaskQueue = new ArrayBlockingQueue<>(UPDATE_TASK_CAPACITY);

    private final ExecutorService taskProcessors = ThreadUtil.newCachedThreadExecutor("Reatmetric Task Processor");

    private final ExecutorService dispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing Dispatcher");

    private final ExecutorService notifier = ThreadUtil.newSingleThreadExecutor("Reatmetric Processing Notifier");

    private final ExecutorService activityOccurrenceDispatcher = ThreadUtil.newSingleThreadExecutor("Reatmetric Activity Occurrence Dispatcher");

    private final Timer operationScheduler = new Timer("Reatmetric Processing Scheduler", true);

    private final WorkingSet workingSet = new WorkingSet();

    private final Map<String, IActivityHandler> activityHandlers = new ConcurrentHashMap<>();

    private final Consumer<List<AbstractDataItem>> outputRedirector;

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
        // Activate the dispatcher
        dispatcher.submit(this::doDispatch);
        // Create redirector that uses the asynchronous notifier
        outputRedirector = createOutputRedirector();
    }

    private Consumer<List<AbstractDataItem>> createOutputRedirector() {
        return items -> notifier.submit(() -> output.notifyUpdate(items));
    }

    private void doDispatch() {
        while(!dispatcher.isShutdown()) {
            try {
                // Get the task
                ProcessingTask toProcess = this.updateTaskQueue.take();
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

    public void scheduleTask(List<AbstractModelOperation> operations) {
        // Create the processing task
        ProcessingTask taskToRun = new ProcessingTask(operations, outputRedirector, workingSet);
        // Add the task to be done to the queue
        updateTaskQueue.add(taskToRun);
        // Done
    }

    public void scheduleAt(Instant executionDate, TimerTask task) {
        this.operationScheduler.schedule(task, new Date(executionDate.toEpochMilli()));
    }


    public void forwardActivityToHandler(IUniqueId occurrenceId, int activityId, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, String route) throws ProcessingModelException {
        // Check if the route exist
        IActivityHandler handler = activityHandlers.get(route);
        if(handler == null) {
            throw new ProcessingModelException("Cannot find activity handler for route " + route);
        }
        if(!handler.getSupportedActivityTypes().contains(type)) {
            throw new ProcessingModelException("The selected activity handler does not support processing of activity type " + type);
        }
        // All fine, schedule the dispatch
        this.activityOccurrenceDispatcher.execute(() -> {
            reportActivityProgress(ActivityProgress.of(occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.PENDING, ActivityOccurrenceState.RELEASE, null));
            try {
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Forwarding activity occurrence %s of activity %s to the activity handler on route %s", occurrenceId, path, route));
                }
                handler.executeActivity(occurrenceId, activityId, path, type, arguments, properties, route);
                if(LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("Activity occurrence %s forwarded", occurrenceId));
                }
                reportActivityProgress(ActivityProgress.of(occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.OK, ActivityOccurrenceState.RELEASE, null));
            } catch (ActivityHandlingException e) {
                LOG.log(Level.SEVERE, String.format("Failure forwarding activity occurrence %s of activity %s to the activity handler on route %s", occurrenceId, path, route), e);
                reportActivityProgress(ActivityProgress.of(occurrenceId, FORWARDING_TO_ACTIVITY_HANDLER_STAGE_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE, null));
            }
        });
    }

    @Override
    public void injectParameters(List<ParameterSample> sampleList) {
        // Build the list of operations to be performed
        List<AbstractModelOperation> operations = new LinkedList<>();
        for(ParameterSample ps : sampleList) {
            ParameterSampleProcessOperation injectOperation = new ParameterSampleProcessOperation(ps);
            operations.add(injectOperation);
        }
        // Schedule task
        scheduleTask(operations);
    }

    @Override
    public void raiseEvent(EventOccurrence event) {
        // Build the list of operations to be performed
        List<AbstractModelOperation> operations = Collections.singletonList(new RaiseEventOperation(event));
        // Schedule task
        scheduleTask(operations);
    }

    @Override
    public IUniqueId startActivity(ActivityRequest request) {
        // TODO: implement support
        throw new UnsupportedOperationException();
    }

    @Override
    public IUniqueId createActivity(ActivityRequest request, ActivityProgress progress) {
        // TODO: implement support
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportActivityProgress(ActivityProgress progress) {
        // TODO: implement support
        throw new UnsupportedOperationException();
    }

    @Override
    public void purgeActivities(List<IUniqueId> activityOccurrenceIds) {
        // TODO: implement support
        throw new UnsupportedOperationException();
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
    public void deregisterActivityHandler(IActivityHandler handler) throws ProcessingModelException {
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
        List<AbstractModelOperation> operations = Collections.singletonList(new EnableDisableOperation(id, b));
        // Schedule task
        scheduleTask(operations);
    }

    @Override
    public ProcessingDefinition getProcessingDefinition() {
        return this.processingDefinition;
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
