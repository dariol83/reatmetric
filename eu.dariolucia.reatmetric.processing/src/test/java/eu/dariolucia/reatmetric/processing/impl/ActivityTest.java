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

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.impl.stubs.ActivityHandlerStub;
import eu.dariolucia.reatmetric.processing.impl.stubs.NominalLifecycleStrategy;
import eu.dariolucia.reatmetric.processing.impl.stubs.SchedulingLifecycleStrategy;
import eu.dariolucia.reatmetric.api.processing.input.PlainActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.time.Instant;
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

            ActivityRequest ar1 = ActivityRequest.newRequest(1000, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withProperty("custom", "hello world")
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

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
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Request activity execution: change of fixed value (wrong)
        {
            outList.clear();
            testLogger.info("Invocation 2");

            ActivityRequest ar1 = ActivityRequest.newRequest(1000, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofSource("ARG3", 4.5))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            try {
                model.startActivity(ar1);
                fail("Exception expected: attempt to update of fixed argument ARG3");
            } catch(ProcessingModelException e) {
                // good, check exception message
                assertTrue(e.getMessage().startsWith("Supplied argument ARG3"));
            }
        }

        // Request activity execution: change of fixed value (correct)
        {
            outList.clear();
            testLogger.info("Invocation 3");

            ActivityRequest ar1 = ActivityRequest.newRequest(1000, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", false))
                    .withArgument(PlainActivityArgument.ofSource("ARG3", 12.4))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "OFF"))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();

            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(2L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT1", state.getName());
            assertEquals("ROOT.ELEMENT.ACT1", state.getPath().asString());
            assertEquals(false, state.getArguments().get("ARG1"));
            assertEquals(43L, state.getArguments().get("ARG2"));
            assertEquals(12.4, state.getArguments().get("ARG3"));
            assertEquals(0, state.getArguments().get("ARG4"));
            assertEquals("100", state.getProperties().get("spacecraft-id"));
            assertEquals("2007-12-03T10:15:30.00123Z", state.getProperties().get("schedule-time"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity2() throws JAXBException, ProcessingModelException, InterruptedException {
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

            ActivityRequest ar1 = ActivityRequest.newRequest(1001, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG2", 1000L))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withProperty("custom", "hello world")
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT2", state.getName());
            assertEquals("ROOT.ELEMENT.ACT2", state.getPath().asString());
            assertEquals(true, state.getArguments().get("ARG1"));
            assertEquals(1000L, state.getArguments().get("ARG2"));
            assertEquals(12.4, state.getArguments().get("ARG3"));
            assertEquals(11, state.getArguments().get("ARG4"));
            assertEquals("hello world", state.getProperties().get("custom"));
            assertEquals("101", state.getProperties().get("spacecraft-id"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Request activity execution: wrong value on ARG2
        {
            outList.clear();
            testLogger.info("Invocation 2");

            ActivityRequest ar1 = ActivityRequest.newRequest(1001, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG2", 70000L))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            try {
                model.startActivity(ar1);
                fail("Exception expected: limit value for ARG2 not detected");
            } catch(ProcessingModelException e) {
                // good, check exception message
                assertTrue(e.getMessage().startsWith("Value 70000 of argument ARG2"));
            }
        }

        // Request activity execution: use of default parameter reference
        {
            outList.clear();
            testLogger.info("Invocation 3");

            // Inject parameter 108 to contain OFF
            model.injectParameters(Collections.singletonList(ParameterSample.of(108, "OFF")));

            // Check for parameter data and system entity items
            AwaitUtil.awaitAndVerify(5000, outList::size, 2);

            outList.clear();
            // Invoke activity
            ActivityRequest ar1 = ActivityRequest.newRequest(1001, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", false))
                    .withArgument(PlainActivityArgument.ofSource("ARG2", 3000L))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();

            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(2L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT2", state.getName());
            assertEquals("ROOT.ELEMENT.ACT2", state.getPath().asString());
            assertEquals(false, state.getArguments().get("ARG1"));
            assertEquals(3000L, state.getArguments().get("ARG2"));
            assertEquals(12.4, state.getArguments().get("ARG3"));
            assertEquals(10, state.getArguments().get("ARG4"));
            assertEquals("101", state.getProperties().get("spacecraft-id"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Request activity execution: wrong value on referenced ARG4
        {
            outList.clear();
            testLogger.info("Invocation 4");

            // Inject parameter 108 to contain OFF
            model.injectParameters(Collections.singletonList(ParameterSample.of(108, "WHATEVER")));

            // Check for parameter data item
            AwaitUtil.awaitAndVerify(5000, outList::size, 1);

            outList.clear();

            ActivityRequest ar1 = ActivityRequest.newRequest(1001, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG2", 4000L))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            try {
                model.startActivity(ar1);
                fail("Exception expected: limit value for ARG4 not detected");
            } catch(ProcessingModelException e) {
                // good, check exception message
                assertTrue(e.getMessage().startsWith("Value 1010 of argument ARG4"));
            }
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity2WithResult() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_activities.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Create activity handler for route A and type TC
        ActivityHandlerStub h1 = ActivityHandlerStub.create()
                .withRoutes("A", "B")
                .withTypes("TC")
                .withLifecycle(new NominalLifecycleStrategy(3, 2000, 2, 1000, () -> 4L))
                .build();

        // Register handler
        model.registerActivityHandler(h1);

        // Request activity execution: nominal
        {
            outList.clear();
            testLogger.info("Invocation 1");

            ActivityRequest ar1 = ActivityRequest.newRequest(1001, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG2", 1000L))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withProperty("custom", "hello world")
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 15);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT2", state.getName());
            assertEquals("ROOT.ELEMENT.ACT2", state.getPath().asString());
            assertEquals(true, state.getArguments().get("ARG1"));
            assertEquals(1000L, state.getArguments().get("ARG2"));
            assertEquals(12.4, state.getArguments().get("ARG3"));
            assertEquals(11, state.getArguments().get("ARG4"));
            assertEquals("hello world", state.getProperties().get("custom"));
            assertEquals("101", state.getProperties().get("spacecraft-id"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertEquals(4L, state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity3() throws JAXBException, ProcessingModelException, InterruptedException {
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
            // Inject parameter 103 to contain value 123
            model.injectParameters(Collections.singletonList(ParameterSample.of(103, 123)));
            // Check for parameter data and system entity items
            AwaitUtil.awaitAndVerify(5000, outList::size, 2);
            outList.clear();
            // Invoke
            ActivityRequest ar1 = ActivityRequest.newRequest(1002, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", 120L))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT3", state.getName());
            assertEquals("ROOT.ELEMENT.ACT3", state.getPath().asString());
            assertEquals(120L, state.getArguments().get("ARG1"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.VERIFICATION, state.getCurrentState());

            // Now inject parameter 103 with 120
            model.injectParameters(Collections.singletonList(ParameterSample.of(103, 120)));

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 19); // Parameter data item plus 1 activity occurrence updates
        }

        // Request activity execution: failure
        {
            outList.clear();
            testLogger.info("Invocation 2");
            // Invoke
            ActivityRequest ar1 = ActivityRequest.newRequest(1002, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", 121L))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(2L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(10000, outList::size, 17);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT3", state.getName());
            assertEquals("ROOT.ELEMENT.ACT3", state.getPath().asString());
            assertEquals(121L, state.getArguments().get("ARG1"));
            assertEquals(ActivityOccurrenceState.VERIFICATION, state.getCurrentState());

            // Now inject parameter 103 with 122
            model.injectParameters(Collections.singletonList(ParameterSample.of(103, 122)));

            //
            AwaitUtil.awaitAndVerify(5000, outList::size, 18); // Parameter data item only

            // Now inject parameter 103 with 129
            model.injectParameters(Collections.singletonList(ParameterSample.of(103, 129)));

            //
            AwaitUtil.awaitAndVerify(5000, outList::size, 19); // Parameter data item only

            // Stop and wait for other 6 seconds max
            AwaitUtil.awaitAndVerify(5000, outList::size, 21); // Activity occurrence verification timeout + fail

            state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT3", state.getName());
            assertEquals("ROOT.ELEMENT.ACT3", state.getPath().asString());
            assertEquals(121L, state.getArguments().get("ARG1"));
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
            assertEquals(ActivityReportState.FAIL, state.getProgressReports().get(state.getProgressReports().size() - 1).getStatus());
            assertEquals("Verification", state.getProgressReports().get(state.getProgressReports().size() - 1).getName());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity4() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_activities.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Create activity handler for route A and type TC
        ActivityHandlerStub h1 = ActivityHandlerStub.create()
                .withRoutes("A", "B")
                .withTypes("TC")
                .withLifecycle(new NominalLifecycleStrategy(3, 4000, 3, 6000))
                .build();

        // Register handler
        model.registerActivityHandler(h1);

        // Request activity execution: nominal
        {
            outList.clear();
            testLogger.info("Invocation 1");
            // Invoke
            ActivityRequest ar1 = ActivityRequest.newRequest(1003, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", 120L))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(15000, outList::size, 19);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT4", state.getName());
            assertEquals("ROOT.ELEMENT.ACT4", state.getPath().asString());
            assertEquals(120L, state.getArguments().get("ARG1"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());

            // Verify the two timeouts
            boolean tTimeoutFound = false;
            boolean eTimeoutFound = false;
            for(ActivityOccurrenceReport aor : state.getProgressReports()) {
                if(aor.getName().equals("Transmission Timeout")) {
                    tTimeoutFound = true;
                }
                if(aor.getName().equals("Execution Timeout")) {
                    eTimeoutFound = true;
                }
            }
            assertTrue(tTimeoutFound);
            assertTrue(eTimeoutFound);
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity1DisabledRoute() throws JAXBException, ProcessingModelException, InterruptedException {
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

        // Request activity execution: unavailable route
        {
            outList.clear();
            testLogger.info("Invocation 1");

            // Set route A as unavailable
            h1.markRouteAsUnavailable("A");

            ActivityRequest ar1 = ActivityRequest.newRequest(1000, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofEngineering("ARG4", "ON"))
                    .withRoute("A")
                    .withSource("XXX")
                    .build();

            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(5000, outList::size, 4);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT1", state.getName());
            assertEquals("ROOT.ELEMENT.ACT1", state.getPath().asString());
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity1CreationPurge() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_activities.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Create activity handler for route A and type TC
        ActivityHandlerStub h1 = ActivityHandlerStub.create().withRoutes("A", "B").withTypes("TC").build();
        // Make sure to reject invocations: the creation of an activity occurrence shall not be dispatched
        h1.setRejectInvocations(true);
        // Register handler
        model.registerActivityHandler(h1);

        // Request activity creation
        {
            outList.clear();
            testLogger.info("Invocation 1");

            ActivityRequest ar1 = ActivityRequest.newRequest(1000, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", true))
                    .withArgument(PlainActivityArgument.ofSource("ARG2", 104L))
                    .withArgument(PlainActivityArgument.ofSource("ARG3", 7.9))
                    .withArgument(PlainActivityArgument.ofSource("ARG4", 0))
                    .withRoute("C")
                    .withSource("XXX")
                    .build();

            // Null occurrence ID, not relevant as it goes together with the activity request
            ActivityProgress pr1 = ActivityProgress.of(1000, null, "Scheduled", Instant.now(), ActivityOccurrenceState.SCHEDULING, ActivityReportState.OK);

            IUniqueId id1 = model.createActivity(ar1, pr1);
            assertEquals(1L, id1.asLong());

            //
            AwaitUtil.awaitAndVerify(5000, outList::size, 2);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT1", state.getName());
            assertEquals("ROOT.ELEMENT.ACT1", state.getPath().asString());
            assertEquals(true, state.getArguments().get("ARG1"));
            assertEquals(104L, state.getArguments().get("ARG2"));
            assertEquals(7.9, state.getArguments().get("ARG3"));
            assertEquals(0, state.getArguments().get("ARG4"));
            assertEquals("C", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.SCHEDULING, state.getCurrentState());
        }

        // Request activity purge
        {
            outList.clear();
            testLogger.info("Invocation 2");

            // Invoke purge
            model.purgeActivities(Collections.singletonList(Pair.of(1000, new LongUniqueId(1))));

            //
            AwaitUtil.awaitAndVerify(5000, outList::size, 1);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT1", state.getName());
            assertEquals("ROOT.ELEMENT.ACT1", state.getPath().asString());
            assertEquals(true, state.getArguments().get("ARG1"));
            assertEquals(104L, state.getArguments().get("ARG2"));
            assertEquals(7.9, state.getArguments().get("ARG3"));
            assertEquals(0, state.getArguments().get("ARG4"));
            assertEquals("C", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

    @Test
    void testActivity4Scheduled() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_activities.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Create activity handler for route A and type TC
        ActivityHandlerStub h1 = ActivityHandlerStub.create()
                .withRoutes("A", "B")
                .withTypes("TC")
                .withLifecycle(new SchedulingLifecycleStrategy(3, 4000, 3, 1000))
                .build();

        // Register handler
        model.registerActivityHandler(h1);

        // Request activity execution: nominal
        {
            outList.clear();
            testLogger.info("Invocation 1");
            // Invoke
            ActivityRequest ar1 = ActivityRequest.newRequest(1003, SystemEntityPath.fromString("Not important"))
                    .withArgument(PlainActivityArgument.ofSource("ARG1", 120L))
                    .withProperty(SchedulingLifecycleStrategy.SCHEDULED_EXECUTION_TIME_KEY, Instant.now().plusSeconds(10).toString())
                    .withRoute("A")
                    .withSource("XXX")
                    .build();
            IUniqueId id1 = model.startActivity(ar1);
            assertEquals(1L, id1.asLong());

            AwaitUtil.awaitCondition(4000, () -> outList.size() > 0);
            assertEquals(1, model.getActiveActivityOccurrences().size());
            //
            AwaitUtil.awaitAndVerify(15000, outList::size, 20);

            // Get the final state and check what it contains
            ActivityOccurrenceData state = (ActivityOccurrenceData) outList.get(outList.size() - 1);
            assertEquals("ACT4", state.getName());
            assertEquals("ROOT.ELEMENT.ACT4", state.getPath().asString());
            assertEquals(120L, state.getArguments().get("ARG1"));
            assertEquals("A", state.getRoute());
            assertEquals("TC", state.getType());
            assertNull(state.getResult());
            assertNotNull(state.getExecutionTime());
            assertEquals(ActivityOccurrenceState.COMPLETED, state.getCurrentState());

            // Verify the two timeouts
            boolean tTimeoutFound = false;
            boolean eTimeoutFound = false;
            for(ActivityOccurrenceReport aor : state.getProgressReports()) {
                if(aor.getName().equals("Transmission Timeout")) {
                    tTimeoutFound = true;
                }
                if(aor.getName().equals("Execution Timeout")) {
                    eTimeoutFound = true;
                }
            }
            assertTrue(tTimeoutFound); // Timeout in transmission
            assertFalse(eTimeoutFound); // No timeout in execution

            assertEquals(0, model.getActiveActivityOccurrences().size());
        }

        // Deregister handler
        model.deregisterActivityHandler(h1);
    }

}