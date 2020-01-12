package eu.dariolucia.reatmetric.processing.impl.operations;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.impl.processors.ParameterProcessor;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;

import java.time.Instant;
import java.util.List;

public class ParameterSampleProcessOperation extends AbstractModelOperation<ParameterProcessor> {

    private final ParameterSample input;

    public ParameterSampleProcessOperation(ParameterSample input) {
        this.input = input;
    }

    @Override
    public Instant getTime() {
        return input.getGenerationTime();
    }

    @Override
    protected List<AbstractDataItem> doProcess() throws ProcessingModelException {
        return getProcessor().process(input);
    }

    @Override
    public int getSystemEntityId() {
        return input.getId();
    }
}
