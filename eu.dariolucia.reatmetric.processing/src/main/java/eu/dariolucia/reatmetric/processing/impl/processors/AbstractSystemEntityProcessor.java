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

package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.AbstractInputDataItem;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.graph.EntityVertex;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.SystemEntityBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is the parent class of all processing elements of the system entity model. A processing class is defined
 * by three template types:
 * <ul>
 *     <li>the processing definition that describes the system entity type and its processing characteristics</li>
 *     <li>the main output (in terms of distributable and storable state) of the processing: ancillary states can still be produced</li>
 *     <li>the input type (if any), which requires processing from the processing element and generates an output state</li>
 * </ul>
 * @param <J> the {@link AbstractProcessingDefinition} type, i.e. the entity definition
 * @param <T> the {@link AbstractDataItem} type, i.e. the visible output state after the processing
 * @param <K> the {@link AbstractInputDataItem} type, i.e. the input to the processing
 */
public abstract class AbstractSystemEntityProcessor<J extends AbstractProcessingDefinition, T extends AbstractDataItem, K extends AbstractInputDataItem> {

    protected volatile SystemEntity entityState;

    protected final AtomicReference<T> state = new AtomicReference<>();

    protected volatile Status entityStatus;

    protected final ProcessingModelImpl processor;

    protected final J definition;

    protected final SystemEntityPath path;

    protected final SystemEntityBuilder systemEntityBuilder;
    private EntityVertex entityVertex;

    protected AbstractSystemEntityProcessor(J definition, ProcessingModelImpl processor, SystemEntityType type) {
        this.definition = definition;
        this.processor = processor;
        this.path = SystemEntityPath.fromString(definition.getLocation());
        this.entityStatus = Status.ENABLED;
        this.systemEntityBuilder = new SystemEntityBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()), type);
        this.systemEntityBuilder.setStatus(entityStatus);
        // The entity state is created in the child constructor
    }

    protected AlarmState getInitialAlarmState() {
        return AlarmState.NOT_APPLICABLE;
    }

    protected final SystemEntityPath getPath() {
        return path;
    }

    public final J getDefinition() {
        return definition;
    }

    public final Status getEntityStatus() {
        return entityStatus;
    }

    public List<AbstractDataItem> enable() throws ProcessingModelException {
        if(this.entityStatus != Status.ENABLED) {
            this.entityStatus = Status.ENABLED;
            return evaluate(true);
        } else {
            return Collections.emptyList();
        }
    }

    public List<AbstractDataItem> disable() throws ProcessingModelException {
        if(this.entityStatus != Status.DISABLED) {
            this.entityStatus = Status.DISABLED;
            return evaluate(true);
        } else {
            return Collections.emptyList();
        }
    }

    public List<AbstractDataItem> ignore() throws ProcessingModelException {
        if(this.entityStatus != Status.IGNORED) {
            this.entityStatus = Status.IGNORED;
            return evaluate(true);
        } else {
            return Collections.emptyList();
        }
    }

    public abstract List<AbstractDataItem> process(K input) throws ProcessingModelException;

    public abstract List<AbstractDataItem> evaluate(boolean includeWeakly) throws ProcessingModelException;

    /**
     * Override when necessary.
     *
     * @return true if weakly consistent.
     */
    public boolean isWeaklyConsistent() {
        return false;
    }

    public final int getSystemEntityId() {
        return definition.getId();
    }

    public final T getState() {
        return state.get();
    }

    public final SystemEntity getEntityState() {
        return entityState;
    }

    public abstract void visit(IProcessingModelVisitor visitor);

    public abstract void putCurrentStates(List<AbstractDataItem> items);

    public abstract AbstractSystemEntityDescriptor getDescriptor();

    public void setEntityVertex(EntityVertex entityVertex) {
        this.entityVertex = entityVertex;
    }

    protected EntityVertex getEntityVertex() {
        return this.entityVertex;
    }
}
