/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;

import java.time.Instant;
import java.util.List;

public class ReportActivityProgressOperation extends AbstractModelOperation<ActivityProcessor> {

    private final ActivityProgress input;

    public ReportActivityProgressOperation(ActivityProgress input) {
        this.input = input;
    }

    @Override
    public Instant getTime() {
        return this.input.getGenerationTime();
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().process(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getActivityId();
    }
}
