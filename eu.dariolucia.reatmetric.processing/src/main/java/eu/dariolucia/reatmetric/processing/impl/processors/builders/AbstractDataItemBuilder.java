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

package eu.dariolucia.reatmetric.processing.impl.processors.builders;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

public abstract class AbstractDataItemBuilder<T extends AbstractDataItem> {

    protected final int id;

    protected final SystemEntityPath path;

    protected boolean changedSinceLastBuild;

    public AbstractDataItemBuilder(int id, SystemEntityPath path) {
        this.id = id;
        this.path = path;
        this.changedSinceLastBuild = false;
    }

    public boolean isChangedSinceLastBuild() {
        return changedSinceLastBuild;
    }

    public abstract T build(IUniqueId updateId);

    public abstract void setInitialisation(T item);
}
