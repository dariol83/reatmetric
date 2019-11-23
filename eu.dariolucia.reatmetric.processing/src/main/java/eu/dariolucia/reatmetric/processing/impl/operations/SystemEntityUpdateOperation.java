package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

public class SystemEntityUpdateOperation extends AbstractModelOperation<AbstractDataItem, AbstractSystemEntityProcessor> {

    @Override
    protected Pair<AbstractDataItem, SystemEntity> doProcess() {
        return getProcessor().evaluate();
    }

    @Override
    public int getSystemEntityId() {
        return getProcessor().getSystemEntityId();
    }
}
