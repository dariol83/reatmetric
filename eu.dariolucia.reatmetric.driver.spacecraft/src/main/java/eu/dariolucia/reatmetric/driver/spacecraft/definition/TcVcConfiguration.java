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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcVcConfiguration {

    @XmlAttribute(name = "tc-vc", required = true)
    private int tcVc = 0;

    @XmlAttribute(name = "segmentation")
    private boolean segmentation = false;

    @XmlAttribute(name = "map-id")
    private int mapId = 0;

    @XmlAttribute(name = "default-tc-vc")
    private boolean defaultTcVc = false;

    public TcVcConfiguration() {
    }

    public TcVcConfiguration(int tcVc) {
        this.tcVc = tcVc;
    }

    public int getTcVc() {
        return tcVc;
    }

    public void setTcVc(int tcVc) {
        this.tcVc = tcVc;
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

    public boolean isDefaultTcVc() {
        return defaultTcVc;
    }

    public void setDefaultTcVc(boolean defaultTcVc) {
        this.defaultTcVc = defaultTcVc;
    }
}
