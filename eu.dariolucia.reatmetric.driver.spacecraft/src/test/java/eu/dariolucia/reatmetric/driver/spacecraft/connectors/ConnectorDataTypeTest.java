/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorDataTypeTest {

    @ParameterizedTest(name = "{0} accepts {1}")
    @MethodSource("supportedDataTypes")
    void acceptsSupportedDataTypesAndAdvertisesRaw(Class<? extends AbstractTransportConnector> connectorType,
                                                   String dataType) throws Exception {
        AbstractTransportConnector connector = connectorType.getConstructor().newInstance();
        connector.prepare();
        try {
            assertEquals("TM_FRAME", connector.getCurrentProperties().get("dataType"));
            String description = connector.getSupportedProperties().get("dataType").getFirst();
            assertTrue(description.contains("TM_FRAME"), description);
            assertTrue(description.contains("AOS_FRAME"), description);
            assertTrue(description.contains("RAW"), description);
            assertFalse(description.contains("VCA"), description);

            initialise(connector, dataType);
            Object configuration = currentConfiguration(connector).invoke(connector);
            Method configuredType = configuration.getClass().getDeclaredMethod("dataType");
            configuredType.setAccessible(true);
            assertEquals(dataType, ((Enum<?>) configuredType.invoke(configuration)).name());
        } finally {
            connector.dispose();
        }
    }

    @ParameterizedTest(name = "{0} rejects VCA")
    @MethodSource("connectorTypes")
    void rejectsVcaDataType(Class<? extends AbstractTransportConnector> connectorType) throws Exception {
        AbstractTransportConnector connector = connectorType.getConstructor().newInstance();
        connector.prepare();
        try {
            initialise(connector, "VCA");
            Method currentConfiguration = currentConfiguration(connector);
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> currentConfiguration.invoke(connector));
            TransportException cause = assertInstanceOf(TransportException.class, failure.getCause());
            assertTrue(cause.getMessage().contains("VCA"), cause.getMessage());
        } finally {
            connector.dispose();
        }
    }

    private static void initialise(AbstractTransportConnector connector, String dataType) {
        Map<String, Object> properties = new HashMap<>(connector.getCurrentProperties());
        properties.put("dataUrl", "tcp://127.0.0.1:6001");
        properties.put("dataType", dataType);
        connector.initialise(properties);
    }

    private static Method currentConfiguration(AbstractTransportConnector connector) throws NoSuchMethodException {
        // Validate the effective settings without opening a TCP/ZeroMQ connection.
        Method currentConfiguration = connector.getClass().getDeclaredMethod("currentConfiguration");
        currentConfiguration.setAccessible(true);
        return currentConfiguration;
    }

    private static Stream<Class<? extends AbstractTransportConnector>> connectorTypes() {
        return Stream.of(JtdpConnector.class, SatDecodeConnector.class);
    }

    private static Stream<Arguments> supportedDataTypes() {
        return connectorTypes().flatMap(type -> Stream.of("TM_FRAME", "AOS_FRAME", "RAW")
                .map(dataType -> Arguments.of(type, dataType)));
    }
}
