/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.processing.IProcessingModel;
import eu.dariolucia.reatmetric.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.stubs.ActivityHandlerStub;
import eu.dariolucia.reatmetric.processing.input.ActivityArgument;
import eu.dariolucia.reatmetric.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {

    @BeforeEach
    void setup() {
        Logger packageLogger = Logger.getLogger("eu.dariolucia.reatmetric.processing");
        packageLogger.setLevel(Level.ALL);
        Arrays.stream(Logger.getLogger("").getHandlers()).forEach(o -> o.setLevel(Level.ALL));
    }


    @Test
    void testActivity1() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_activities.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Create activity handler for route A and type TC
        ActivityHandlerStub h1 = ActivityHandlerStub.create().withRoutes("A", "B").withTypes("TC").build();

        // Register handler
        model.registerActivityHandler(h1);

        // Request activity execution
        testLogger.info("Invocation 1");

        ActivityRequest ar1 = ActivityRequest.newRequest(1000)
                .withArgument(ActivityArgument.ofSource("ARG1", true))
                .withArgument(ActivityArgument.ofEngineering("ARG4", "ON"))
                .withProperty("custom", "hello world")
                .withRoute("A")
                .build();
        IUniqueId id1 = model.startActivity(ar1);
        assertEquals(0L, id1.asLong());

        //
        AwaitUtil.awaitAndVerify(10000, outList::size, 18);

        ((ActivityOccurrenceData)outList.get(outList.size() - 1)).getProgressReports().forEach(System.out::println); // TODO remove
    }
}