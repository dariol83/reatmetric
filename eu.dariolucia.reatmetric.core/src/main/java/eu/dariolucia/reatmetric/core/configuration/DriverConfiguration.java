/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DriverConfiguration {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "configuration", required = true)
    private String configuration;

    public DriverConfiguration() {
    }

    public String getName() {
        return name;
    }

    public DriverConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public DriverConfiguration setType(String type) {
        this.type = type;
        return this;
    }

    public String getConfiguration() {
        return configuration;
    }

    public DriverConfiguration setConfiguration(String configuration) {
        this.configuration = configuration;
        return this;
    }
}
