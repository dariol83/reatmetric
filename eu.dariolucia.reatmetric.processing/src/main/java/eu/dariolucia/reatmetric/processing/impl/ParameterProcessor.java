/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.core.message.IMessageProcessor;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.core.storage.impl.StorageProcessor;
import eu.dariolucia.reatmetric.core.util.ThreadUtil;
import eu.dariolucia.reatmetric.processing.util.ThreadUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class ParameterProcessor implements IParameterResolver, IParameterDataProvisionService {

    public static final String SOURCE_ID = "Parameter Processor";

    private final List<ParameterSubscription> listeners = new CopyOnWriteArrayList<ParameterSubscription>();

    private final Map<String, ParameterDataProcessor> parameterDataProcessorMap = new HashMap<>();

    private final ExecutorService dispatcher = ThreadUtil.newSingleThreadExecutor("Parameter Processor Dispatcher");

    private final ProcessingDefinition processingDefinition;

    private volatile IMessageProcessor logger;

    private volatile StorageProcessor storer;

    private final AlarmParameterProcessor alarmParameterProcessor;

    public ParameterProcessor(ProcessingDefinition processingDefinition) {
        this.processingDefinition = processingDefinition;
        this.alarmParameterProcessor = new AlarmParameterProcessor();
    }

    public IAlarmParameterDataProvisionService getAlarmParameterProcessor() {
        return this.alarmParameterProcessor;
    }

    public void setStorer(StorageProcessor storer) {
        this.storer = storer;
        this.alarmParameterProcessor.setStorer(storer);
    }

    public void setLogger(IMessageProcessor logger) {
        this.logger = logger;
        this.alarmParameterProcessor.setLogger(logger);
    }

    public void process(List<ParameterValue> values) {
        dispatcher.submit(() -> doProcess(values));
    }

    private void doProcess(List<ParameterValue> values) {
        List<ParameterData> processedData = new ArrayList<>(values.size());
        for (ParameterValue pv : values) {
            try {
                ParameterDataProcessor pdp = parameterDataProcessorMap.get(pv.getId());
                if (pdp != null) {
                    ParameterData processed = pdp.process(pv);
                    processedData.add(processed);
                } else {
                    logger.raiseMessage("Parameter " + pv.getId() + " not found", SOURCE_ID, Severity.WARN);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.raiseMessage("Parameter " + pv.getId() + " model exception: " + e.getMessage(), SOURCE_ID, Severity.ALARM);
            }
        }
        // Store to database
        store(processedData);
        // Distribute
        distribute(processedData);
        // Alarm processing
        this.alarmParameterProcessor.processAlarms(processedData);
    }

    private void store(List<ParameterData> processedData) {
        this.storer.storeParameters(processedData);
    }

    private void distribute(List<ParameterData> processedData) {
        this.listeners.forEach(o -> o.distribute(processedData));
    }

    @Override
    public ParameterData resolve(String parameterId) {
        return this.parameterDataProcessorMap.get(parameterId).getState();
    }

    @Override
    public void subscribe(IParameterDataSubscriber subscriber, ParameterDataFilter filter) {
        this.listeners.add(new ParameterSubscription(subscriber, filter));
    }

    @Override
    public void unsubscribe(IParameterDataSubscriber subscriber) {
        Optional<ParameterSubscription> toBeRemoved = this.listeners.stream().filter(o -> o.getSubscriber().equals(subscriber)).findFirst();
        toBeRemoved.ifPresent(ParameterSubscription::terminate);
        toBeRemoved.ifPresent(this.listeners::remove);
    }

    @Override
    public List<ParameterData> retrieve(Instant startTime, ParameterDataFilter filter) {
        return this.storer.retrieveParameters(startTime, filter);
    }

    @Override
    public List<ParameterData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        return this.storer.retrieveParameters(startTime, numRecords, direction, filter);
    }

    @Override
    public List<ParameterData> retrieve(ParameterData startItem, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        return this.storer.retrieveParameters(startItem, numRecords, direction, filter);
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return Collections.emptyList();
    }
}
