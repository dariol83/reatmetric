package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.input.VoidInputDataItem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to process a system entity of type CONTAINER.
 */
public class ContainerProcessor extends AbstractSystemEntityProcessor<ContainerProcessor.Definition, SystemEntity, VoidInputDataItem> {

    private final List<AbstractSystemEntityProcessor> childProcessors = new ArrayList<>();

    // TODO: we need to add a way here to pass the children of this processor (other AbstractSystemEntityProcessors... in the constructor or as setter)
    public ContainerProcessor(Definition definition, ProcessingModelImpl processingModel) {
        super(definition, processingModel);
    }

    public void addChildProcessor(AbstractSystemEntityProcessor processor) {
        this.childProcessors.add(processor);
    }

    @Override
    public SystemEntity process(VoidInputDataItem input) {
        // TODO
        return null;
    }

    @Override
    public SystemEntity evaluate() {
        return process(VoidInputDataItem.instance());
    }

    /**
     * Internal container definition class, instances are created on the fly, in memory only
     */
    public static class Definition extends AbstractProcessingDefinition {

        public Definition(int id, String description, String location) {
            super(id, description, location);
        }
    }
}
