/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.api.processing;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;

import java.util.Map;

/**
 * Factory interface for creating processing models.
 * Implementations of this interface are responsible for building processing models
 * from definition databases and initial state.
 */
public interface IProcessingModelFactory {
    /**
     * Builds a processing model from the specified definition database, output, and initial counters.
     * This is a default method that delegates to the full build method with null initial state.
     *
     * @param definitionDatabase the definition database to use for building the model
     * @param output the output handler for the processing model
     * @param initialUniqueCounters the initial unique counters for data item classes
     * @return the built processing model
     * @throws ProcessingModelException if the model cannot be built
     */
    default IProcessingModel build(Object definitionDatabase, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters) throws ProcessingModelException {
        return build(definitionDatabase, output, initialUniqueCounters, null);
    }

    /**
     * Builds a processing model from the specified definition database, output, initial counters, and initial state.
     *
     * @param definitionDatabase the definition database to use for building the model
     * @param output the output handler for the processing model
     * @param initialUniqueCounters the initial unique counters for data item classes
     * @param initialState the initial state for the processing model, can be null
     * @return the built processing model
     * @throws ProcessingModelException if the model cannot be built
     */
    IProcessingModel build(Object definitionDatabase, IProcessingModelOutput output, Map<Class<? extends AbstractDataItem>, Long> initialUniqueCounters, IProcessingModelInitialiser initialState) throws ProcessingModelException;
}
