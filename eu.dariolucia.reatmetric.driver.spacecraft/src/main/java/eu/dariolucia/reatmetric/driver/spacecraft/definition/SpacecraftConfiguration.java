/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

@XmlRootElement(name = "spacecraft", namespace = "http://dariolucia.eu/reatmetric/driver/spacecraft")
@XmlAccessorType(XmlAccessType.FIELD)
public class SpacecraftConfiguration {

    public static SpacecraftConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SpacecraftConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SpacecraftConfiguration o = (SpacecraftConfiguration) u.unmarshal(is);
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
    private Instant epoch = null;

    @XmlElement(name = "tc")
    private TcDataLinkConfiguration tcDataLinkConfiguration = new TcDataLinkConfiguration();

    @XmlElement(name = "tm")
    private TmDataLinkConfiguration tmDataLinkConfigurations = new TmDataLinkConfiguration();

    @XmlElement(name = "pus", required = true)
    private TmPacketConfiguration tmPacketConfiguration = new TmPacketConfiguration();

    public SpacecraftConfiguration() {
    }

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

    public Instant getEpoch() {
        return epoch;
    }

    public void setEpoch(Instant epoch) {
        this.epoch = epoch;
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
}
