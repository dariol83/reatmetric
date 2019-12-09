package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.time.Instant;
import java.util.List;

public class SystemEntityUpdateOperation extends AbstractModelOperation<AbstractSystemEntityProcessor> {

    private final Instant creationTime = Instant.now();

    @Override
    public Instant getTime() {
        return creationTime;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().evaluate();
    }

    @Override
    public int getSystemEntityId() {
        return getProcessor().getSystemEntityId();
    }
}
