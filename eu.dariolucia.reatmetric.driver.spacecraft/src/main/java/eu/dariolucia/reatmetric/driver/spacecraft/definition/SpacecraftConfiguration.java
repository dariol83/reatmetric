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

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "spacecraft", namespace = "http://dariolucia.eu/reatmetric/driver/spacecraft")
@XmlAccessorType(XmlAccessType.FIELD)
public class SpacecraftConfiguration {

    private static final String HOME_VAR = "$HOME";
    private static final String HOME_DIR = System.getProperty("user.home");
    public static SpacecraftConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SpacecraftConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SpacecraftConfiguration o = (SpacecraftConfiguration) u.unmarshal(is);
            if(o.getPacketServiceConfiguration() != null) {
                for (ServiceConfiguration sc : o.getPacketServiceConfiguration().getServices()) {
                    sc.setConfiguration(sc.getConfiguration().replace(HOME_VAR, HOME_DIR));
                }
            }
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "id", required = true)
    private int id;

    @XmlElement(name = "obt-epoch")
    private Date epoch = null;

    @XmlElement(name = "propagation-delay")
    private long propagationDelay = 0; // From first bit ASM generation onboard to reception of the first bit ASM at the antenna, in microseconds

    @XmlElement(name = "tc")
    private TcDataLinkConfiguration tcDataLinkConfiguration = new TcDataLinkConfiguration();

    @XmlElement(name = "tm")
    private TmDataLinkConfiguration tmDataLinkConfigurations = new TmDataLinkConfiguration();

    @XmlElement(name = "tm-packet", required = true)
    private TmPacketConfiguration tmPacketConfiguration = new TmPacketConfiguration();

    @XmlElement(name = "tc-packet", required = true)
    private TcPacketConfiguration tcPacketConfiguration = new TcPacketConfiguration();

    @XmlElement(name = "services")
    private PacketServiceConfiguration packetServiceConfiguration = new PacketServiceConfiguration();

    @XmlElementWrapper(name="external-connectors")
    @XmlElement(name = "external-connector")
    private List<ExternalConnectorConfiguration> externalConnectorConfigurations = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getEpoch() {
        return epoch;
    }

    public void setEpoch(Date epoch) {
        this.epoch = epoch;
    }

    /**
     * Return the propagation delay (one way light time) in microseconds.
     *
     * @return the propagation delay in microseconds
     */
    public long getPropagationDelay() {
        return propagationDelay;
    }

    public void setPropagationDelay(long propagationDelay) {
        this.propagationDelay = propagationDelay;
    }

    public TcDataLinkConfiguration getTcDataLinkConfiguration() {
        return tcDataLinkConfiguration;
    }

    public void setTcDataLinkConfiguration(TcDataLinkConfiguration tcDataLinkConfiguration) {
        this.tcDataLinkConfiguration = tcDataLinkConfiguration;
    }

    public TmDataLinkConfiguration getTmDataLinkConfigurations() {
        return tmDataLinkConfigurations;
    }

    public void setTmDataLinkConfigurations(TmDataLinkConfiguration tmDataLinkConfigurations) {
        this.tmDataLinkConfigurations = tmDataLinkConfigurations;
    }

    public TmPacketConfiguration getTmPacketConfiguration() {
        return tmPacketConfiguration;
    }

    public void setTmPacketConfiguration(TmPacketConfiguration tmPacketConfiguration) {
        this.tmPacketConfiguration = tmPacketConfiguration;
    }

    public PacketServiceConfiguration getPacketServiceConfiguration() {
        return packetServiceConfiguration;
    }

    public void setPacketServiceConfiguration(PacketServiceConfiguration packetServiceConfiguration) {
        this.packetServiceConfiguration = packetServiceConfiguration;
    }

    public TcPacketConfiguration getTcPacketConfiguration() {
        return tcPacketConfiguration;
    }

    public void setTcPacketConfiguration(TcPacketConfiguration tcPacketConfiguration) {
        this.tcPacketConfiguration = tcPacketConfiguration;
    }

    public List<ExternalConnectorConfiguration> getExternalConnectorConfigurations() {
        return externalConnectorConfigurations;
    }

    public void setExternalConnectorConfigurations(List<ExternalConnectorConfiguration> externalConnectorConfigurations) {
        this.externalConnectorConfigurations = externalConnectorConfigurations;
    }
}
