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

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class TmDataLinkConfiguration {

    @XmlElementWrapper(name = "vc-limit")
    @XmlElement(name = "vc")
    private List<Integer> processVcs;

    @XmlElement(name = "type")
    private TransferFrameType type = TransferFrameType.TM;

    @XmlAttribute(name = "fecf")
    private boolean fecfPresent = false;

    @XmlAttribute(name = "ocf")
    private boolean ocfPresent = true;

    @XmlAttribute(name = "derandomize")
    private boolean derandomize = false;

    @XmlAttribute(name = "aos-fhec")
    private boolean aosFrameHeaderErrorControlPresent = false;

    @XmlAttribute(name = "aos-insert-zone-length")
    private int aosTransferFrameInsertZoneLength = 0;

    @XmlAttribute(name = "frame-length")
    private int frameLength = -1;

    public TmDataLinkConfiguration() {
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

    public boolean isDerandomize() {
        return derandomize;
    }

    public void setDerandomize(boolean derandomize) {
        this.derandomize = derandomize;
    }

    public TransferFrameType getType() {
        return type;
    }

    public void setType(TransferFrameType type) {
        this.type = type;
    }

    public void setFrameLength(int frameLength) {
        this.frameLength = frameLength;
    }

    public int getFrameLength() {
        return frameLength;
    }
}
