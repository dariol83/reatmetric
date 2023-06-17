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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcDataLinkConfiguration {

    @XmlAttribute(name = "randomize")
    private boolean randomize = true;

    @XmlAttribute(name = "fecf")
    private boolean fecf = true;

    @XmlAttribute(name = "ad-mode-default")
    private boolean adModeDefault = false;

    @XmlElement(name = "tc-vc-descriptor")
    private List<TcVcConfiguration> tcVcDescriptors = new LinkedList<>();

    public int getDefaultTcVc() {
        for(TcVcConfiguration tcVc : tcVcDescriptors) {
            if(tcVc.isDefaultTcVc()) {
                return tcVc.getTcVc();
            }
        }
        // Return the first one
        return tcVcDescriptors.get(0).getTcVc();
    }

    public boolean isRandomize() {
        return randomize;
    }

    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    public boolean isAdModeDefault() {
        return adModeDefault;
    }

    public void setAdModeDefault(boolean adModeDefault) {
        this.adModeDefault = adModeDefault;
    }

    public boolean isFecf() {
        return this.fecf;
    }

    public void setFecf(boolean fecf) {
        this.fecf = fecf;
    }

    public List<TcVcConfiguration> getTcVcDescriptors() {
        return tcVcDescriptors;
    }

    public void setTcVcDescriptors(List<TcVcConfiguration> tcVcDescriptors) {
        this.tcVcDescriptors = tcVcDescriptors;
    }
}
