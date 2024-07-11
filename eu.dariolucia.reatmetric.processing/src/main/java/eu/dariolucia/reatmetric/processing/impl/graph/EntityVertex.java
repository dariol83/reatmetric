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

package eu.dariolucia.reatmetric.processing.impl.graph;

import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;
import eu.dariolucia.reatmetric.processing.impl.operations.SystemEntityUpdateOperation;
import eu.dariolucia.reatmetric.processing.impl.processors.AbstractSystemEntityProcessor;

import java.util.*;

public class EntityVertex {

    private List<AbstractModelOperation<?>> updateOperationsForAffectedEntities = null;
    private int orderingId;

    private final AbstractSystemEntityProcessor processor;
    private final Set<DependencyEdge> successors = new HashSet<>();
    private final Set<DependencyEdge> predecessors = new HashSet<>();
    private final AbstractModelOperation updateOperation;

    public EntityVertex(AbstractSystemEntityProcessor processor) {
        this.processor = processor;
        this.updateOperation = new SystemEntityUpdateOperation();
        this.updateOperation.setProcessor(this.processor);
        this.processor.setEntityVertex(this);
    }

    public int getSystemEntityId() {
        return this.processor.getSystemEntityId();
    }

    public Set<DependencyEdge> getSuccessors() {
        return successors;
    }

    public Set<DependencyEdge> getPredecessors() {
        return predecessors;
    }

    public AbstractModelOperation getUpdateOperation() {
        return updateOperation;
    }

    public void setOrderingId(int orderingId) {
        this.orderingId = orderingId;
        updateOperation.setOrderingId(orderingId);
    }

    public int getOrderingId() {
        return orderingId;
    }

    public void assignProcessor(AbstractModelOperation operation) {
        operation.setOrderingId(orderingId);
        operation.setProcessor(processor);
    }

    public synchronized List<AbstractModelOperation<?>> getUpdateOperationsForAffectedEntities(boolean includeWeaklyConsistent) {
        // If we are processing only consistent system elements, then this object, being weakly consistent, will be evaluated,
        // but it will not contribute with update operations to affected entities
        if(!includeWeaklyConsistent && getProcessor().isWeaklyConsistent()) {
            return Collections.emptyList();
        }
        // The method is synchronized to avoid double computation
        if(updateOperationsForAffectedEntities == null) {
            // The affected entities are the vertex's predecessors + their affected entities
            updateOperationsForAffectedEntities = new LinkedList<>();
            //
            Set<Integer> processed = new HashSet<>();
            for(DependencyEdge de : predecessors) {
                // The direct predecessor
                updateOperationsForAffectedEntities.add(de.getSource().getUpdateOperation());
                processed.add(de.getSource().getSystemEntityId());
                // The items affected by the predecessor: we always force the exclusion of weakly consistent parameters here: this is right, do not modify it! :)
                for (AbstractModelOperation<?> amd : de.getSource().getUpdateOperationsForAffectedEntities(false)) {
                    if (!processed.contains(amd.getSystemEntityId())) {
                        processed.add(amd.getSystemEntityId());
                        updateOperationsForAffectedEntities.add(amd);
                    }
                }
            }
        }
        return updateOperationsForAffectedEntities;
    }

    @Override
    public int hashCode() {
        return processor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return processor == obj;
    }

    public AbstractSystemEntityProcessor getProcessor() {
        return processor;
    }
}
