/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.processing.*;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.util.Map;

public class ProcessingModelFactoryImpl implements IProcessingModelFactory {
    @Override
    public void setCommandHandler(String commandType, IActionHandler handler) {
        // TODO
    }

    @Override
    public IProcessingModel build(ProcessingDefinition definitionDatabase, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters) throws ProcessingModelException {
        return new ProcessingModelImpl(definitionDatabase, output, initialUniqueCounters);
    }
}
