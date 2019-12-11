/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
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
import eu.dariolucia.reatmetric.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventTest {

    @Test
    void testEvents() throws JAXBException, ProcessingModelException, InterruptedException {
        // TODO
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_events.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        ParameterSample batteryState = ParameterSample.of(1001, true);
        ParameterSample batteryTension = ParameterSample.of(1002, 1000L);
        ParameterSample batteryCurrent = ParameterSample.of(1003, 1L);

        testLogger.info("Injection - Batch 1");
        model.injectParameters(Arrays.asList(batteryCurrent, batteryState, batteryTension));

        // First compilation of the expressions can take time
        AwaitUtil.awaitAndVerify(10000, outList::size, 9);

        // Battery state
        assertEquals(1001, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("BATTERY_STATE", ((SystemEntity)outList.get(1)).getName());
        assertEquals(true, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(true, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(0)).getAlarmState());
        // Battery tension
        assertEquals(1002, ((ParameterData)outList.get(2)).getExternalId());
        assertEquals("BATTERY_TENSION", ((SystemEntity)outList.get(3)).getName());
        assertEquals(1000L, ((ParameterData)outList.get(2)).getSourceValue());
        assertEquals(2010L, ((ParameterData)outList.get(2)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(2)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(2)).getAlarmState());
        // Battery current
        assertEquals(1003, ((ParameterData)outList.get(4)).getExternalId());
        assertEquals("BATTERY_CURRENT", ((SystemEntity)outList.get(5)).getName());
        assertEquals(1L, ((ParameterData)outList.get(4)).getSourceValue());
        assertEquals(7.7, ((ParameterData)outList.get(4)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(4)).getValidity());
        assertEquals(AlarmState.ALARM, ((ParameterData)outList.get(4)).getAlarmState());
        assertEquals(AlarmState.ALARM, ((AlarmParameterData)outList.get(6)).getCurrentAlarmState());
        // Battery state (enumeration)
        assertEquals(1004, ((ParameterData)outList.get(7)).getExternalId());
        assertEquals("BATTERY_STATE_EN", ((SystemEntity)outList.get(8)).getName());
        assertEquals(1, ((ParameterData)outList.get(7)).getSourceValue());
        assertEquals("ON", ((ParameterData)outList.get(7)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(7)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(7)).getAlarmState());

        testLogger.info("Injection - Batch 2");
        outList.clear();

        batteryState = ParameterSample.of(1001, false);
        batteryTension = ParameterSample.of(1002, 300L);
        batteryCurrent = ParameterSample.of(1003, 2L);

        model.injectParameters(Arrays.asList(batteryCurrent, batteryState, batteryTension));

        AwaitUtil.awaitAndVerify(5000, outList::size, 10);

        // Battery state
        assertEquals(1001, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("BATTERY_STATE", ((SystemEntity)outList.get(1)).getName());
        assertEquals(false, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(false, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.ALARM, ((ParameterData)outList.get(0)).getAlarmState());
        assertEquals(AlarmState.ALARM, ((AlarmParameterData)outList.get(2)).getCurrentAlarmState());
        assertEquals(true, ((AlarmParameterData)outList.get(2)).getLastNominalValue());
        assertEquals(false, ((AlarmParameterData)outList.get(2)).getCurrentValue());
        // Battery tension
        assertEquals(1002, ((ParameterData)outList.get(3)).getExternalId());
        assertEquals("BATTERY_TENSION", ((SystemEntity)outList.get(4)).getName());
        assertEquals(300L, ((ParameterData)outList.get(3)).getSourceValue());
        assertNull(((ParameterData) outList.get(3)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(3)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(3)).getAlarmState());
        // Battery current
        assertEquals(1003, ((ParameterData)outList.get(5)).getExternalId());
        assertEquals("BATTERY_CURRENT", ((SystemEntity)outList.get(6)).getName());
        assertEquals(2L, ((ParameterData)outList.get(5)).getSourceValue());
        assertNull(((ParameterData) outList.get(5)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(5)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(5)).getAlarmState());
        // Battery state (enumeration)
        assertEquals(1004, ((ParameterData)outList.get(7)).getExternalId());
        assertEquals("BATTERY_STATE_EN", ((SystemEntity)outList.get(8)).getName());
        assertEquals(0, ((ParameterData)outList.get(7)).getSourceValue());
        assertEquals("OFF", ((ParameterData)outList.get(7)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(7)).getValidity());
        assertEquals(AlarmState.ALARM, ((ParameterData)outList.get(7)).getAlarmState());
        assertEquals(AlarmState.ALARM, ((AlarmParameterData)outList.get(9)).getCurrentAlarmState());
        assertEquals("ON", ((AlarmParameterData)outList.get(9)).getLastNominalValue());

        testLogger.info("Injection - Batch 3");
        outList.clear();

        batteryState = ParameterSample.of(1001, true);

        model.injectParameters(Collections.singletonList(batteryState));

        AwaitUtil.awaitAndVerify(5000, outList::size, 10);

        // Battery state
        assertEquals(1001, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("BATTERY_STATE", ((SystemEntity)outList.get(1)).getName());
        assertEquals(true, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(true, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(0)).getAlarmState());
        assertEquals(AlarmState.NOMINAL, ((AlarmParameterData)outList.get(2)).getCurrentAlarmState());
        assertEquals(true, ((AlarmParameterData)outList.get(2)).getLastNominalValue());
        assertEquals(true, ((AlarmParameterData)outList.get(2)).getCurrentValue());
        // Battery tension
        assertEquals(1002, ((ParameterData)outList.get(3)).getExternalId());
        assertEquals("BATTERY_TENSION", ((SystemEntity)outList.get(4)).getName());
        assertEquals(300L, ((ParameterData)outList.get(3)).getSourceValue());
        assertEquals(610L, ((ParameterData)outList.get(3)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(3)).getValidity());
        assertEquals(AlarmState.WARNING, ((ParameterData)outList.get(3)).getAlarmState());
        assertEquals(AlarmState.WARNING, ((AlarmParameterData)outList.get(5)).getCurrentAlarmState());
        assertEquals(2010L, ((AlarmParameterData)outList.get(5)).getLastNominalValue());
        // Battery current
        assertEquals(1003, ((ParameterData)outList.get(6)).getExternalId());
        assertEquals(2L, ((ParameterData)outList.get(6)).getSourceValue());
        assertNull(((ParameterData) outList.get(6)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(6)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(6)).getAlarmState());
        // Battery state (enumeration)
        assertEquals(1004, ((ParameterData)outList.get(7)).getExternalId());
        assertEquals("BATTERY_STATE_EN", ((SystemEntity)outList.get(8)).getName());
        assertEquals(1, ((ParameterData)outList.get(7)).getSourceValue());
        assertEquals("ON", ((ParameterData)outList.get(7)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(7)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(7)).getAlarmState());
        assertEquals(AlarmState.NOMINAL, ((AlarmParameterData)outList.get(9)).getCurrentAlarmState());

        testLogger.info("Injection - Batch 4");
        outList.clear();

        batteryTension = ParameterSample.of(1002, 500);
        batteryCurrent = ParameterSample.of(1003, 0L);

        model.injectParameters(Arrays.asList(batteryCurrent, batteryTension));

        AwaitUtil.awaitAndVerify(5000, outList::size, 6);

        // Battery tension
        assertEquals(1002, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("BATTERY_TENSION", ((SystemEntity)outList.get(1)).getName());
        assertEquals(500L, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(1010L, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(0)).getAlarmState());
        assertEquals(AlarmState.NOMINAL, ((AlarmParameterData)outList.get(2)).getCurrentAlarmState());
        // Battery current
        assertEquals(1003, ((ParameterData)outList.get(3)).getExternalId());
        assertEquals("BATTERY_CURRENT", ((SystemEntity)outList.get(4)).getName());
        assertEquals(0L, ((ParameterData)outList.get(3)).getSourceValue());
        assertEquals(3.3666, (Double) ((ParameterData)outList.get(3)).getEngValue(), 0.001);
        assertEquals(Validity.VALID, ((ParameterData)outList.get(3)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(3)).getAlarmState());
        assertEquals(AlarmState.NOMINAL, ((AlarmParameterData)outList.get(5)).getCurrentAlarmState()); // ALARM - Validity INVALID - Validity INVALID - NOMINAL

        testLogger.info("Injection - Disable battery");
        outList.clear();

        model.disable(SystemEntityPath.fromString("ROOT.BATTERY"));

        AwaitUtil.awaitAndVerify(5000, () -> outList.size() == 5);

        for(AbstractDataItem i : outList) {
            assertEquals(Status.DISABLED, ((SystemEntity)i).getStatus());
        }

        testLogger.info("Injection - Enable battery");
        outList.clear();

        model.enable(SystemEntityPath.fromString("ROOT.BATTERY"));

        AwaitUtil.awaitAndVerify(5000, () -> outList.size() == 9);

        for(AbstractDataItem i : outList) {
            if(i instanceof SystemEntity) {
                assertEquals(Status.ENABLED, ((SystemEntity) i).getStatus());
            }
        }
    }

    // TODO: test event including inhibition
    // TODO: test event raised by parameter triggers
}