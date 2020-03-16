/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import javax.xml.bind.annotation.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class TmPacketConfiguration {

    @XmlAttribute(name = "parameter-id-offset")
    private int parameterIdOffset = 0;

    @XmlAttribute(name = "event-id-offset")
    private int eventIdOffset = 0;

    @XmlElement(name = "default-pus-configuration")
    private PusConfiguration defaultPusConfiguration;

    @XmlElement(name = "apid-pus-configuration")
    private List<ApidPusConfiguration> apidPusConfiguration = new LinkedList<>();

    public int getParameterIdOffset() {
        return parameterIdOffset;
    }

    public void setParameterIdOffset(int parameterIdOffset) {
        this.parameterIdOffset = parameterIdOffset;
    }

    public int getEventIdOffset() {
        return eventIdOffset;
    }

    public void setEventIdOffset(int eventIdOffset) {
        this.eventIdOffset = eventIdOffset;
    }

    public PusConfiguration getDefaultPusConfiguration() {
        return defaultPusConfiguration;
    }

    public void setDefaultPusConfiguration(PusConfiguration defaultPusConfiguration) {
        this.defaultPusConfiguration = defaultPusConfiguration;
    }

    public List<ApidPusConfiguration> getApidPusConfiguration() {
        return apidPusConfiguration;
    }

    public void setApidPusConfiguration(List<ApidPusConfiguration> apidPusConfiguration) {
        this.apidPusConfiguration = apidPusConfiguration;
    }

    // -------------------------------------------------------------
    // Transient data
    // -------------------------------------------------------------
    private transient final Map<Short, PusConfiguration> apid2pusConfiguration = new HashMap<>();

    public void buildLookupMap() {
        for(ApidPusConfiguration apidConf : this.apidPusConfiguration) {
            apid2pusConfiguration.put(apidConf.getApid(), apidConf);
        }
    }

    public PusConfiguration getPusConfigurationFor(short apid) {
        return apid2pusConfiguration.getOrDefault(apid, defaultPusConfiguration);
    }
}
