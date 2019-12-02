/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.processing.IProcessingModel;
import eu.dariolucia.reatmetric.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

class ProcessingModelFactoryImplTest {

    @Test
    void loadModel() throws JAXBException, ProcessingModelException, InterruptedException {
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        IProcessingModelOutput output = items -> items.forEach(System.out::println);
        IProcessingModel model = factory.build(pd, output, null);

        ParameterSample p1 = ParameterSample.of(101, true);
        ParameterSample p2 = ParameterSample.of(102, 1);
        ParameterSample p3 = ParameterSample.of(101, false);
        System.out.println("Injection - Batch 1");
        model.injectParameters(Arrays.asList(p1, p2, p3));

        Thread.sleep(1000);
        System.out.println("Disable");
        model.disable(SystemEntityPath.fromString("ROOT.ELEMENT1"));

        Thread.sleep(1000);
        System.out.println("Injection - Batch 2");
        p1 = ParameterSample.of(101, true);
        p2 = ParameterSample.of(102, 2);
        p3 = ParameterSample.of(101, false);
        model.injectParameters(Arrays.asList(p1, p2, p3));

        Thread.sleep(1000);
        System.out.println("Enable");
        model.enable(SystemEntityPath.fromString("ROOT.ELEMENT1"));

        Thread.sleep(1000);
        System.out.println("Injection - Batch 3");
        p1 = ParameterSample.of(101, true);
        p2 = ParameterSample.of(102, 3);
        p3 = ParameterSample.of(101, false);
        model.injectParameters(Arrays.asList(p1, p2, p3));

        Thread.sleep(1000);
    }

    @Test
    void testBatteryModel() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new LinkedList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        ParameterSample batteryState = ParameterSample.of(1001, true);
        ParameterSample batteryTension = ParameterSample.of(1002, 1000L);
        ParameterSample batteryCurrent = ParameterSample.of(1003, 1L);

        testLogger.info("Injection - Batch 1");
        model.injectParameters(Arrays.asList(batteryCurrent, batteryState, batteryTension));

        Thread.sleep(1000);
        // TODO: Verify: 1001, 1002, 1003
        System.out.println("List size: " + outList.size());
        outList.forEach(System.out::println);
    }

}