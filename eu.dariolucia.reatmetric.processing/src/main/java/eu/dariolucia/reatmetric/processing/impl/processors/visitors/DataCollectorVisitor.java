/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl.processors.visitors;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;

import java.util.LinkedList;
import java.util.List;

public class DataCollectorVisitor implements IProcessingModelVisitor {

    private final AbstractDataItemFilter filter;
    private final List<AbstractDataItem> result = new LinkedList<>();

    public DataCollectorVisitor(AbstractDataItemFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean shouldDescend(SystemEntity entity) {
        return filter == null || filter.select(entity);
    }

    @Override
    public void startVisit(SystemEntity path) {
        // Nothing here
    }

    @Override
    public void onVisit(AbstractDataItem item) {
        if(filter == null && item != null) {
            result.add(item);
        } else {
            if (item != null && item.getClass().equals(filter.getDataItemType())) {
                if (filter.test(item)) {
                    result.add(item);
                }
            }
        }
    }

    @Override
    public void endVisit(SystemEntity path) {
        // Nothing here
    }

    public List<AbstractDataItem> getCollectedData() {
        return result;
    }
}
