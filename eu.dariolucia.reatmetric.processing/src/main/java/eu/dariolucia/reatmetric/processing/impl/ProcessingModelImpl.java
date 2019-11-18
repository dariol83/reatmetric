/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.ISystemModelSubscriber;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.processing.IProcessingModel;
import eu.dariolucia.reatmetric.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.graph.GraphModel;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class ProcessingModelImpl implements IParameterResolver, IProcessingModel {

    private static final Logger LOG = Logger.getLogger(ProcessingModelImpl.class.getName());

    public static final String SOURCE_ID = "Processor Model";

    private final List<ISystemModelSubscriber> listeners = new CopyOnWriteArrayList<>();

    private final Map<String, ParameterProcessor> parameterDataProcessorMap = new HashMap<>();

    private final ProcessingDefinition processingDefinition;

    private final IProcessingModelOutput output;

    private final Map<Class<? extends AbstractDataItem>, AtomicLong> updateSequencerMap = new ConcurrentHashMap<>();

    private final GraphModel graphModel;

    public ProcessingModelImpl(ProcessingDefinition processingDefinition, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialSequencerMap) {
        this.processingDefinition = processingDefinition;
        this.output = output;
        // Initialise the sequencer
        if(initialSequencerMap != null) {
            for(Map.Entry<Class<? extends AbstractDataItem>, Long> entry : initialSequencerMap.entrySet()) {
                updateSequencerMap.put(entry.getKey(), new AtomicLong(entry.getValue()));
            }
        }
        // Build the graph model and compute the topological sort
        graphModel = new GraphModel(processingDefinition);
        graphModel.build();
    }

    public long getNextId(Class<? extends AbstractDataItem> type) {
        return updateSequencerMap.computeIfAbsent(type, o -> new AtomicLong(0)).getAndIncrement();
    }

    @Override
    public void injectParameters(List<ParameterSample> sampleList) {
        // Create the partitioning of processing task: each processing task has the list of all the operations to be
        // executed at the given time.
        // The partitioning is performed by a custom strategy (not defined yet, TODO). Do we really need it?
        List<ProcessingTask> t = graphModel.computeOperationList(sampleList); // TODO: Or something else!
        processingTaskQueue.addAll(t);
    }

    @Override
    public void raiseEvent(EventOccurrence event, List<ParameterSample> attachedParameters) {
        // TODO
    }

    @Override
    public IUniqueId startAction() {
        // TODO
        return null;
    }

    @Override
    public void enable(SystemEntityPath path, boolean recursive) {
        // TODO
    }

    @Override
    public void disable(SystemEntityPath path, boolean recursive) {
        // TODO
    }

    @Override
    public ProcessingDefinition getProcessingDefinition() {
        return this.processingDefinition;
    }

    /*
     * ISystemModelProvisionService implementation
     */

    @Override
    public void subscribe(ISystemModelSubscriber subscriber) {

    }

    @Override
    public void unsubscribe(ISystemModelSubscriber subscriber) {

    }

    @Override
    public SystemEntity getRoot() {
        return null;
    }

    @Override
    public List<SystemEntity> getContainedEntities(SystemEntityPath se) {
        return null;
    }

    @Override
    public SystemEntity getSystemEntityAt(SystemEntityPath path) {
        return null;
    }

    @Override
    public SystemEntity getSystemEntityOf(int externalId) {
        return null;
    }

    @Override
    public int getExternalIdOf(SystemEntityPath path) {
        return 0;
    }

    @Override
    public SystemEntityPath getPathOf(int externalId) {
        return null;
    }

    /*
     * IParameterResolver implementation
     */

    @Override
    public ParameterData resolve(int parameterExternalId) {
        return null;
    }
}
