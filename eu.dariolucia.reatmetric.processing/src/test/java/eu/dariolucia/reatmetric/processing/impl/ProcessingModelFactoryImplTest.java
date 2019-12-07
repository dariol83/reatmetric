/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessingModelFactoryImplTest {

    @Test
    void testBatteryModel() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
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

        Thread.sleep(5000);

        assertEquals(8, outList.size());
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
        // Battery state (enumeration)
        assertEquals(1004, ((ParameterData)outList.get(6)).getExternalId());
        assertEquals("BATTERY_STATE_EN", ((SystemEntity)outList.get(7)).getName());
        assertEquals(1, ((ParameterData)outList.get(6)).getSourceValue());
        assertEquals("ON", ((ParameterData)outList.get(6)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(6)).getValidity());
        assertEquals(AlarmState.NOMINAL, ((ParameterData)outList.get(6)).getAlarmState());

        testLogger.info("Injection - Batch 2");
        outList.clear();

        batteryState = ParameterSample.of(1001, false);
        batteryTension = ParameterSample.of(1002, 300L);
        batteryCurrent = ParameterSample.of(1003, 2L);

        model.injectParameters(Arrays.asList(batteryCurrent, batteryState, batteryTension));

        Thread.sleep(5000);

        assertEquals(8, outList.size());
        // Battery state
        assertEquals(1001, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("BATTERY_STATE", ((SystemEntity)outList.get(1)).getName());
        assertEquals(false, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(false, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.ALARM, ((ParameterData)outList.get(0)).getAlarmState());
        // Battery tension
        assertEquals(1002, ((ParameterData)outList.get(2)).getExternalId());
        assertEquals("BATTERY_TENSION", ((SystemEntity)outList.get(3)).getName());
        assertEquals(300L, ((ParameterData)outList.get(2)).getSourceValue());
        assertEquals(null, ((ParameterData)outList.get(2)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(2)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(2)).getAlarmState());
        // Battery current
        assertEquals(1003, ((ParameterData)outList.get(4)).getExternalId());
        assertEquals("BATTERY_CURRENT", ((SystemEntity)outList.get(5)).getName());
        assertEquals(2L, ((ParameterData)outList.get(4)).getSourceValue());
        assertEquals(null, ((ParameterData)outList.get(4)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(4)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(4)).getAlarmState());
        // Battery state (enumeration)
        assertEquals(1004, ((ParameterData)outList.get(6)).getExternalId());
        assertEquals("BATTERY_STATE_EN", ((SystemEntity)outList.get(7)).getName());
        assertEquals(0, ((ParameterData)outList.get(6)).getSourceValue());
        assertEquals("OFF", ((ParameterData)outList.get(6)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(6)).getValidity());
        assertEquals(AlarmState.ALARM, ((ParameterData)outList.get(6)).getAlarmState());
    }

}