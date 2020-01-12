/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.processing;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;

import java.util.Map;

public interface IProcessingModelFactory {
    IProcessingModel build(Object definitionDatabase, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters) throws ProcessingModelException;
}
