package eu.dariolucia.reatmetric.processing.impl.processors;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.processing.definition.AbstractProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.ProcessingModelImpl;
import eu.dariolucia.reatmetric.processing.input.VoidInputDataItem;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is used to process a system entity of type CONTAINER.
 */
public class ContainerProcessor extends AbstractSystemEntityProcessor<ContainerProcessor.Definition, SystemEntity, VoidInputDataItem> {

    private static final Logger LOG = Logger.getLogger(ContainerProcessor.class.getName());

    private final List<AbstractSystemEntityProcessor> childProcessors = new ArrayList<>();

    public ContainerProcessor(Definition definition, ProcessingModelImpl processingModel) {
        super(definition, processingModel, SystemEntityType.CONTAINER);
    }

    public void addChildProcessor(AbstractSystemEntityProcessor processor) {
        this.childProcessors.add(processor);
    }

    @Override
    public Pair<SystemEntity, SystemEntity> process(VoidInputDataItem input) {
        this.systemEntityBuilder.setStatus(entityStatus);
        this.systemEntityBuilder.setAlarmState(entityStatus == Status.ENABLED ? deriveAlarmState() : AlarmState.NOT_CHECKED);
        if(this.systemEntityBuilder.isChangedSinceLastBuild()) {
            this.state = this.systemEntityBuilder.build(new LongUniqueId(processor.getNextId(SystemEntity.class)));
            this.entityState = this.state;
            return Pair.of(null, this.entityState);
        } else {
            // No reason to send out anything relevant
            return Pair.of(null, null);
        }
    }

    private AlarmState deriveAlarmState() {
        AlarmState result = AlarmState.NOMINAL;
        for(AbstractSystemEntityProcessor child : childProcessors) {
            if(child.getEntityState().getAlarmState().ordinal() < result.ordinal()) {
                result = child.getEntityState().getAlarmState();
            }
        }
        return result;
    }

    @Override
    public Pair<SystemEntity, SystemEntity> evaluate() {
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
