/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
