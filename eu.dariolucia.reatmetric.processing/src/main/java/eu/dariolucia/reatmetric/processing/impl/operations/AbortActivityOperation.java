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

package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ActivityProcessor;

import java.time.Instant;
import java.util.List;

public class AbortActivityOperation extends AbstractModelOperation<ActivityProcessor> {

    private final int activityId;
    private final IUniqueId activityOccurrenceId;
    private final Instant time;

    public AbortActivityOperation(int activityId, IUniqueId activityOccurrenceId) {
        this.activityId = activityId;
        this.activityOccurrenceId = activityOccurrenceId;
        this.time = Instant.now();
    }

    @Override
    public Instant getTime() {
        return this.time;
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().abort(activityOccurrenceId);
    }

    @Override
    public int getSystemEntityId() {
        return activityId;
    }

    @Override
    public String toString() {
        return "'Abort Activity " + activityId + ":" + activityOccurrenceId + "'";
    }
}
