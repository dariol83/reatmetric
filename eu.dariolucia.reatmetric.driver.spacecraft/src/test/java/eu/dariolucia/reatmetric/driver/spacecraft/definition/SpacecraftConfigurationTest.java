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

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import eu.dariolucia.ccsds.encdec.time.AbsoluteTimeDescriptor;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpacecraftConfigurationTest {

    private static final String NAMESPACE = "http://dariolucia.eu/reatmetric/driver/spacecraft";
    private static final String CONFIGURATION = """
            <s:spacecraft xmlns:s="http://dariolucia.eu/reatmetric/driver/spacecraft">
                <name>Orbita Ω &amp; &lt;test&gt;</name>
                <id>321</id>
                <obt-epoch>2000-01-01T00:00:00.123Z</obt-epoch>
                <propagation-delay>7654321</propagation-delay>
                <tc randomize="false" fecf="false" ad-mode-default="true">
                    <tc-vc-descriptor tc-vc="1" segmentation="true" map-id="17"
                                      system-entity-path="SPACE.FOP.TCVC1"/>
                    <tc-vc-descriptor tc-vc="4" default-tc-vc="true" access-mode="VCA"/>
                </tc>
                <tm fecf="true" ocf="false" derandomize="true" aos-fhec="true"
                    aos-insert-zone-length="8" frame-length="2048" required-quality="ALL">
                    <tm-vc-descriptor id="2" process-type="VCA" override-live="NOT_LIVE" enable-gap-warning="false"/>
                    <tm-vc-descriptor id="7" process-type="IGNORE"/>
                    <type>AOS</type>
                </tm>
                <tm-packet parameter-id-offset="123456" required-quality="GOOD_ONLY" only-live-processing="true">
                    <default-pus-configuration destination-field-length="16" packet-subcounter-present="true"
                                               tm-spare-length="3" tm-pec-present="CRC">
                        <obt-cuc-config explicit-p-field="true" coarse="3" fine="2"/>
                    </default-pus-configuration>
                    <apid-pus-configuration apid="42" destination-field-length="8" tm-pec-present="ISO">
                        <obt-cds-config short-days="false" subtime-byte-res="4"/>
                    </apid-pus-configuration>
                    <vc-limit><vc>2</vc><vc>7</vc></vc-limit>
                </tm-packet>
                <tc-packet telecommand-id-offset="234567" activity-tc-packet-type="TC Ω &amp; test"
                           pus-source-id-default-value="123" pus-source-id-length="16"
                           pus-spare-length="5" tc-pec-present="ISO"/>
                <services>
                    <service type="mission.TimeService" configuration="$HOME/mission Ω/time &amp; status.xml"/>
                    <service type="mission.EventService" configuration=""/>
                </services>
                <external-connectors>
                    <external-connector type="mission.CltuConnector" data-unit-type="CLTU"
                                        configuration="host:23532?name=Ω&amp;mode=&quot;live&quot;" live="false"/>
                    <external-connector type="mission.PacketConnector" data-unit-type="TC_PACKET" configuration=""/>
                </external-connectors>
            </s:spacecraft>
            """;

    @Test
    void loadsTheBundledSpacecraftConfiguration() throws Exception {
        SpacecraftConfiguration configuration;
        try (InputStream stream = getClass().getResourceAsStream("/configuration.xml")) {
            assertNotNull(stream, "Bundled spacecraft configuration is missing");
            configuration = SpacecraftConfiguration.load(stream);
        }

        assertEquals("TEST", configuration.getName());
        assertEquals(123, configuration.getId());
        assertEquals(Instant.parse("2000-01-01T00:00:00Z"), configuration.getEpoch().toInstant());
        assertTrue(configuration.getTcDataLinkConfiguration().isAdModeDefault());
        assertEquals(2, configuration.getTcDataLinkConfiguration().getTcVcDescriptors().size());
        assertEquals(0, configuration.getTcDataLinkConfiguration().getDefaultTcVc());
        assertEquals(VirtualChannelType.VCA,
                configuration.getTcDataLinkConfiguration().getTcVcDescriptors().get(1).getAccessMode());
        assertEquals(1115, configuration.getTmDataLinkConfigurations().getFrameLength());
        assertEquals(4, configuration.getTmDataLinkConfigurations().getTmVcConfigurations().size());
        assertEquals(TransferFrameType.TM, configuration.getTmDataLinkConfigurations().getType());
        assertEquals(100000, configuration.getTmPacketConfiguration().getParameterIdOffset());
        CucConfiguration obt = assertInstanceOf(CucConfiguration.class,
                configuration.getTmPacketConfiguration().getDefaultTmPusConfiguration().getObtConfiguration());
        assertEquals(4, obt.getCoarse());
        assertEquals(2, obt.getFine());
        assertEquals(PacketErrorControlType.CRC, configuration.getTcPacketConfiguration().getTcPecPresent());
        assertEquals(5, configuration.getPacketServiceConfiguration().getServices().size());
        assertEquals(3, configuration.getExternalConnectorConfigurations().size());
    }

    @Test
    void retainsDefaultsWhenOptionalAttributesAndElementsAreOmitted() throws Exception {
        SpacecraftConfiguration configuration = load("""
                <s:spacecraft xmlns:s="http://dariolucia.eu/reatmetric/driver/spacecraft">
                    <name>Minimal</name><id>1</id>
                    <tc><tc-vc-descriptor tc-vc="2"/></tc>
                    <tm><tm-vc-descriptor id="3"/></tm>
                    <tm-packet/><tc-packet/>
                </s:spacecraft>
                """);

        assertNull(configuration.getEpoch());
        assertEquals(0, configuration.getPropagationDelay());
        TcDataLinkConfiguration tc = configuration.getTcDataLinkConfiguration();
        assertTrue(tc.isRandomize());
        assertTrue(tc.isFecf());
        assertFalse(tc.isAdModeDefault());
        assertEquals(2, tc.getDefaultTcVc());
        assertFalse(tc.getTcVcDescriptors().get(0).isSegmentation());
        assertEquals(0, tc.getTcVcDescriptors().get(0).getMapId());
        assertEquals(VirtualChannelType.PACKET, tc.getTcVcDescriptors().get(0).getAccessMode());
        TmDataLinkConfiguration tm = configuration.getTmDataLinkConfigurations();
        assertEquals(TransferFrameType.TM, tm.getType());
        assertFalse(tm.isFecfPresent());
        assertTrue(tm.isOcfPresent());
        assertFalse(tm.isDerandomize());
        assertFalse(tm.isAosFrameHeaderErrorControlPresent());
        assertEquals(0, tm.getAosTransferFrameInsertZoneLength());
        assertEquals(-1, tm.getFrameLength());
        assertEquals(RequiredTmQuality.NOT_BAD, tm.getRequiredQuality());
        assertEquals(OverrideLiveEnum.INHERIT, tm.getTmVcConfigurations().get(0).getOverrideLive());
        assertTrue(tm.getTmVcConfigurations().get(0).isEnableGapWarning());
        assertNull(configuration.getTmPacketConfiguration().getDefaultTmPusConfiguration());
        assertNull(configuration.getTmPacketConfiguration().getProcessVcs());
        assertFalse(configuration.getTmPacketConfiguration().isOnlyLiveProcessing());
        assertEquals("TC", configuration.getTcPacketConfiguration().getActivityTcPacketType());
        assertNull(configuration.getTcPacketConfiguration().getSourceIdDefaultValue());
        assertEquals(PacketErrorControlType.CRC, configuration.getTcPacketConfiguration().getTcPecPresent());
        assertTrue(configuration.getPacketServiceConfiguration().getServices().isEmpty());
        assertTrue(configuration.getExternalConnectorConfigurations().isEmpty());
    }

    @Test
    void roundTripPreservesExplicitSpacecraftAndVirtualChannelSettings() throws Exception {
        SpacecraftConfiguration configuration = roundTrip(load(CONFIGURATION));

        assertEquals("Orbita Ω & <test>", configuration.getName());
        assertEquals(321, configuration.getId());
        assertEquals(Instant.parse("2000-01-01T00:00:00.123Z"), configuration.getEpoch().toInstant());
        assertEquals(7654321, configuration.getPropagationDelay());
        TcDataLinkConfiguration tc = configuration.getTcDataLinkConfiguration();
        assertFalse(tc.isRandomize());
        assertFalse(tc.isFecf());
        assertTrue(tc.isAdModeDefault());
        assertEquals(4, tc.getDefaultTcVc());
        assertEquals(2, tc.getTcVcDescriptors().size());
        TcVcConfiguration tcVc = tc.getTcVcDescriptors().get(0);
        assertEquals(1, tcVc.getTcVc());
        assertTrue(tcVc.isSegmentation());
        assertEquals(17, tcVc.getMapId());
        assertEquals("SPACE.FOP.TCVC1", tcVc.getSystemEntityPath());
        assertEquals(VirtualChannelType.VCA, tc.getTcVcDescriptors().get(1).getAccessMode());
        TmDataLinkConfiguration tm = configuration.getTmDataLinkConfigurations();
        assertEquals(TransferFrameType.AOS, tm.getType());
        assertTrue(tm.isFecfPresent());
        assertFalse(tm.isOcfPresent());
        assertTrue(tm.isDerandomize());
        assertTrue(tm.isAosFrameHeaderErrorControlPresent());
        assertEquals(8, tm.getAosTransferFrameInsertZoneLength());
        assertEquals(2048, tm.getFrameLength());
        assertEquals(RequiredTmQuality.ALL, tm.getRequiredQuality());
        assertEquals(2, tm.getTmVcConfigurations().size());
        assertEquals(2, tm.getTmVcConfigurations().get(0).getId());
        assertEquals(VirtualChannelType.VCA, tm.getTmVcConfigurations().get(0).getProcessType());
        assertEquals(OverrideLiveEnum.NOT_LIVE, tm.getTmVcConfigurations().get(0).getOverrideLive());
        assertFalse(tm.getTmVcConfigurations().get(0).isEnableGapWarning());
        assertEquals(VirtualChannelType.IGNORE, tm.getTmVcConfigurations().get(1).getProcessType());
    }

    @Test
    void preservesPolymorphicTimeFormatsAndBuildsCcsdsDescriptorsAfterRoundTrip() throws Exception {
        SpacecraftConfiguration loaded = load(CONFIGURATION);
        for (SpacecraftConfiguration configuration : List.of(loaded, roundTrip(loaded))) {
            TmPacketConfiguration tm = configuration.getTmPacketConfiguration();
            assertEquals(123456, tm.getParameterIdOffset());
            assertEquals(RequiredTmQuality.GOOD_ONLY, tm.getRequiredQuality());
            assertTrue(tm.isOnlyLiveProcessing());
            assertEquals(List.of(2, 7), tm.getProcessVcs());
            TmPusConfiguration defaultPus = tm.getDefaultTmPusConfiguration();
            assertEquals(16, defaultPus.getDestinationLength());
            assertTrue(defaultPus.isPacketSubCounterPresent());
            assertEquals(3, defaultPus.getTmSpareLength());
            assertEquals(PacketErrorControlType.CRC, defaultPus.getTmPecPresent());
            CucConfiguration cuc = assertInstanceOf(CucConfiguration.class, defaultPus.getObtConfiguration());
            assertTrue(cuc.isExplicitPField());
            assertEquals(3, cuc.getCoarse());
            assertEquals(2, cuc.getFine());
            AbsoluteTimeDescriptor cucDescriptor = defaultPus.getTimeDescriptor();
            assertTrue(cucDescriptor.cuc);
            assertEquals(3, cucDescriptor.coarseTime);
            assertEquals(2, cucDescriptor.fineTime);
            assertEquals(1, tm.getApidPusConfiguration().size());
            ApidTmPusConfiguration apidPus = tm.getApidPusConfiguration().get(0);
            assertEquals(42, apidPus.getApid());
            assertEquals(8, apidPus.getDestinationLength());
            assertEquals(PacketErrorControlType.ISO, apidPus.getTmPecPresent());
            CdsConfiguration cds = assertInstanceOf(CdsConfiguration.class, apidPus.getObtConfiguration());
            assertFalse(cds.isExplicitPField());
            assertFalse(cds.isShortDays());
            assertEquals(4, cds.getSubtimeResolution());
            AbsoluteTimeDescriptor cdsDescriptor = apidPus.getTimeDescriptor();
            assertFalse(cdsDescriptor.cuc);
            assertFalse(cdsDescriptor.use16bits);
            assertEquals(4, cdsDescriptor.subMsPart);
            tm.buildLookupMap();
            assertSame(apidPus, tm.getPusConfigurationFor((short) 42));
            assertSame(defaultPus, tm.getPusConfigurationFor((short) 43));
            TcPacketConfiguration tc = configuration.getTcPacketConfiguration();
            assertEquals(234567, tc.getTelecommandIdOffset());
            assertEquals("TC Ω & test", tc.getActivityTcPacketType());
            assertEquals(Integer.valueOf(123), tc.getSourceIdDefaultValue());
            assertEquals(16, tc.getSourceIdLength());
            assertEquals(5, tc.getSpareLength());
            assertEquals(PacketErrorControlType.ISO, tc.getTcPecPresent());
        }
    }

    @Test
    void roundTripPreservesServiceAndConnectorListsWithEscapedUnicodeText() throws Exception {
        SpacecraftConfiguration loaded = load(CONFIGURATION);
        for (SpacecraftConfiguration configuration : List.of(loaded, roundTrip(loaded))) {
            List<ServiceConfiguration> services = configuration.getPacketServiceConfiguration().getServices();
            assertEquals(2, services.size());
            assertEquals("mission.TimeService", services.get(0).getType());
            assertEquals(System.getProperty("user.home") + "/mission Ω/time & status.xml", services.get(0).getConfiguration());
            assertEquals("mission.EventService", services.get(1).getType());
            assertEquals("", services.get(1).getConfiguration());
            List<ExternalConnectorConfiguration> connectors = configuration.getExternalConnectorConfigurations();
            assertEquals(2, connectors.size());
            assertEquals("mission.CltuConnector", connectors.get(0).getType());
            assertEquals(DataUnitType.CLTU, connectors.get(0).getDataUnitType());
            assertEquals("host:23532?name=Ω&mode=\"live\"", connectors.get(0).getConfiguration());
            assertFalse(connectors.get(0).isLive());
            assertEquals("mission.PacketConnector", connectors.get(1).getType());
            assertEquals(DataUnitType.TC_PACKET, connectors.get(1).getDataUnitType());
            assertEquals("", connectors.get(1).getConfiguration());
            assertTrue(connectors.get(1).isLive());
        }
    }

    @Test
    void wrapsMalformedXmlAndAnUnexpectedRootNamespaceAsIoExceptions() {
        IOException malformed = assertThrows(IOException.class,
                () -> load("<s:spacecraft xmlns:s=\"" + NAMESPACE + "\"><name>broken</name>"));
        assertInstanceOf(JAXBException.class, malformed.getCause());

        IOException unexpectedRoot = assertThrows(IOException.class,
                () -> load("<spacecraft xmlns=\"urn:unexpected\"><name>Wrong namespace</name></spacecraft>"));
        assertInstanceOf(JAXBException.class, unexpectedRoot.getCause());
    }

    private static SpacecraftConfiguration load(String xml) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return SpacecraftConfiguration.load(stream);
        }
    }

    private static SpacecraftConfiguration roundTrip(SpacecraftConfiguration configuration) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JAXBContext.newInstance(SpacecraftConfiguration.class).createMarshaller().marshal(configuration, output);
        try (InputStream stream = new ByteArrayInputStream(output.toByteArray())) {
            return SpacecraftConfiguration.load(stream);
        }
    }
}
