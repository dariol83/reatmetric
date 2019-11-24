package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

import java.time.Instant;

public class ParameterSampleProcessOperation extends AbstractModelOperation<ParameterData, ParameterProcessor> {

    private final ParameterSample input;

    public ParameterSampleProcessOperation(ParameterSample input) {
        this.input = input;
    }

    @Override
    public Instant getTime() {
        return input.getGenerationTime();
    }

    @Override
    protected Pair<ParameterData, SystemEntity> doProcess() throws ProcessingModelException {
        return getProcessor().process(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }
}
