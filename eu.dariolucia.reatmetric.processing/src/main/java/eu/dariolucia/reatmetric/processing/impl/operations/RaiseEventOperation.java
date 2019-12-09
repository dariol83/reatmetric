/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.EventProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.time.Instant;
import java.util.List;

public class RaiseEventOperation extends AbstractModelOperation<EventProcessor> {

    private final EventOccurrence input;

    public RaiseEventOperation(EventOccurrence input) {
        this.input = input;
    }

    @Override
    public Instant getTime() {
        return input.getGenerationTime();
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().process(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }
}
