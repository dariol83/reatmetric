/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.definition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "tmtc", namespace = "http://dariolucia.eu/monitoringcentre/core/tmtc")
@XmlAccessorType(XmlAccessType.FIELD)
public class TmTcConfiguration {

    public static TmTcConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(TmTcConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            TmTcConfiguration o = (TmTcConfiguration) u.unmarshal(is);
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlElement(name = "tc_vc")
    private TcVirtualChannelConfiguration tcVirtualChannelConfiguration = new TcVirtualChannelConfiguration();

    @XmlElement(name = "tm_vc")
    private TmVirtualChannelConfiguration tmVirtualChannelConfigurations = new TmVirtualChannelConfiguration();

    @XmlElement(name = "pus")
    private PusConfiguration pusConfiguration = new PusConfiguration();

    public TmTcConfiguration() {
    }

    public TcVirtualChannelConfiguration getTcVirtualChannelConfiguration() {
        return tcVirtualChannelConfiguration;
    }

    public void setTcVirtualChannelConfiguration(TcVirtualChannelConfiguration tcVirtualChannelConfiguration) {
        this.tcVirtualChannelConfiguration = tcVirtualChannelConfiguration;
    }

    public TmVirtualChannelConfiguration getTmVirtualChannelConfigurations() {
        return tmVirtualChannelConfigurations;
    }

    public void setTmVirtualChannelConfigurations(TmVirtualChannelConfiguration tmVirtualChannelConfigurations) {
        this.tmVirtualChannelConfigurations = tmVirtualChannelConfigurations;
    }

    public PusConfiguration getPusConfiguration() {
        return pusConfiguration;
    }

    public void setPusConfiguration(PusConfiguration pusConfiguration) {
        this.pusConfiguration = pusConfiguration;
    }
}
