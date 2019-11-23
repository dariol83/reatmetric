package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

public class SystemEntityUpdateOperation extends AbstractModelOperation<AbstractDataItem, AbstractSystemEntityProcessor> {

    @Override
    protected AbstractDataItem doProcess() {
        return getProcessor().evaluate();
    }

    @Override
    public int getSystemEntityId() {
        return getProcessor().getSystemEntityId();
    }
}
