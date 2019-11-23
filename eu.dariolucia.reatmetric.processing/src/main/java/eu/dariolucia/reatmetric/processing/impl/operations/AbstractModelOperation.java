package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.util.function.Supplier;
// TODO: we might need to have a field of type Instant to support supersampled parameters
public abstract class AbstractModelOperation<T extends AbstractDataItem, K extends AbstractSystemEntityProcessor> implements Supplier<T> {

    private int orderingId;

    protected K processor;

    protected K getProcessor() {
        return processor;
    }

    public void setProcessor(K processor) {
        this.processor = processor;
    }

    @Override
    public T get() {
        return doProcess();
    }

    protected abstract T doProcess();

    public abstract int getSystemEntityId();

    public int getOrderingId() {
        return orderingId;
    }

    public void setOrderingId(int orderingId) {
        this.orderingId = orderingId;
    }
}
