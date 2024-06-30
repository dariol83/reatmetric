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
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.time.Instant;
import java.util.List;

public abstract class AbstractModelOperation<K extends AbstractSystemEntityProcessor> {

    private boolean abortOnException = false;

    private int orderingId;

    protected K processor;

    public K getProcessor() {
        return processor;
    }

    public void setProcessor(K processor) {
        this.processor = processor;
    }

    public List<AbstractDataItem> execute() throws ProcessingModelException {
        // A processor is required: if it is not set, exception
        if(processor == null) {
            throw new ProcessingModelException("Processor not set/not found to process operation " + getClass().getSimpleName() + " for entity " + getSystemEntityId());
        }
        return doProcess();
    }

    protected abstract List<AbstractDataItem> doProcess() throws ProcessingModelException;

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

    public boolean isAbortOnException() {
        return abortOnException;
    }

    protected void setAbortOnException(boolean abortOnException) {
        this.abortOnException = abortOnException;
    }
}
