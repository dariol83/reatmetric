package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.time.Instant;
import java.util.function.Supplier;

public abstract class AbstractModelOperation<T extends AbstractDataItem, K extends AbstractSystemEntityProcessor> {

    private int orderingId;

    protected K processor;

    protected K getProcessor() {
        return processor;
    }

    public void setProcessor(K processor) {
        this.processor = processor;
    }

    public Pair<T, SystemEntity> execute() throws ProcessingModelException {
        // A processor is required: if it is not set, exception
        if(processor == null) {
            throw new ProcessingModelException("Processor not set on operation " + getClass().getSimpleName() + " for entity " + getSystemEntityId());
        }
        return doProcess();
    }

    protected abstract Pair<T, SystemEntity> doProcess() throws ProcessingModelException;

    public abstract int getSystemEntityId();

    /**
     * This method is required by the processing function, in order to sort operations in the right way, in case of supersampled parameters
     * or operations affecting the same entity. In fact quicksort is not a stable sorting algorithm, so we need an additional secondary value to make sure that we
     * can handle supersampled parameters.
     *
     * @return the time used by the dispatcher thread to order the operations for execution
     */
    public abstract Instant getTime();

    public int getOrderingId() {
        return orderingId;
    }

    public void setOrderingId(int orderingId) {
        this.orderingId = orderingId;
    }
}
