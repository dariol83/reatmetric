package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;

import java.util.function.Supplier;

public abstract class AbstractModelOperation<T extends AbstractDataItem> implements Supplier<T> {

    @Override
    public T get() {
        return doProcess();
    }

    protected abstract T doProcess();

    public abstract int getSystemEntityId();

}
