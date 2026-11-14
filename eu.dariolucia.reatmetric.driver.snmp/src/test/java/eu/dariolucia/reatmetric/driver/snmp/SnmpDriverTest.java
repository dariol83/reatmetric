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

package eu.dariolucia.reatmetric.driver.snmp;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import org.junit.jupiter.api.Test;
import org.snmp4j.PDU;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SnmpDriverTest {

    private static final String HANDLER = "test-snmp";
    // Independent BER fixture: RESPONSE, request 42, success, description "router", integer 12345.
    private static final byte[] ARCHIVED_RESPONSE = HexFormat.of().parseHex(
            "A22F02012A0201000201003024"
                    + "301206082B060102010101000406726F75746572"
                    + "300E06082B0601020101030002023039");

    private final SnmpDriver driver = new SnmpDriver() {
        @Override
        public String getHandler() {
            return HANDLER;
        }
    };

    @Test
    void rendersArchivedBerResponseAndPreservesVariableOrder() throws Exception {
        Map<String, String> rendered = driver.render(rawData(ARCHIVED_RESPONSE));

        assertEquals(Map.of(
                "Message Type", String.valueOf(PDU.RESPONSE),
                "Error Status", "Success", "Request ID", "42",
                "OID #0", "1.3.6.1.2.1.1.1.0", "Value #0", "router", "Type #0", "OCTET STRING",
                "OID #1", "1.3.6.1.2.1.1.3.0", "Value #1", "12345", "Type #1", "Integer32"), rendered);
        assertEquals(List.of("Message Type", "Error Status", "Request ID",
                        "OID #0", "Value #0", "Type #0", "OID #1", "Value #1", "Type #1"),
                List.copyOf(rendered.keySet()));
    }

    @Test
    void rendersCachedPduWithoutRequiringEncodedContents() throws Exception {
        RawData rawData = rawData(null);
        PDU response = new PDU();
        response.setType(PDU.RESPONSE);
        response.setRequestID(new Integer32(123));
        response.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0"), new OctetString("cached")));
        rawData.setData(response);

        assertEquals(Map.of("Message Type", String.valueOf(PDU.RESPONSE),
                "Error Status", "Success", "Request ID", "123",
                "OID #0", "1.3.6.1.2.1.1.1.0", "Value #0", "cached", "Type #0", "OCTET STRING"),
                driver.render(rawData));
    }

    @Test
    void rendersErrorStatusFromEncodedResponse() throws Exception {
        PDU response = new PDU();
        response.setType(PDU.RESPONSE);
        response.setRequestID(new Integer32(7));
        response.setErrorStatus(PDU.noAccess);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        response.encodeBER(encoded);

        assertEquals(Map.of("Message Type", String.valueOf(PDU.RESPONSE),
                "Error Status", "No access", "Request ID", "7"), driver.render(rawData(encoded.toByteArray())));
    }

    @Test
    void returnsEmptyRenderingForTruncatedBer() throws Exception {
        assertTrue(driver.render(rawData(new byte[]{(byte) 0xA2, 0x2F, 0x02})).isEmpty());
    }

    private static RawData rawData(byte[] contents) {
        Instant time = Instant.parse("2026-01-02T03:04:05Z");
        return new RawData(new LongUniqueId(1), time, "Status", SnmpDriver.SNMP_MESSAGE_TYPE,
                "test-device", "test-device", Quality.GOOD, null, contents, time, HANDLER, null);
    }
}
