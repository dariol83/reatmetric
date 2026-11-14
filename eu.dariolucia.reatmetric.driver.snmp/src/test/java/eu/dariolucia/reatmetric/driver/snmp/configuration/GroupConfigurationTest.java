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

package eu.dariolucia.reatmetric.driver.snmp.configuration;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GroupConfigurationTest {

    private static final Instant GENERATION_TIME = Instant.parse("2026-01-02T03:04:05Z");
    private static final String OID_PREFIX = "1.3.6.1.4.1.50000.";

    @Test
    void preparesGetRequestInConfiguredOrder() {
        GroupConfiguration group = group(
                entry(3, "Temperature", OidEntryType.DOUBLE),
                entry(1, "Status", OidEntryType.INTEGER));

        PDU request = group.preparePollRequest();

        assertEquals(PDU.GET, request.getType());
        assertEquals(List.of(new OID(OID_PREFIX + "3"), new OID(OID_PREFIX + "1")),
                request.getVariableBindings().stream().map(VariableBinding::getOid).toList());
        assertTrue(request.getVariableBindings().stream().allMatch(vb -> vb.getVariable() instanceof Null));
    }

    @Test
    void mapsBerResponseToAllConfiguredValueTypesAndSampleMetadata() throws Exception {
        GroupConfiguration group = group(
                entry(1, "Description", OidEntryType.STRING),
                entry(2, "Status", OidEntryType.INTEGER),
                entry(3, "Counter", OidEntryType.LONG),
                entry(4, "Temperature", OidEntryType.DOUBLE),
                entry(5, "Payload", OidEntryType.BYTE_ARRAY),
                entry(6, "Mode", OidEntryType.OID));
        group.initialise("TEST.Device", processingModel(Map.of(
                "TEST.Device.Description", 201, "TEST.Device.Status", 202,
                "TEST.Device.Counter", 203, "TEST.Device.Temperature", 204,
                "TEST.Device.Payload", 205, "TEST.Device.Mode", 206)));
        byte[] payload = {0, 1, (byte) 0x80, (byte) 0xFF};
        // The response deliberately uses a different order from the configuration.
        PDU response = response(
                binding(2, new Integer32(-42)),
                binding(6, new OID("1.3.6.1.4.1.50000.3")),
                binding(1, new OctetString("Nominal")),
                binding(5, new OctetString(payload)),
                binding(3, new Counter64(4_294_967_296L)),
                binding(4, new OctetString("-12.75")));

        List<ParameterSample> samples = group.mapResponse(device(), berEvent(group, response), GENERATION_TIME);

        assertEquals(List.of(202, 206, 201, 205, 203, 204), samples.stream().map(ParameterSample::getId).toList());
        Map<Integer, ParameterSample> byId = samples.stream()
                .collect(Collectors.toMap(ParameterSample::getId, Function.identity()));
        assertEquals("Nominal", byId.get(201).getValue());
        assertEquals(-42, byId.get(202).getValue());
        assertEquals(4_294_967_296L, byId.get(203).getValue());
        assertEquals(-12.75, byId.get(204).getValue());
        assertArrayEquals(payload, (byte[]) byId.get(205).getValue());
        assertEquals(3, byId.get(206).getValue());
        for (ParameterSample sample : samples) {
            assertEquals(GENERATION_TIME, sample.getGenerationTime());
            assertEquals(GENERATION_TIME, sample.getReceptionTime());
            assertEquals("test-device", sample.getRoute());
            assertNull(sample.getContainerId());
            assertNull(sample.getExtension());
        }
    }

    @Test
    void ignoresUnknownOidsAndUnparseableValuesWithoutDroppingValidSamples() throws Exception {
        GroupConfiguration group = group(
                entry(1, "Status", OidEntryType.INTEGER),
                entry(2, "Temperature", OidEntryType.DOUBLE),
                entry(3, "Description", OidEntryType.STRING));
        group.initialise("TEST.Device", processingModel(Map.of(
                "TEST.Device.Status", 201, "TEST.Device.Temperature", 202, "TEST.Device.Description", 203)));
        PDU response = response(
                binding(1, new OctetString("not-an-integer")),
                binding(99, new OctetString("unconfigured")),
                binding(2, new OctetString("not-a-number")),
                binding(3, new OctetString("Still available")));

        assertEquals(List.of(ParameterSample.of(203, GENERATION_TIME, GENERATION_TIME, null,
                        "Still available", "test-device", null)),
                group.mapResponse(device(), berEvent(group, response), GENERATION_TIME));
    }

    @Test
    void ignoresOidsWhoseParameterPathCannotBeResolved() throws Exception {
        GroupConfiguration group = group(
                entry(1, "Missing", OidEntryType.INTEGER),
                entry(2, "Status", OidEntryType.INTEGER));
        group.initialise("TEST.Device", processingModel(Map.of("TEST.Device.Status", 202)));

        assertEquals(List.of(ParameterSample.of(202, GENERATION_TIME, GENERATION_TIME, null,
                        7, "test-device", null)),
                group.mapResponse(device(), berEvent(group, response(
                        binding(1, new Integer32(99)), binding(2, new Integer32(7)))), GENERATION_TIME));
    }

    @Test
    void extractsTextEncodedIntegerBoundariesAndRejectsOverflow() {
        OidEntry integer = entry(1, "Status", OidEntryType.INTEGER);
        OidEntry longValue = entry(2, "Counter", OidEntryType.LONG);

        assertEquals(Integer.MIN_VALUE, integer.extractValue(new OctetString("-2147483648")));
        assertEquals(Integer.MAX_VALUE, integer.extractValue(new OctetString("2147483647")));
        assertNull(integer.extractValue(new OctetString("2147483648")));
        assertEquals(Long.MIN_VALUE, longValue.extractValue(new OctetString("-9223372036854775808")));
        assertEquals(Long.MAX_VALUE, longValue.extractValue(new OctetString("9223372036854775807")));
        assertNull(longValue.extractValue(new OctetString("9223372036854775808")));
    }

    @Test
    void rejectsNonOctetValuesForTextBinaryAndDoubleFields() {
        for (OidEntryType type : List.of(OidEntryType.STRING, OidEntryType.BYTE_ARRAY, OidEntryType.DOUBLE)) {
            assertNull(entry(1, "Value", type).extractValue(new Integer32(42)), type.name());
        }
    }

    private static OidEntry entry(int suffix, String path, OidEntryType type) {
        return new OidEntry(OID_PREFIX + suffix, path, type);
    }

    private static GroupConfiguration group(OidEntry... entries) {
        GroupConfiguration group = new GroupConfiguration();
        group.setOidEntryList(List.of(entries));
        return group;
    }

    private static VariableBinding binding(int suffix, Variable value) {
        return new VariableBinding(new OID(OID_PREFIX + suffix), value);
    }

    private static PDU response(VariableBinding... bindings) {
        PDU response = new PDU();
        response.setType(PDU.RESPONSE);
        response.setRequestID(new Integer32(42));
        response.setVariableBindings(List.of(bindings));
        return response;
    }

    private static ResponseEvent<UdpAddress> berEvent(GroupConfiguration group, PDU response) throws Exception {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        response.encodeBER(encoded);
        PDU decoded = new PDU();
        decoded.decodeBER(new BERInputStream(ByteBuffer.wrap(encoded.toByteArray())));
        return new ResponseEvent<>(group, new UdpAddress("127.0.0.1/161"),
                group.preparePollRequest(), decoded, null, 0);
    }

    private static SnmpDevice device() {
        SnmpDevice device = new SnmpDevice();
        device.setName("test-device");
        return device;
    }

    private static IProcessingModel processingModel(Map<String, Integer> parameterIds) {
        return (IProcessingModel) Proxy.newProxyInstance(IProcessingModel.class.getClassLoader(),
                new Class<?>[]{IProcessingModel.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getExternalIdOf")) {
                        throw new AssertionError("Unexpected processing model call: " + method.getName());
                    }
                    String path = ((SystemEntityPath) args[0]).asString();
                    Integer id = parameterIds.get(path);
                    if (id == null) {
                        throw new ProcessingModelException("Unknown parameter: " + path);
                    }
                    return id;
                });
    }
}
