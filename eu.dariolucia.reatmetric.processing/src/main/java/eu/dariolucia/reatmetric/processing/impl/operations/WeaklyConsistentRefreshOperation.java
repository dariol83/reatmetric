/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.time.Instant;
import java.util.List;

public class WeaklyConsistentRefreshOperation extends AbstractModelOperation<AbstractSystemEntityProcessor<?,?,?>> {

    private final Instant creationTime = Instant.now();

    private final int systemEntityId;

    public WeaklyConsistentRefreshOperation(int id) {
        this.systemEntityId = id;
    }

    @Override
    public Instant getTime() {
        return creationTime;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().evaluate(true);
    }

    @Override
    public int getSystemEntityId() {
        return systemEntityId;
    }

    @Override
    public String toString() {
        return "'Refresh weakly consistent system entity " + getSystemEntityId() + " state'";
    }
}
