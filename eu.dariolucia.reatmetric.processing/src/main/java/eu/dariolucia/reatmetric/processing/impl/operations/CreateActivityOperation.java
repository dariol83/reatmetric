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
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;

import java.time.Instant;
import java.util.List;

public class CreateActivityOperation extends AbstractModelOperation<ActivityProcessor> {

    private final ActivityRequest input;
    private final ActivityProgress state;

    public CreateActivityOperation(ActivityRequest input, ActivityProgress state) {
        this.input = input;
        this.state = state;
    }

    @Override
    public Instant getTime() {
        return this.state.getGenerationTime();
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().create(input, state);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }
}
