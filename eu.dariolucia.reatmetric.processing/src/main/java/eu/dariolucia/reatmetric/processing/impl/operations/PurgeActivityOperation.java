/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;
import eu.dariolucia.reatmetric.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;

import java.time.Instant;
import java.util.List;

public class PurgeActivityOperation extends AbstractModelOperation<ActivityProcessor> {

    private final Pair<Integer, IUniqueId> input;
    private final Instant time;

    public PurgeActivityOperation(Pair<Integer, IUniqueId> input) {
        this.input = input;
        this.time = Instant.now();
    }

    @Override
    public Instant getTime() {
        return this.time;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().purge(input.getSecond());
    }

    @Override
    public int getSystemEntityId() {
        return input.getFirst();
    }
}
