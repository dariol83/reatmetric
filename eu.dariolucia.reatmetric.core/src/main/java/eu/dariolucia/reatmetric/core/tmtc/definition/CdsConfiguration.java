/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.tmtc.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class CdsConfiguration extends ObtConfiguration {

    @XmlAttribute(name = "short_days")
    private boolean shortDays = true; // true: 16 bits, false: 24 bits

    @XmlAttribute(name = "subtime_byte_res")
    private int subtimeResolution = 2; // 0, 2 (micro in milli) or 4 (pico in milli)

    public CdsConfiguration() {
    }

    public boolean isShortDays() {
        return shortDays;
    }

    public void setShortDays(boolean shortDays) {
        this.shortDays = shortDays;
    }

    public int getSubtimeResolution() {
        return subtimeResolution;
    }

    public void setSubtimeResolution(int subtimeResolution) {
        this.subtimeResolution = subtimeResolution;
    }
}
