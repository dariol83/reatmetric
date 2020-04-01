/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelInitialiser;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.util.Map;

public class ProcessingModelFactoryImpl implements IProcessingModelFactory {

    @Override
    public IProcessingModel build(Object definitionDatabase, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters, IProcessingModelInitialiser initialState) throws ProcessingModelException {
        return new ProcessingModelImpl((ProcessingDefinition) definitionDatabase, output, initialUniqueCounters, initialState);
    }
}
