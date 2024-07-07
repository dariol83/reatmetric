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

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterWeakTest {

    @BeforeEach
    void setup() {
        Logger packageLogger = Logger.getLogger("eu.dariolucia.reatmetric.processing");
        packageLogger.setLevel(Level.ALL);
        Arrays.stream(Logger.getLogger("").getHandlers()).forEach(o -> o.setLevel(Level.ALL));
    }

    @Test
    void testWeakParameterUpdate() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters_weak.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<List<AbstractDataItem>> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::add;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");
        ParameterSample p1 = ParameterSample.of(101, true);
        model.injectParameters(List.of(p1));

        testLogger.info("Injection - Batch 2");
        ParameterSample p2 = ParameterSample.of(104, 3);
        ParameterSample p3 = ParameterSample.of(106, 9);
        model.injectParameters(List.of(p2));
        model.injectParameters(List.of(p3));
        AwaitUtil.awaitCondition(10000, () -> outList.size() == 3);

        testLogger.info("Injection - Batch 3");
        ParameterSample p4 = ParameterSample.of(105, 1.0);
        ParameterSample p5 = ParameterSample.of(107, 2.0);
        ParameterSample p6 = ParameterSample.of(108, 3.0);
        ParameterSample p7 = ParameterSample.of(102, 2);
        model.injectParameters(List.of(p4));
        model.injectParameters(List.of(p5, p6));
        model.injectParameters(List.of(p7));

        AwaitUtil.awaitCondition(10000, () -> outList.size() == 8);

        // Check values
        assertEquals(2, rawValueOf(102, model));
        assertEquals(144.0, rawValueOf(1034, model));
        assertEquals(12.0, rawValueOf(1033, model));
        assertEquals(6.0, rawValueOf(1032, model));
        assertEquals(12L, rawValueOf(1031, model));

        assertEquals(8, outList.size());
    }

    private Object rawValueOf(int id, IProcessingModel model) throws ProcessingModelException {
        List<AbstractDataItem> items = model.getById(List.of(id));
        return ((ParameterData) items.get(0)).getSourceValue();
    }
}