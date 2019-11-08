/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.common.FieldDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.core.message.IMessageProcessor;
import eu.dariolucia.reatmetric.core.storage.impl.StorageProcessor;

import java.time.Instant;
import java.util.List;

public class AlarmParameterProcessor implements IAlarmParameterDataProvisionService {

    private volatile IMessageProcessor logger;

    private volatile StorageProcessor storer;


    public void setLogger(IMessageProcessor logger) {
        this.logger = logger;
    }

    public void setStorer(StorageProcessor storer) {
        this.storer = storer;
    }

    public void processAlarms(List<ParameterData> processedData) {
        // TODO
    }

    @Override
    public void subscribe(IAlarmParameterDataSubscriber subscriber, AlarmParameterDataFilter filter) {

    }

    @Override
    public void unsubscribe(IAlarmParameterDataSubscriber subscriber) {

    }

    @Override
    public List<AlarmParameterData> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        return null;
    }

    @Override
    public List<AlarmParameterData> retrieve(AlarmParameterData startItem, int numRecords, RetrievalDirection direction, AlarmParameterDataFilter filter) {
        return null;
    }

    @Override
    public List<FieldDescriptor> getAdditionalFieldDescriptors() {
        return null;
    }
}
