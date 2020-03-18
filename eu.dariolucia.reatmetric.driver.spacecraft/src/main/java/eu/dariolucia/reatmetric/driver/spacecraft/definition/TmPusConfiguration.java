/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.definition;

import eu.dariolucia.ccsds.encdec.time.AbsoluteTimeDescriptor;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class TmPusConfiguration {

    @XmlAttribute(name = "destination-field-length")
    private int destinationLength = 0;

    @XmlAttribute(name = "packet-subcounter-present")
    private boolean packetSubCounterPresent = false;

    @XmlAttribute(name = "tm-spare-length")
    private int tmSpareLength = 0;

    @XmlAttribute(name = "tm-pec-present")
    private PacketErrorControlType tmPecPresent = PacketErrorControlType.NONE;

    @XmlElements({
            @XmlElement(name = "obt-cuc-config", type = CucConfiguration.class),
            @XmlElement(name = "obt-cds-config", type = CdsConfiguration.class)
    })
    private ObtConfiguration obtConfiguration = null; // null means not present

    public int getDestinationLength() {
        return destinationLength;
    }

    public void setDestinationLength(int destinationLength) {
        this.destinationLength = destinationLength;
    }

    public boolean isPacketSubCounterPresent() {
        return packetSubCounterPresent;
    }

    public void setPacketSubCounterPresent(boolean packetSubCounterPresent) {
        this.packetSubCounterPresent = packetSubCounterPresent;
    }

    public int getTmSpareLength() {
        return tmSpareLength;
    }

    public void setTmSpareLength(int tmSpareLength) {
        this.tmSpareLength = tmSpareLength;
    }

    public PacketErrorControlType getTmPecPresent() {
        return tmPecPresent;
    }

    public void setTmPecPresent(PacketErrorControlType tmPecPresent) {
        this.tmPecPresent = tmPecPresent;
    }

    public ObtConfiguration getObtConfiguration() {
        return obtConfiguration;
    }

    public void setObtConfiguration(ObtConfiguration obtConfiguration) {
        this.obtConfiguration = obtConfiguration;
    }

    public AbsoluteTimeDescriptor getTimeDescriptor() {
        if(obtConfiguration == null) {
            return null;
        } else {
            if(obtConfiguration instanceof CucConfiguration) {
                CucConfiguration cuc = (CucConfiguration) obtConfiguration;
                return AbsoluteTimeDescriptor.newCucDescriptor(cuc.getCoarse(), cuc.getFine());
            } else if(obtConfiguration instanceof CdsConfiguration) {
                CdsConfiguration cds = (CdsConfiguration) obtConfiguration;
                return AbsoluteTimeDescriptor.newCdsDescriptor(cds.isShortDays(), cds.getSubtimeResolution());
            } else {
                return null;
            }
        }
    }
}
