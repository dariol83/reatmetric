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
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.VoidInputDataItem;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.operations.AbstractModelOperation;
import eu.dariolucia.reatmetric.processing.impl.operations.EnableDisableOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is used to process a system entity of type CONTAINER.
 */
public class ContainerProcessor extends AbstractSystemEntityProcessor<ContainerProcessor.Definition, SystemEntity, VoidInputDataItem> {

    private static final Logger LOG = Logger.getLogger(ContainerProcessor.class.getName());

    private final List<AbstractSystemEntityProcessor> childProcessors = new ArrayList<>();

    private final ContainerDescriptor descriptor;

    public ContainerProcessor(Definition definition, ProcessingModelImpl processingModel) {
        super(definition, processingModel, SystemEntityType.CONTAINER);
        // Initialise the entity state
        this.systemEntityBuilder.setAlarmState(getInitialAlarmState());
        this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
        this.descriptor = new ContainerDescriptor(getPath());
    }

    public void addChildProcessor(AbstractSystemEntityProcessor processor) {
        if(!this.childProcessors.contains(processor)) {
            this.childProcessors.add(processor);
        }
    }

    @Override
    public List<AbstractDataItem> process(VoidInputDataItem input) {
        this.systemEntityBuilder.setStatus(entityStatus);
        this.systemEntityBuilder.setAlarmState(AlarmState.NOT_APPLICABLE);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            SystemEntity newState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            this.state.set(newState);
            this.entityState = newState;
            return List.of(this.entityState);
        } else {
            // No reason to send out anything relevant
            return Collections.emptyList();
        }
    }

    @Override
    public List<AbstractDataItem> evaluate(boolean includeWeakly) {
        return process(VoidInputDataItem.instance());
    }

    @Override
    public void visit(IProcessingModelVisitor visitor) {
        for(AbstractSystemEntityProcessor proc : childProcessors) {
            SystemEntity toVisit = proc.getEntityState();
            if(visitor.shouldDescend(toVisit)) {
                visitor.startVisit(toVisit);
                proc.visit(visitor);
                visitor.endVisit(toVisit);
            }
        }
    }

    @Override
    public void putCurrentStates(List<AbstractDataItem> items) {
        // Nothing to do
    }

    @Override
    public AbstractSystemEntityDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public List<AbstractDataItem> enable() throws ProcessingModelException {
        // Propagate
        propagateEnablement(Status.ENABLED);
        // Process now
        return super.enable();
    }

    @Override
    public List<AbstractDataItem> disable() throws ProcessingModelException {
        // Propagate
        propagateEnablement(Status.DISABLED);
        // Process now
        return super.disable();
    }

    private void propagateEnablement(Status toBeApplied) {
        // One layer only
        List<AbstractModelOperation<?>> ops = new ArrayList<>(childProcessors.size());
        for(AbstractSystemEntityProcessor proc : childProcessors) {
            ops.add(new EnableDisableOperation(proc.getSystemEntityId(), toBeApplied));
        }
        // Schedule operation
        processor.scheduleTask(ops, ProcessingModelImpl.COMMAND_DISPATCHING_QUEUE, true);
    }

    @Override
    public List<AbstractDataItem> ignore() throws ProcessingModelException {
        // Propagate
        propagateEnablement(Status.IGNORED);
        // Process now
        return super.ignore();
    }

    public List<SystemEntity> getContainedEntities() {
        List<SystemEntity> states = new ArrayList<>(childProcessors.size());
        for(AbstractSystemEntityProcessor proc : childProcessors) {
            states.add(proc.getEntityState());
        }
        return states;
    }

    /**
     * Internal container definition class, instances are created on the fly, in memory only
     */
    public static class Definition extends AbstractProcessingDefinition {

        public Definition(int id, String description, String location) {
            super(id, description, location);
        }

        @Override
        public void preload() {
            // Nothing to do here
        }
    }
}
