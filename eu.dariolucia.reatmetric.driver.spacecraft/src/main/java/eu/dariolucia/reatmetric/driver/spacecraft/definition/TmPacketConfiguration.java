/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import javax.xml.bind.annotation.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class TmPacketConfiguration {

    @XmlAttribute(name = "parameter-id-offset")
    private int parameterIdOffset = 0;

    @XmlElement(name = "default-pus-configuration")
    private TmPusConfiguration defaultTmPusConfiguration;

    @XmlElement(name = "apid-pus-configuration")
    private List<ApidTmPusConfiguration> apidPusConfiguration = new LinkedList<>();

    @XmlElementWrapper(name = "vc-limit")
    @XmlElement(name = "vc")
    private List<Integer> processVcs;

    public int getParameterIdOffset() {
        return parameterIdOffset;
    }

    public void setParameterIdOffset(int parameterIdOffset) {
        this.parameterIdOffset = parameterIdOffset;
    }

    public TmPusConfiguration getDefaultTmPusConfiguration() {
        return defaultTmPusConfiguration;
    }

    public void setDefaultTmPusConfiguration(TmPusConfiguration defaultTmPusConfiguration) {
        this.defaultTmPusConfiguration = defaultTmPusConfiguration;
    }

    public List<ApidTmPusConfiguration> getApidPusConfiguration() {
        return apidPusConfiguration;
    }

    public void setApidPusConfiguration(List<ApidTmPusConfiguration> apidPusConfiguration) {
        this.apidPusConfiguration = apidPusConfiguration;
    }

    public List<Integer> getProcessVcs() {
        return processVcs;
    }

    public void setProcessVcs(List<Integer> processVcs) {
        this.processVcs = processVcs;
    }

    // -------------------------------------------------------------
    // Transient data
    // -------------------------------------------------------------
    private transient final Map<Short, TmPusConfiguration> apid2pusConfiguration = new HashMap<>();

    public void buildLookupMap() {
        for(ApidTmPusConfiguration apidConf : this.apidPusConfiguration) {
            apid2pusConfiguration.put(apidConf.getApid(), apidConf);
        }
    }

    public TmPusConfiguration getPusConfigurationFor(short apid) {
        return apid2pusConfiguration.getOrDefault(apid, defaultTmPusConfiguration);
    }
}
