/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ApidTmPusConfiguration extends TmPusConfiguration {

    @XmlAttribute(name = "apid", required = true)
    private short apid;

    public short getApid() {
        return apid;
    }

    public void setApid(short apid) {
        this.apid = apid;
    }
}
