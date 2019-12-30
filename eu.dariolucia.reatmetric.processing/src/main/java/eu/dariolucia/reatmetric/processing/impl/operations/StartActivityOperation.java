/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.EventProcessor;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;

import java.time.Instant;
import java.util.List;

public class StartActivityOperation extends AbstractModelOperation<ActivityProcessor> {

    private final ActivityRequest input;
    private final Instant time;

    public StartActivityOperation(ActivityRequest input) {
        this.input = input;
        this.time = Instant.now();
        setAbortOnException(true);
    }

    @Override
    public Instant getTime() {
        return this.time;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().invoke(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }

    @Override
    public String toString() {
        return "'Start Activity " + input.getId() + "'";
    }
}
