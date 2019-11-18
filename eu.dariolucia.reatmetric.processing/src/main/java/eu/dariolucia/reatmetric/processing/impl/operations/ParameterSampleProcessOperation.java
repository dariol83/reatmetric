package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;

public class ParameterSampleProcessOperation extends AbstractModelOperation<ParameterData> {

    private final ParameterSample input;
    private final ParameterProcessor processor;

    public ParameterSampleProcessOperation(ParameterSample input, ParameterProcessor processor) {
        this.input = input;
        this.processor = processor;
    }

    @Override
    protected ParameterData doProcess() {
        return processor.process(input);
    }

    @Override
    public int getSystemEntityId() {
        return processor.getId();
    }
}
