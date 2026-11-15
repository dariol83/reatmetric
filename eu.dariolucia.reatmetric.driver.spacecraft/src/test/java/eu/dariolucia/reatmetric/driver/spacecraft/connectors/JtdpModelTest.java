/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JtdpModelTest {

    @Test
    void sampleModelLoadsAndDefinesEveryMonitoringParameterAndEvent() throws Exception {
        // The processing implementation is a runtime dependency of core, not a module dependency of the driver.
        // Invoke its public loader reflectively to validate the shipped model without changing the driver API.
        Class<?> definitionType = Class.forName("eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition");
        Object definition;
        try (InputStream input = JtdpModelTest.class.getResourceAsStream("/jtdp_model.xml")) {
            assertNotNull(input, "The sample processing model must be packaged with the connector");
            definition = definitionType.getMethod("load", InputStream.class).invoke(null, input);
        }
        assertEquals(List.of(), property(definition, "validate"));

        Map<String, Object> parameters = byName((List<?>) property(definition, "getParameterDefinitions"));
        Map<String, ValueTypeEnum> expectedRawTypes = Map.ofEntries(
                Map.entry("CARRIER_LOCK", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("SYMBOL_LOCK", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("FRAME_LOCK", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("SNR_AVAILABLE", ValueTypeEnum.BOOLEAN),
                Map.entry("CENTER_FREQUENCY", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("CARRIER_OFFSET", ValueTypeEnum.SIGNED_INTEGER),
                Map.entry("SNR", ValueTypeEnum.REAL),
                Map.entry("TOTAL_FRAMES", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("ERROR_FRAMES", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("GOOD_FRAMES", ValueTypeEnum.UNSIGNED_INTEGER),
                Map.entry("DISTRIBUTED_FRAMES", ValueTypeEnum.UNSIGNED_INTEGER));
        assertEquals(expectedRawTypes.keySet(), parameters.keySet());
        for (Map.Entry<String, ValueTypeEnum> expected : expectedRawTypes.entrySet()) {
            assertEquals(expected.getValue(), property(parameters.get(expected.getKey()), "getRawType"), expected.getKey());
        }
        for (String name : List.of("CARRIER_LOCK", "SYMBOL_LOCK", "FRAME_LOCK")) {
            Object parameter = parameters.get(name);
            assertEquals(ValueTypeEnum.CHARACTER_STRING, property(parameter, "getEngineeringType"));
            Object calibration = ((List<?>) property(parameter, "getCalibrations")).get(0);
            for (int state = 0; state < 4; ++state) {
                assertEquals(List.of("SEARCHING", "LOCKED", "UNLOCKED", "UNKNOWN").get(state),
                        calibration.getClass().getMethod("calibrate", Object.class, IBindingResolver.class, ValueTypeEnum.class)
                                .invoke(calibration, (long) state, null, ValueTypeEnum.CHARACTER_STRING));
            }
        }

        Map<String, Object> events = byName((List<?>) property(definition, "getEventDefinitions"));
        assertEquals(Set.of("DECODER_STARTED", "DECODER_STOPPED", "CARRIER_STATE_CHANGED", "SYMBOL_STATE_CHANGED",
                "FRAME_STATE_CHANGED", "DECODER_ERROR", "DISTRIBUTION_OVERRUN", "DATA_DISCARDED"), events.keySet());
        assertEquals(Severity.ALARM, property(events.get("DECODER_ERROR"), "getSeverity"));
        assertEquals(Severity.WARN, property(events.get("DISTRIBUTION_OVERRUN"), "getSeverity"));
        assertEquals(Severity.WARN, property(events.get("DATA_DISCARDED"), "getSeverity"));
        for (Object event : events.values()) {
            assertEquals("JTDP", property(event, "getType"));
            assertEquals(true, property(event, "isLogEnabled"));
        }

        Set<Object> entityIds = new HashSet<>();
        for (Object parameter : parameters.values()) {
            assertTrue(entityIds.add(property(parameter, "getId")));
        }
        for (Object event : events.values()) {
            assertTrue(entityIds.add(property(event, "getId")));
        }
    }

    private static Map<String, Object> byName(List<?> definitions) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object definition : definitions) {
            String location = (String) property(definition, "getLocation");
            assertTrue(location.startsWith("SPACE.JTDP.ANTENNA1."), location);
            assertNull(result.put(location.substring("SPACE.JTDP.ANTENNA1.".length()), definition), location);
        }
        return result;
    }

    private static Object property(Object target, String method) throws Exception {
        return target.getClass().getMethod(method).invoke(target);
    }
}
