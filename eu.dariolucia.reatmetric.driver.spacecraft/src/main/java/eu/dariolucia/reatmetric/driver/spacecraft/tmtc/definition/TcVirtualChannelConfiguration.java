/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcVirtualChannelConfiguration {

    @XmlAttribute(name = "tc_vc")
    private int commandVc = 0;

    public TcVirtualChannelConfiguration() {
    }

    public int getCommandVc() {
        return commandVc;
    }

    public void setCommandVc(int commandVc) {
        this.commandVc = commandVc;
    }
}
