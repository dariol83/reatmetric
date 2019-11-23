package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

public class ParameterSampleProcessOperation extends AbstractModelOperation<ParameterData, ParameterProcessor> {

    private final ParameterSample input;

    public ParameterSampleProcessOperation(ParameterSample input) {
        this.input = input;
    }

    @Override
    protected ParameterData doProcess() {
        return getProcessor().process(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }
}
