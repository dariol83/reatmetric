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
public class TcDataLinkConfiguration {

    @XmlAttribute(name = "tc_vc")
    private int commandVc = 0;

    @XmlAttribute(name = "randomize")
    private boolean randomize = true;

    @XmlAttribute(name = "segmentation")
    private boolean segmentation = false;

    @XmlAttribute(name = "mapId")
    private int mapId = 0;

    public TcDataLinkConfiguration() {
    }

    public int getCommandVc() {
        return commandVc;
    }

    public void setCommandVc(int commandVc) {
        this.commandVc = commandVc;
    }

    public boolean isRandomize() {
        return randomize;
    }

    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    public boolean isSegmentation() {
        return segmentation;
    }

    public void setSegmentation(boolean segmentation) {
        this.segmentation = segmentation;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }
}
