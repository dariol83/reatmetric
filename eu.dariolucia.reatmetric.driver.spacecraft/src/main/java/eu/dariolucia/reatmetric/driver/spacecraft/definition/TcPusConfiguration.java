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
public class TcPusConfiguration {

    @XmlAttribute(name = "source_field_length")
    private int sourceLength = 0;

    @XmlAttribute(name = "tc_spare_length")
    private int tcSpareLength = 0;

    public int getSourceLength() {
        return sourceLength;
    }

    public void setSourceLength(int sourceLength) {
        this.sourceLength = sourceLength;
    }

    public int getTcSpareLength() {
        return tcSpareLength;
    }

    public void setTcSpareLength(int tcSpareLength) {
        this.tcSpareLength = tcSpareLength;
    }
}
