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

    @Test
    void testWrongDefinitionsAndInjections() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // Expect failure due to wrong validity evaluation
        testLogger.info("Injection - Batch 1");
        ParameterSample wrongValidityEval = ParameterSample.of(1010, 0);
        model.injectParameters(Collections.singletonList(wrongValidityEval));

        AwaitUtil.awaitAndVerify(5000, outList::size,  1);

        assertEquals(1010, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals(0, ((ParameterData)outList.get(0)).getSourceValue());
        assertNull(((ParameterData) outList.get(0)).getEngValue());
        assertEquals(Validity.ERROR, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(0)).getAlarmState());

        // Update of synthetic parameter 1012 and 1014
        testLogger.info("Injection - Batch 2");
        outList.clear();
        ParameterSample baseParam = ParameterSample.of(1011, 10);
        model.injectParameters(Collections.singletonList(baseParam));

        AwaitUtil.awaitAndVerify(5000, outList::size,  5);

        // Base parameter
        assertEquals(1011, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals("DEPEN", ((SystemEntity)outList.get(1)).getName());
        assertEquals(10.0, ((ParameterData)outList.get(0)).getSourceValue());
        assertEquals(10.0, ((ParameterData)outList.get(0)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.NOT_CHECKED, ((ParameterData)outList.get(0)).getAlarmState());
        // Syntethic parameter
        assertEquals(1012, ((ParameterData)outList.get(2)).getExternalId());
        assertEquals("SYNTH_SAMPLE", ((SystemEntity)outList.get(3)).getName());
        assertEquals(20.0, ((ParameterData)outList.get(2)).getSourceValue());
        assertEquals(20.0, ((ParameterData)outList.get(2)).getEngValue());
        assertEquals(Validity.VALID, ((ParameterData)outList.get(2)).getValidity());
        assertEquals(AlarmState.NOT_CHECKED, ((ParameterData)outList.get(2)).getAlarmState());
        // Wrong syntethic parameter
        assertEquals(1014, ((ParameterData)outList.get(4)).getExternalId());
        assertNull(((ParameterData) outList.get(4)).getSourceValue());
        assertNull(((ParameterData) outList.get(4)).getEngValue());
        assertEquals(Validity.INVALID, ((ParameterData)outList.get(4)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(4)).getAlarmState());

        // Injection of a sample to a synthetic parameter - no output
        testLogger.info("Injection - Batch 3");
        outList.clear();

        ParameterSample wrongInjDueToSynthExp = ParameterSample.of(1012, 0);
        model.injectParameters(Collections.singletonList(wrongInjDueToSynthExp));

        // Expect failure
        AwaitUtil.await(5000);
        assertEquals(0, outList.size());

        // Update of parameter with wrong validity expression
        testLogger.info("Injection - Batch 4");
        ParameterSample wrongValExp = ParameterSample.of(1013, 10);
        model.injectParameters(Collections.singletonList(wrongValExp));

        AwaitUtil.awaitAndVerify(5000, outList::size,  1);

        // Parameter
        assertEquals(1013, ((ParameterData)outList.get(0)).getExternalId());
        assertEquals(10, ((ParameterData)outList.get(0)).getSourceValue());
        assertNull(((ParameterData) outList.get(0)).getEngValue());
        assertEquals(Validity.ERROR, ((ParameterData)outList.get(0)).getValidity());
        assertEquals(AlarmState.UNKNOWN, ((ParameterData)outList.get(0)).getAlarmState());
    }

    @Test
    void testSourceTypes() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // All types
        int[] ids = { 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 };
        Object[] value = { true, 3, 1022322222222213L, -1234342121232L, 10.1, BitString.parse("001000101010"), new byte[] {0x00, 0x01}, "Hello", Instant.now(), Duration.ofMillis(2131) };
        Map<Integer, Object> valueMap = new HashMap<>();
        List<ParameterSample> items = new ArrayList<>(ids.length);
        for(int i = 0; i < ids.length; ++i) {
            ParameterSample ps = ParameterSample.of(ids[i], value[i]);
            valueMap.put(ps.getId(), ps.getValue());
            items.add(ps);
        }

        testLogger.info("Injection");
        model.injectParameters(items);

        AwaitUtil.awaitAndVerify(5000, outList::size,  items.size() * 2); // Also the entity state

        for(AbstractDataItem adi : outList) {
            if(adi instanceof ParameterData) {
                int id = ((ParameterData) adi).getExternalId();
                assertEquals(valueMap.get(id), (((ParameterData) adi)).getSourceValue());
                assertEquals(valueMap.get(id), ((ParameterData) adi).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) adi).getValidity());
                assertEquals(AlarmState.NOT_CHECKED, ((ParameterData) adi).getAlarmState());
            }
        }
    }

    // TODO: test the validity matcher with value and with parameter
    // TODO: test event including inhibition
    // TODO: test expression with binding property
    // TODO: test event raised by parameter triggers
}