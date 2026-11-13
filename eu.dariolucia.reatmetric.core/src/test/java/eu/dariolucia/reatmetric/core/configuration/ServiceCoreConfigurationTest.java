/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.core.configuration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceCoreConfigurationTest {

    private static final String NAMESPACE = "http://dariolucia.eu/reatmetric/core/configuration";

    @Test
    void loadsAndRoundTripsResumeConfigurationWithDriversAndConnectorLists() throws Exception {
        ServiceCoreConfiguration configuration = load(configurationXml("""
                <name>Centrale Ω &amp; &lt;missione&gt;</name>
                <log-property-file>config/registri-é.properties</log-property-file>
                <archive-location>archive/storico &amp; dati</archive-location>
                <definitions-location>definitions/telemetria-東京</definitions-location>
                <init-resume look-back-time="7200"/>
                <scheduler-configuration>config/pianificatore.xml</scheduler-configuration>
                <driver name="USB &amp; &quot;A&quot;" type="example.SerialDriver" configuration="config/serial-é"/>
                <driver name="遥測" type="example.RemoteDriver" configuration="config/remote &amp; backup"/>
                <autostart-connectors startup="true" reconnect="true">
                    <startup-exclusion>link &amp; backup</startup-exclusion>
                    <startup-exclusion>手動</startup-exclusion>
                    <reconnect-exclusion>maintenance</reconnect-exclusion>
                    <reconnect-exclusion>réserve</reconnect-exclusion>
                </autostart-connectors>
                """));

        assertResumeConfiguration(configuration);
        assertResumeConfiguration(roundTrip(configuration));
    }

    @Test
    void preservesTimeInitialisationAndItsArchiveAcrossAMarshalLoadRoundTrip() throws Exception {
        ServiceCoreConfiguration configuration = load(configurationXml("""
                <definitions-location>definitions</definitions-location>
                <init-from-time time="2026-11-13T12:34:56.789+02:00" archive-location="replay/Ω &amp; history"/>
                """));

        assertTimeInitialisation(configuration);
        assertTimeInitialisation(roundTrip(configuration));
    }

    @Test
    void retainsDefaultsWhenOptionalConfigurationIsAbsent() throws Exception {
        ServiceCoreConfiguration configuration = load(configurationXml("""
                <definitions-location>definitions</definitions-location>
                """));

        assertNull(configuration.getName());
        assertNull(configuration.getArchiveLocation());
        assertNull(configuration.getLogPropertyFile());
        assertNull(configuration.getSchedulerConfiguration());
        assertNull(configuration.getInitialisation());
        assertTrue(configuration.getDrivers().isEmpty());
        assertNotNull(configuration.getAutostartConnectors());
        assertFalse(configuration.getAutostartConnectors().isStartup());
        assertFalse(configuration.getAutostartConnectors().isReconnect());
        assertTrue(configuration.getAutostartConnectors().getStartupExclusions().isEmpty());
        assertTrue(configuration.getAutostartConnectors().getReconnectExclusions().isEmpty());

        ServiceCoreConfiguration resumed = load(configurationXml("""
                <definitions-location>definitions</definitions-location>
                <init-resume/>
                <autostart-connectors/>
                """));
        assertEquals(3600, assertInstanceOf(ResumeInitialisationConfiguration.class,
                resumed.getInitialisation()).getLookBackTime());
        assertFalse(resumed.getAutostartConnectors().isStartup());
        assertFalse(resumed.getAutostartConnectors().isReconnect());
    }

    @Test
    void expandsSupportedHomeAndPrefixTokensWithoutRewritingOtherText() throws Exception {
        String home = System.getProperty("user.home");
        String prefix = System.getProperty("reatmetric.prefix.dir", "");
        ServiceCoreConfiguration configuration = load(configurationXml("""
                <log-property-file>$HOME/logs/reatmetric.properties</log-property-file>
                <archive-location>$PREFIX/archive/é</archive-location>
                <definitions-location>$HOME/definitions/Ω</definitions-location>
                <scheduler-configuration>$PREFIX/scheduler/configuration.xml</scheduler-configuration>
                <driver name="driver" type="example.Driver" configuration="$HOME/$PREFIX/drivers/$UNRESOLVED"/>
                """));

        assertEquals(home + "/logs/reatmetric.properties", configuration.getLogPropertyFile());
        assertEquals(prefix + "/archive/é", configuration.getArchiveLocation());
        assertEquals(home + "/definitions/Ω", configuration.getDefinitionsLocation());
        assertEquals(prefix + "/scheduler/configuration.xml", configuration.getSchedulerConfiguration());
        assertEquals((home + "/$PREFIX/drivers/$UNRESOLVED").replace("$PREFIX", prefix),
                configuration.getDrivers().get(0).getConfiguration());
    }

    @Test
    void rejectsARootElementInTheWrongNamespace() {
        String xml = configurationXml("<definitions-location>definitions</definitions-location>")
                .replace(NAMESPACE, "urn:unrelated:configuration");

        assertThrows(JAXBException.class, () -> load(xml));
    }

    @Test
    void reportsMalformedXmlThroughTheLoadersJaxbExceptionContract() {
        String xml = configurationXml("<definitions-location>definitions</definitions-location><driver");

        assertThrows(JAXBException.class, () -> load(xml));
    }

    private static void assertResumeConfiguration(ServiceCoreConfiguration configuration) {
        assertEquals("Centrale Ω & <missione>", configuration.getName());
        assertEquals("config/registri-é.properties", configuration.getLogPropertyFile());
        assertEquals("archive/storico & dati", configuration.getArchiveLocation());
        assertEquals("definitions/telemetria-東京", configuration.getDefinitionsLocation());
        assertEquals("config/pianificatore.xml", configuration.getSchedulerConfiguration());
        assertEquals(7200, assertInstanceOf(ResumeInitialisationConfiguration.class,
                configuration.getInitialisation()).getLookBackTime());

        assertEquals(2, configuration.getDrivers().size());
        DriverConfiguration serial = configuration.getDrivers().get(0);
        assertEquals("USB & \"A\"", serial.getName());
        assertEquals("example.SerialDriver", serial.getType());
        assertEquals("config/serial-é", serial.getConfiguration());
        DriverConfiguration remote = configuration.getDrivers().get(1);
        assertEquals("遥測", remote.getName());
        assertEquals("example.RemoteDriver", remote.getType());
        assertEquals("config/remote & backup", remote.getConfiguration());

        AutostartConnectorsConfiguration connectors = configuration.getAutostartConnectors();
        assertTrue(connectors.isStartup());
        assertTrue(connectors.isReconnect());
        assertEquals(List.of("link & backup", "手動"), connectors.getStartupExclusions());
        assertEquals(List.of("maintenance", "réserve"), connectors.getReconnectExclusions());
    }

    private static void assertTimeInitialisation(ServiceCoreConfiguration configuration) {
        assertEquals("definitions", configuration.getDefinitionsLocation());
        TimeInitialisationConfiguration initialisation = assertInstanceOf(TimeInitialisationConfiguration.class,
                configuration.getInitialisation());
        assertEquals(Date.from(Instant.parse("2026-11-13T10:34:56.789Z")), initialisation.getTime());
        assertEquals("replay/Ω & history", initialisation.getArchiveLocation());
        assertEquals(3600, initialisation.getLookBackTime());
    }

    private static ServiceCoreConfiguration roundTrip(ServiceCoreConfiguration configuration) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JAXBContext.newInstance(ServiceCoreConfiguration.class).createMarshaller().marshal(configuration, output);
        try (ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray())) {
            return ServiceCoreConfiguration.load(input);
        }
    }

    private static ServiceCoreConfiguration load(String xml) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return ServiceCoreConfiguration.load(input);
        }
    }

    private static String configurationXml(String elements) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<core:core xmlns:core=\"" + NAMESPACE + "\">\n" +
                elements + "\n</core:core>";
    }
}
