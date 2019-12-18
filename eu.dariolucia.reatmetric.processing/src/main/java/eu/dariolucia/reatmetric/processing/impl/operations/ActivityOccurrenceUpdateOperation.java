package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;

import java.time.Instant;
import java.util.List;

public class ActivityOccurrenceUpdateOperation extends AbstractModelOperation<ActivityProcessor> {

    private final Instant creationTime = Instant.now();
    private final IUniqueId occurrenceId;

    public ActivityOccurrenceUpdateOperation(IUniqueId occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    @Override
    public Instant getTime() {
        return creationTime;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().evaluate(occurrenceId);
    }

    @Override
    public int getSystemEntityId() {
        return getProcessor().getSystemEntityId();
    }
}
