package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.AbstractInputDataItem;

/**
 * This class is the parent class of all processing elements of the system entity model. A processing class is defined
 * by three template types:
 * <ul>
 *     <li>the processing definition that describes the system entity type and its processing characteristics</li>
 *     <li>the output (in terms of distributable and storable state) of the processing</li>
 *     <li>the input type (if any), which requires processing from the processing element and generates an output state</li>
 * </ul>
 * @param <J> the {@link AbstractProcessingDefinition} type, i.e. the entity definition
 * @param <T> the {@link AbstractDataItem} type, i.e. the visible output state after the processing
 * @param <K> the {@link AbstractInputDataItem} type, i.e. the input to the processing
 */
public abstract class AbstractSystemEntityProcessor<J extends AbstractProcessingDefinition, T extends AbstractDataItem, K extends AbstractInputDataItem> {

    protected volatile T state;

    protected volatile boolean enabled;

    protected final J definition;

    protected AbstractSystemEntityProcessor(J definition) {
        this.definition = definition;
    }

    public final J getDefinition() {
        return definition;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void enable() {
        this.enabled = true;
    }

    public final void disable() {
        this.enabled = false;
    }

    public abstract T process(K input);

    public abstract T evaluate();

    public final int getId() {
        return definition.getId();
    }

    public final T getState() {
        return state;
    }

    public final void initialise(T state) {
        this.state = state;
    }
}
