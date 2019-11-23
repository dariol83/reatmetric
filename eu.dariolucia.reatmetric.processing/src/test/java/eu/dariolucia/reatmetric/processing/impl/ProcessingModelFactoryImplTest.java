/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.processing.IProcessingModel;
import eu.dariolucia.reatmetric.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.util.Arrays;

class ProcessingModelFactoryImplTest {

    @Test
    void loadModel() throws JAXBException, ProcessingModelException, InterruptedException {
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        IProcessingModelOutput output = items -> items.forEach(System.out::println);
        IProcessingModel model = factory.build(pd, output, null);

        ParameterSample p1 = ParameterSample.of(101, true);
        ParameterSample p2 = ParameterSample.of(102, AlarmState.UNKNOWN);
        model.injectParameters(Arrays.asList(p1, p2));

        Thread.sleep(1000);
    }

}