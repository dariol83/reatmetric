/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc.definition;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class TmVirtualChannelConfiguration {

    @XmlElementWrapper(name = "vc_limit")
    @XmlElement(name = "vc")
    private List<Integer> processVcs;

    @XmlAttribute(name = "fecf")
    private boolean fecfPresent = false;

    @XmlAttribute(name = "ocf")
    private boolean ocfPresent = true;

    @XmlAttribute(name = "aos_fhec")
    private boolean aosFrameHeaderErrorControlPresent = false;

    @XmlAttribute(name = "aos_insert_zone_length")
    private int aosTransferFrameInsertZoneLength = 0;

    public TmVirtualChannelConfiguration() {
    }

    public List<Integer> getProcessVcs() {
        return processVcs;
    }

    public void setProcessVcs(List<Integer> processVcs) {
        this.processVcs = processVcs;
    }

    public boolean isFecfPresent() {
        return fecfPresent;
    }

    public void setFecfPresent(boolean fecfPresent) {
        this.fecfPresent = fecfPresent;
    }

    public boolean isOcfPresent() {
        return ocfPresent;
    }

    public void setOcfPresent(boolean ocfPresent) {
        this.ocfPresent = ocfPresent;
    }

    public boolean isAosFrameHeaderErrorControlPresent() {
        return aosFrameHeaderErrorControlPresent;
    }

    public void setAosFrameHeaderErrorControlPresent(boolean aosFrameHeaderErrorControlPresent) {
        this.aosFrameHeaderErrorControlPresent = aosFrameHeaderErrorControlPresent;
    }

    public int getAosTransferFrameInsertZoneLength() {
        return aosTransferFrameInsertZoneLength;
    }

    public void setAosTransferFrameInsertZoneLength(int aosTransferFrameInsertZoneLength) {
        this.aosTransferFrameInsertZoneLength = aosTransferFrameInsertZoneLength;
    }
}
