/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.BitString;
import eu.dariolucia.reatmetric.processing.IProcessingModel;
import eu.dariolucia.reatmetric.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.processing.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void testEvents() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_events.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Simple injection
        testLogger.info("Injection - Batch 1");

        EventOccurrence injectedEvent = EventOccurrence.of(2001, "Q1", new String[] { "Report", "Example" });
        model.raiseEvent(injectedEvent);

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        // Check
        assertEquals(2001, ((EventData)outList.get(0)).getExternalId());
        assertEquals("Q1", ((EventData)outList.get(0)).getQualifier());
        assertEquals("ONBOARD", ((EventData)outList.get(0)).getType());
        assertEquals(Severity.WARN, ((EventData)outList.get(0)).getSeverity());
        assertArrayEquals(new String[] { "Report", "Example" }, (String[]) ((EventData)outList.get(0)).getReport());

        // First parameter sample with value
        testLogger.info("Injection - Batch 2");
        outList.clear();

        ParameterSample paramSample = ParameterSample.of(1001, 10L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 4);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(10L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(10L, ((ParameterData) outList.get(i)).getEngValue());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) {
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item: " + outList.get(i));
            }
        }

        // Another parameter sample with same value
        testLogger.info("Injection - Batch 3");
        outList.clear();

        paramSample = ParameterSample.of(1001, 10L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 2);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(10L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(10L, ((ParameterData) outList.get(i)).getEngValue());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that brings the parameter in alarm
        testLogger.info("Injection - Batch 4");
        outList.clear();

        paramSample = ParameterSample.of(1001, 13L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 6);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(13L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(13L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) {
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2005) {
                    assertEquals(2005, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.ALARM, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that keeps the parameter in alarm
        testLogger.info("Injection - Batch 5");
        outList.clear();

        paramSample = ParameterSample.of(1001, 12L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 4);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(12L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(12L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) {
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that brings the parameter in nominal state
        testLogger.info("Injection - Batch 6");
        outList.clear();

        paramSample = ParameterSample.of(1001, 15L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 6);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(15L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(15L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.NOMINAL, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.NOMINAL, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) {
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2006) {
                    assertEquals(2006, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ANOTHER_TYPE", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that brings the parameter in alarm and triggers also event 2007
        testLogger.info("Injection - Batch 7");
        outList.clear();

        paramSample = ParameterSample.of(1001, 20L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 7);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) { // New sample
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) { // New value
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2005) { // In alarm
                    assertEquals(2005, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.ALARM, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2007) { // Condition based
                    assertEquals(2007, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.ALARM, ((EventData) outList.get(i)).getSeverity());
                    assertEquals(null, ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that keeps the parameter in alarm and should trigger again event 2007, but it does not happen
        // due to no other sample resetting the trigger condition
        testLogger.info("Injection - Batch 8");
        outList.clear();

        paramSample = ParameterSample.of(1001, 20L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 3);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) { // New sample
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that brings the parameter in nominal state
        testLogger.info("Injection - Batch 9");
        outList.clear();

        paramSample = ParameterSample.of(1001, 15L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 6);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(15L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(15L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.NOMINAL, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.NOMINAL, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) {
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) {
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2006) {
                    assertEquals(2006, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ANOTHER_TYPE", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Another parameter sample with value that brings the parameter in alarm and triggers also event 2007 (again)
        testLogger.info("Injection - Batch 10");
        outList.clear();

        paramSample = ParameterSample.of(1001, 20L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 7);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("PARAM1", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) { // New sample
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2004) { // New value
                    assertEquals(2004, ((EventData)outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData)outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData)outList.get(i)).getType());
                    assertEquals(Severity.WARN, ((EventData)outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData)outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2005) { // In alarm
                    assertEquals(2005, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.ALARM, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else if(((EventData) outList.get(i)).getExternalId() == 2007) { // Condition based
                    assertEquals(2007, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.ALARM, ((EventData) outList.get(i)).getSeverity());
                    assertEquals(null, ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Disable the new sample event
        outList.clear();
        model.disable(SystemEntityPath.fromString("ROOT.EVT.EVT3"));
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        // Another parameter sample with value that keeps the parameter in alarm and should trigger again event 2007, but it does not happen
        // due to no other sample resetting the trigger condition. 2003 is not triggered.
        testLogger.info("Injection - Batch 11");
        outList.clear();

        paramSample = ParameterSample.of(1001, 20L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 2);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else {
                fail("Unexpected data item");
            }
        }

        // Enable the new sample event
        outList.clear();
        model.enable(SystemEntityPath.fromString("ROOT.EVT.EVT3"));
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        // Another parameter sample with value that keeps the parameter in alarm and should trigger again event 2007, but it does not happen
        // due to no other sample resetting the trigger condition. 2003 is triggered again.
        testLogger.info("Injection - Batch 12");
        outList.clear();

        paramSample = ParameterSample.of(1001, 20L);

        model.injectParameters(Collections.singletonList(paramSample));

        AwaitUtil.awaitAndVerify(5000, outList::size, 3);

        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData) {
                assertEquals(1001, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(outList.get(i) instanceof EventData) {
                if(((EventData) outList.get(i)).getExternalId() == 2003) { // New sample
                    assertEquals(2003, ((EventData) outList.get(i)).getExternalId());
                    assertEquals(null, ((EventData) outList.get(i)).getQualifier());
                    assertEquals("ONGROUND", ((EventData) outList.get(i)).getType());
                    assertEquals(Severity.INFO, ((EventData) outList.get(i)).getSeverity());
                    assertEquals("ROOT.EVT.PARAM1", ((EventData) outList.get(i)).getSource());
                } else {
                    fail("Event Data ID not expected: " + ((EventData) outList.get(i)).getExternalId());
                }
            } else {
                fail("Unexpected data item");
            }
        }

        // Simple injection with inhibition of 3000 ms
        testLogger.info("Injection - Batch 13");
        outList.clear();

        injectedEvent = EventOccurrence.of(2002, "Q2", new String[] { "Report", "Example" });
        model.raiseEvent(injectedEvent);

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        // Check
        assertEquals(2002, ((EventData)outList.get(0)).getExternalId());
        assertEquals("Q2", ((EventData)outList.get(0)).getQualifier());
        assertEquals("ONBOARD", ((EventData)outList.get(0)).getType());
        assertEquals(Severity.ALARM, ((EventData)outList.get(0)).getSeverity());
        assertArrayEquals(new String[] { "Report", "Example" }, (String[]) ((EventData)outList.get(0)).getReport());

        // Inject again two parameters, 2001 and 2002
        testLogger.info("Injection - Batch 14");
        outList.clear();

        injectedEvent = EventOccurrence.of(2002, "Q2", new String[] { "Report", "Example" });
        model.raiseEvent(injectedEvent);
        AwaitUtil.await(200);
        injectedEvent = EventOccurrence.of(2001, "Q1", new String[] { "Report", "Example" });
        model.raiseEvent(injectedEvent);

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);
        // Check
        assertEquals(2001, ((EventData)outList.get(0)).getExternalId());

        // Wait a bit
        AwaitUtil.await(3000);
        // Simple injection with inhibition of 3000 ms
        testLogger.info("Injection - Batch 15");
        outList.clear();

        injectedEvent = EventOccurrence.of(2002, "Q2", new String[] { "Report", "Example" });
        model.raiseEvent(injectedEvent);

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        // Check
        assertEquals(2002, ((EventData)outList.get(0)).getExternalId());
        assertEquals("Q2", ((EventData)outList.get(0)).getQualifier());
        assertEquals("ONBOARD", ((EventData)outList.get(0)).getType());
        assertEquals(Severity.ALARM, ((EventData)outList.get(0)).getSeverity());
        assertArrayEquals(new String[] { "Report", "Example" }, (String[]) ((EventData)outList.get(0)).getReport());
    }

    // TODO: error case: event injection on one having a condition defined
    // TODO: error case: event condition with issue (return not a boolean)
    // TODO: error case: event condition with issue (missing symbol)
}