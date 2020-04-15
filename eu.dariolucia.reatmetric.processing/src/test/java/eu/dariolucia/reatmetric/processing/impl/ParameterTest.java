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

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.value.BitString;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ParameterTest {

    @BeforeEach
    void setup() {
        Logger packageLogger = Logger.getLogger("eu.dariolucia.reatmetric.processing");
        packageLogger.setLevel(Level.ALL);
        Arrays.stream(Logger.getLogger("").getHandlers()).forEach(o -> o.setLevel(Level.ALL));
    }

    @Test
    void testBasicCapabilities() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
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
        assertEquals(AlarmState.VIOLATED, ((ParameterData)outList.get(3)).getAlarmState());
        assertEquals(AlarmState.VIOLATED, ((AlarmParameterData)outList.get(5)).getCurrentAlarmState());

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

        AwaitUtil.await(5000);
        assertEquals(9, outList.size());
        for(AbstractDataItem i : outList) {
            if(i instanceof SystemEntity) {
                assertEquals(Status.ENABLED, ((SystemEntity) i).getStatus());
            }
        }
    }

    @Test
    void testWrongDefinitionsAndInjections() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
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
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        // All types
        int[] ids = { 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 };
        Object[] value = { true, 3, 1022322222222213L, -1234342121232L, 10.1, BitString.parse("_001000101010"), new byte[] {0x00, 0x01}, "Hello", Instant.now(), Duration.ofMillis(2131) };
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

    @Test
    void testValidityMatchers() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");
        ParameterSample b = ParameterSample.of(1020, 20);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 8);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1020) {
                assertEquals(1020, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.NOMINAL, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE", ((SystemEntity) outList.get(i)).getName());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1021) {
                assertEquals(1021, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("VALMATCH1", ((SystemEntity) outList.get(i)).getName());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1022) {
                assertEquals(1022, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1023) {
                assertEquals(1023, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity());
            } else {
                assertEquals(1024, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("VALMATCH4", ((SystemEntity) outList.get(i)).getName());
            }
        }

        testLogger.info("Injection - Batch 2");
        outList.clear();
        b = ParameterSample.of(1020, 25);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 9);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1020) {
                assertEquals(1020, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE", ((SystemEntity) outList.get(i)).getName()); // Nominal -> Alarm
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1021) {
                assertEquals(1021, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("VALMATCH1", ((SystemEntity) outList.get(i)).getName()); // Not checked -> Unknown
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1022) {
                assertEquals(1022, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("VALMATCH2", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1023) {
                assertEquals(1023, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity());
            } else {
                assertEquals(1024, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
            }
        }
        testLogger.info("Injection - Batch 3");
        outList.clear();
        Instant current = Instant.ofEpochMilli(500000);
        Instant next = current.plusMillis(30000);
        b = ParameterSample.of(1025, current);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 4);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1025) {
                assertEquals(1025, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.NOT_CHECKED, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE2", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1027) {
                assertEquals(1027, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.ERROR, ((ParameterData) outList.get(i)).getValidity()); // The other value is not set
            } else {
                assertEquals(1028, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity()); // The other value is not set, but equality test with null is OK
            }
        }

        testLogger.info("Injection - Batch 4");
        outList.clear();
        b = ParameterSample.of(1026, current);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 5);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1026) {
                assertEquals(1026, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.NOT_CHECKED, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE3", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Nominal
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1027) {
                assertEquals(1027, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity());
            } else {
                assertEquals(1028, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity()); // The other value is not set
                ++i;
                assertEquals("VALMATCH6", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            }
        }

        testLogger.info("Injection - Batch 5");
        outList.clear();
        b = ParameterSample.of(1026, next);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 5);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1026) {
                assertEquals(1026, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.NOT_CHECKED, ((ParameterData) outList.get(i)).getAlarmState());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1027) {
                assertEquals(1027, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("VALMATCH5", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            } else {
                assertEquals(1028, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(Validity.INVALID, ((ParameterData) outList.get(i)).getValidity()); // The other value is not set
                ++i;
                assertEquals("VALMATCH6", ((SystemEntity) outList.get(i)).getName()); // Not checked -> Unknown
            }
        }
    }

    @Test
    void testExpressionPropertyBindings() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");
        ParameterSample b = ParameterSample.of(1030, 10);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 8);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1030) {
                assertEquals(1030, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(10L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.NOMINAL, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE", ((SystemEntity) outList.get(i)).getName());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1031) {
                assertEquals(1031, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(-20L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(-20L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("EXP1", ((SystemEntity) outList.get(i)).getName());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1032) {
                assertEquals(1032, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(40L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(40L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("EXP2", ((SystemEntity) outList.get(i)).getName());
            } else {
                assertEquals(1033, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals("ALL OK", ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals("ALL OK", ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                ++i;
                assertEquals("EXP3", ((SystemEntity) outList.get(i)).getName());
            }
        }

        testLogger.info("Injection - Batch 2");
        outList.clear();
        b = ParameterSample.of(1030, 30);
        model.injectParameters(Collections.singletonList(b));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 6);

        // Checks
        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1030) {
                assertEquals(1030, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(30L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(60L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
                assertEquals(AlarmState.ALARM, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("BASE", ((SystemEntity) outList.get(i)).getName());
                ++i;
                assertEquals(AlarmState.ALARM, ((AlarmParameterData) outList.get(i)).getCurrentAlarmState());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1031) {
                assertEquals(1031, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(100L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(100L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
            } else if(((ParameterData) outList.get(i)).getExternalId() == 1032) {
                assertEquals(1032, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(120L, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(120L, ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
            } else {
                assertEquals(1033, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals("IN ALARM", ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals("IN ALARM", ((ParameterData) outList.get(i)).getEngValue());
                assertEquals(Validity.VALID, ((ParameterData) outList.get(i)).getValidity());
            }
        }
    }

    @Test
    void testLogXYCalibrations() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");

        ParameterSample logSample = ParameterSample.of(1041, 3.2);
        ParameterSample xySample = ParameterSample.of(1042, 7);
        model.injectParameters(Arrays.asList(logSample, xySample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 4);

        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1041) {
                assertEquals(1041, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(3.2, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(0.13649, (Double) ((ParameterData) outList.get(i)).getEngValue(), 0.00001);
                ++i;
                assertEquals("LOGPARAM", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            } else {
                assertEquals(1042, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(7.0, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(7.0, ((ParameterData) outList.get(i)).getEngValue());
                ++i;
                assertEquals("XYPARAM", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            }
        }

        testLogger.info("Injection - Batch 2");
        outList.clear();
        xySample = ParameterSample.of(1042, -2);
        model.injectParameters(Collections.singletonList(xySample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        for (AbstractDataItem dataItem : outList) {
            if (((ParameterData) dataItem).getExternalId() == 1042) {
                assertEquals(1042, ((ParameterData) dataItem).getExternalId());
                assertEquals(-2.0, ((ParameterData) dataItem).getSourceValue());
                assertEquals(-2.0, ((ParameterData) dataItem).getEngValue());
            } else {
                fail("Expected 1042");
            }
        }

        testLogger.info("Injection - Batch 3");
        outList.clear();
        xySample = ParameterSample.of(1042, 13);
        model.injectParameters(Collections.singletonList(xySample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 1);

        for (AbstractDataItem abstractDataItem : outList) {
            if (((ParameterData) abstractDataItem).getExternalId() == 1042) {
                assertEquals(1042, ((ParameterData) abstractDataItem).getExternalId());
                assertEquals(13.0, ((ParameterData) abstractDataItem).getSourceValue());
                assertEquals(16.0, ((ParameterData) abstractDataItem).getEngValue());
            } else {
                fail("Expected 1042");
            }
        }
    }

    @Test
    void testConditionalCalibrations() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");

        Instant current = Instant.ofEpochMilli(500000);
        Instant next = current.plusMillis(30000);
        ParameterSample b = ParameterSample.of(1065, current);
        ParameterSample c = ParameterSample.of(1066, next);
        model.injectParameters(Arrays.asList(b, c));
        // 6 updates out
        ParameterSample paramSample = ParameterSample.of(1061, 3.2);
        // 1 out
        model.injectParameters(Collections.singletonList(paramSample));

        // Expect 7 updates
        AwaitUtil.awaitAndVerify(5000, outList::size, 7);

        boolean firstEntry = true;
        for(int i = 0; i < outList.size(); ++i) {
            if(outList.get(i) instanceof ParameterData && ((ParameterData) outList.get(i)).getExternalId() == 1061) {
                if(firstEntry) {
                    assertNull(((ParameterData) outList.get(i)).getSourceValue());
                    assertNull(((ParameterData) outList.get(i)).getEngValue());
                    firstEntry = false;
                } else {
                    assertEquals(1061, ((ParameterData) outList.get(i)).getExternalId());
                    assertEquals(3.2, ((ParameterData) outList.get(i)).getSourceValue());
                    assertEquals(0.13649, (Double) ((ParameterData) outList.get(i)).getEngValue(), 0.00001);
                }
            }
        }

        testLogger.info("Injection - Batch 2");
        outList.clear();
        c = ParameterSample.of(1066, next.minusMillis(700000));
        model.injectParameters(Collections.singletonList(c));
        // Expect 2 updates out
        AwaitUtil.awaitAndVerify(5000, outList::size, 2);

        for (AbstractDataItem dataItem : outList) {
            if (dataItem instanceof ParameterData && ((ParameterData) dataItem).getExternalId() == 1061) {
                assertEquals(1061, ((ParameterData) dataItem).getExternalId());
                assertEquals(3.2, ((ParameterData) dataItem).getSourceValue());
                assertEquals(3.2, (Double) ((ParameterData) dataItem).getEngValue(), 0.00001);
            }
        }
    }

    @Test
    void testOldGenerationTime() throws JAXBException, ProcessingModelException, InterruptedException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");

        Instant toInject = Instant.ofEpochMilli(3000000);
        ParameterSample logSample = ParameterSample.of(1041, toInject, Instant.now(), 3.2);
        model.injectParameters(Collections.singletonList(logSample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 2);

        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1041) {
                assertEquals(1041, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(3.2, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(0.13649, (Double) ((ParameterData) outList.get(i)).getEngValue(), 0.00001);
                ++i;
                assertEquals("LOGPARAM", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Not checked
            } else {
                fail("Expected 1041");
            }
        }

        testLogger.info("Injection - Batch 2");
        outList.clear();
        toInject = Instant.ofEpochMilli(3000000 - 1);
        logSample = ParameterSample.of(1041, toInject, Instant.now(), 400);
        model.injectParameters(Collections.singletonList(logSample));

        //
        AwaitUtil.await(5000);
        assertEquals(0, outList.size());
    }

    @Test
    void testExternal() throws InterruptedException, ProcessingModelException, JAXBException {
        Logger testLogger = Logger.getLogger(getClass().getName());
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_parameters.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        testLogger.info("Injection - Batch 1");

        Instant toInject = Instant.ofEpochMilli(3000000);
        ParameterSample logSample = ParameterSample.of(1051, toInject, Instant.now(), 3.2);
        model.injectParameters(Collections.singletonList(logSample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 2);

        for(int i = 0; i < outList.size(); ++i) {
            if(((ParameterData) outList.get(i)).getExternalId() == 1051) {
                assertEquals(1051, ((ParameterData) outList.get(i)).getExternalId());
                assertEquals(3.2, ((ParameterData) outList.get(i)).getSourceValue());
                assertEquals(3.2, (Double) ((ParameterData) outList.get(i)).getEngValue(), 0.00001);
                assertEquals(AlarmState.NOMINAL, ((ParameterData) outList.get(i)).getAlarmState());
                ++i;
                assertEquals("EXTPARAM", ((SystemEntity) outList.get(i)).getName()); // Unknown -> Nominal
            } else {
                fail("Expected 1051");
            }
        }
    }
}