/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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
 *
 */

package eu.dariolucia.reatmetric.driver.socket.configuration.message;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.driver.socket.configuration.SocketConfiguration;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AsciiMessageDefinitionTest {

    @Test
    public void testTlmAsciiMessageHandling() throws IOException, ReatmetricException {
        Locale.setDefault(Locale.UK);
        SocketConfiguration sc = SocketConfiguration.load(this.getClass().getClassLoader().getResourceAsStream("ascii_single/ascii_single_driver.xml"));

        String decode1 = "{TLM,1,12332,53.2,20,2,3}";
        String decode2 = "{TLM,1,,53.2,20,2,3}";
        String decode3 = "{TLM,,,,,,}";
        String decode4 = "{TLM,1,12332,53.2,20,2}"; // One field less
        String decode5 = "{TLM,1,12332,53.2,20,2,3,10}"; // One field more

        AsciiMessageDefinition tlmMessage = (AsciiMessageDefinition) sc.getMessageDefinitions().stream().filter(o -> o.getId().equals("TLM")).findFirst().get();
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode1));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode1); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertEquals(1, retrieved.get("status_val"));
            assertEquals(12332L, retrieved.get("freq_val"));
            assertEquals(53.2, retrieved.get("temp_val"));
            assertEquals(20L, retrieved.get("offset_val"));
            assertEquals(2, retrieved.get("mode_val"));
            assertEquals(3, retrieved.get("sweep_val"));
        }
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode2));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode2); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertEquals(1, retrieved.get("status_val"));
            assertNull(retrieved.get("freq_val"));
            assertEquals(53.2, retrieved.get("temp_val"));
            assertEquals(20L, retrieved.get("offset_val"));
            assertEquals(2, retrieved.get("mode_val"));
            assertEquals(3, retrieved.get("sweep_val"));
        }
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode3));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode3); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertNull(retrieved.get("status_val"));
            assertNull(retrieved.get("freq_val"));
            assertNull(retrieved.get("temp_val"));
            assertNull(retrieved.get("offset_val"));
            assertNull(retrieved.get("mode_val"));
            assertNull(retrieved.get("sweep_val"));
        }
        {
            assertNull(tlmMessage.identify(decode4));
            assertThrows(ReatmetricException.class, () -> {
                tlmMessage.decode(null, decode4); // no secondary id
            });
        }
        {
            // Identification is done correctly
            assertThrows(ReatmetricException.class, () -> {
                tlmMessage.decode(null, decode5); // no secondary id
            });
        }

        Map<String, Object> encodeValueMap = new HashMap<>();
        encodeValueMap.put("status_val", 1);
        encodeValueMap.put("freq_val", 12332L);
        encodeValueMap.put("temp_val", 53.2);
        encodeValueMap.put("offset_val", 20L);
        encodeValueMap.put("mode_val", 2);
        encodeValueMap.put("sweep_val", 3);
        String result = tlmMessage.encode(null, encodeValueMap);
        assertEquals(decode1, result);

        encodeValueMap.put("freq_val", null);
        result = tlmMessage.encode(null, encodeValueMap);
        assertEquals(decode2, result);
    }

    @Test
    public void testSetAsciiMessageHandling() throws IOException, ReatmetricException {
        Locale.setDefault(Locale.UK);
        SocketConfiguration sc = SocketConfiguration.load(this.getClass().getClassLoader().getResourceAsStream("ascii_single/ascii_single_driver.xml"));

        String decode1 = "{SET,SUB1,freq_val,12321}";
        String decode2 = "{SET,SUB1,temp_val,40.1}";
        String decode3 = "{SET,SUB1,status_val,1}";

        AsciiMessageDefinition tlmMessage = (AsciiMessageDefinition) sc.getMessageDefinitions().stream().filter(o -> o.getId().equals("SET")).findFirst().get();
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode1));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode1); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertEquals("SUB1", retrieved.get("device_subsystem"));
            assertEquals("freq_val", retrieved.get("parameter"));
            assertNumberEquals(12321L, (Number) retrieved.get("new_value"));
        }
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode2));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode2); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertEquals("SUB1", retrieved.get("device_subsystem"));
            assertEquals("temp_val", retrieved.get("parameter"));
            assertNumberEquals(40.1, (Number) retrieved.get("new_value"));
        }
        {
            assertEquals(tlmMessage.getId(), tlmMessage.identify(decode3));
            Map<String, Object> retrieved = tlmMessage.decode(null, decode3); // no secondary id
            assertNotNull(retrieved);
            assertEquals(tlmMessage.getSymbols().size(), retrieved.size());
            assertEquals("SUB1", retrieved.get("device_subsystem"));
            assertEquals("status_val", retrieved.get("parameter"));
            assertNumberEquals(1, (Number) retrieved.get("new_value"));
        }

        Map<String, Object> encodeValueMap = new HashMap<>();
        encodeValueMap.put("device_subsystem", "SUB1");
        encodeValueMap.put("parameter", "status_val");
        encodeValueMap.put("new_value", 1);
        String result = tlmMessage.encode(null, encodeValueMap);
        assertEquals(decode3, result);

        encodeValueMap.put("parameter", "temp_val");
        encodeValueMap.put("new_value", 40.1);
        result = tlmMessage.encode(null, encodeValueMap);
        assertEquals(decode2, result);

        encodeValueMap.put("parameter", "freq_val");
        encodeValueMap.put("new_value", 12321L);
        result = tlmMessage.encode(null, encodeValueMap);
        assertEquals(decode1, result);
    }

    private static void assertNumberEquals(Number expected, Number actual) {
        if((expected != null && actual == null) || (expected == null && actual != null)) {
            assertEquals(expected, actual);
            return;
        }
        if(expected == null) {
            return;
        } else {
            if(expected instanceof Double || expected instanceof Float || actual instanceof Double || actual instanceof Float) {
                assertEquals(expected.doubleValue(), actual.doubleValue());
            } else {
                assertEquals(expected.longValue(), actual.longValue());
            }
        }
    }
}