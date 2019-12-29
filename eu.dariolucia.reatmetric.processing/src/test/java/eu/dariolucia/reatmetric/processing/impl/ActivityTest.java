/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
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

        // Request activity execution: nominal
        {
            outList.clear();
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

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT1", state.getName());
            assertEquals("ROOT.ELEMENT.ACT1", state.getPath().asString());
            assertEquals(true, state.getArguments().get("ARG1"));
            assertEquals(43L, state.getArguments().get("ARG2"));
            assertEquals(12.4, state.getArguments().get("ARG3"));
            assertEquals(1, state.getArguments().get("ARG4"));
            assertEquals("hello world", state.getProperties().get("custom"));
            assertEquals("100", state.getProperties().get("spacecraft-id"));
            assertEquals("2007-12-03T10:15:30.00123Z", state.getProperties().get("schedule-time"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETION, state.getCurrentState());
        }

        // Request activity execution: change of fixed value
        {
            outList.clear();
            testLogger.info("Invocation 1");

            ActivityRequest ar1 = ActivityRequest.newRequest(1000)
                    .withArgument(ActivityArgument.ofSource("ARG1", true))
                    .withArgument(ActivityArgument.ofSource("ARG3", 4.5))
                    .withArgument(ActivityArgument.ofEngineering("ARG4", "ON"))
                    .withRoute("A")
                    .build();
            try {
                model.startActivity(ar1);
                fail("Exception expected: attempt to update of fixed argument ARG3");
            } catch(ProcessingModelException e) {
                // good, check exception message
                e.printStackTrace();
                assertTrue(e.getMessage().startsWith("Supplied argument ARG3"));
            }
        }

    }
}