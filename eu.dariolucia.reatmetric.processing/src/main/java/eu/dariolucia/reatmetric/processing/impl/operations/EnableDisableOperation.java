/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.time.Instant;
import java.util.List;

public class EnableDisableOperation extends AbstractModelOperation<AbstractSystemEntityProcessor> {

    private final Instant creationTime = Instant.now();

    private final int id;
    private final boolean enable;

    public EnableDisableOperation(int id, boolean enable) {
        this.enable = enable;
        this.id = id;
    }

    @Override
    public Instant getTime() {
        return creationTime;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return enable ? getProcessor().enable() : getProcessor().disable();
    }

    @Override
    public int getSystemEntityId() {
        return id;
    }

    @Override
    public String toString() {
        return (enable ? "'Enable " : "'Disable ") + id + "'";
    }
}
