package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelVisitor;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.impl.processors.builders.SystemEntityBuilder;
import eu.dariolucia.reatmetric.api.processing.input.AbstractInputDataItem;

import java.util.List;

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

    protected volatile T state;

    protected volatile Status entityStatus;

    protected final ProcessingModelImpl processor;

    protected final J definition;

    protected final SystemEntityPath path;

    protected final SystemEntityBuilder systemEntityBuilder;

    protected AbstractSystemEntityProcessor(J definition, ProcessingModelImpl processor, SystemEntityType type) {
        this.definition = definition;
        this.processor = processor;
        this.path = SystemEntityPath.fromString(definition.getLocation());
        this.entityStatus = Status.ENABLED;
        this.systemEntityBuilder = new SystemEntityBuilder(definition.getId(), SystemEntityPath.fromString(definition.getLocation()), type);
        this.systemEntityBuilder.setStatus(entityStatus);
        this.systemEntityBuilder.setAlarmState(getInitialAlarmState());
        this.entityState = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
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
        this.entityStatus = Status.ENABLED;
        return evaluate();
    }

    public List<AbstractDataItem> disable() throws ProcessingModelException {
        this.entityStatus = Status.DISABLED;
        return evaluate();
    }

    public abstract List<AbstractDataItem> process(K input) throws ProcessingModelException;

    public abstract List<AbstractDataItem> evaluate() throws ProcessingModelException;

    public final int getSystemEntityId() {
        return definition.getId();
    }

    public final T getState() {
        return state;
    }

    public final SystemEntity getEntityState() {
        return entityState;
    }

    public void initialise(List<T> state, SystemEntity entityState) {
        this.state = state.isEmpty() ? null : state.get(0);
        this.entityState = entityState;
    }

    public abstract void visit(IProcessingModelVisitor visitor);

    public abstract void putCurrentStates(List<AbstractDataItem> items);
}
