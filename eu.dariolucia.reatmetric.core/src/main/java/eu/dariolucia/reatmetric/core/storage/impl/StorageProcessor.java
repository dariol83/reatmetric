/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.storage.impl;

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class StorageProcessor {

    public void storeParameters(List<ParameterData> processedData) {
        // TODO
    }

    public List<ParameterData> retrieveParameters(Instant startTime, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public List<ParameterData> retrieveParameters(ParameterData startItem, int numRecords, RetrievalDirection direction, ParameterDataFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public List<ParameterData> retrieveParameters(Instant startTime, ParameterDataFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public void storeRawData(List<RawData> rawData) {
        // TODO
    }

    public List<RawData> retrieveRawData(Instant startTime, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public List<RawData> retrieveRawData(RawData startItem, int numRecords, RetrievalDirection direction, RawDataFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public void storeMessages(List<OperationalMessage> messages) {
        // TODO
    }

    public List<OperationalMessage> retrieveMessages(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        // TODO
        return Collections.emptyList();
    }

    public List<OperationalMessage> retrieveMessages(OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        // TODO
        return Collections.emptyList();
    }
}
