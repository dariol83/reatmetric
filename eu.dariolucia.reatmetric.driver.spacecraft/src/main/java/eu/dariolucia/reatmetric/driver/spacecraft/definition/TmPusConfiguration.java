/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
