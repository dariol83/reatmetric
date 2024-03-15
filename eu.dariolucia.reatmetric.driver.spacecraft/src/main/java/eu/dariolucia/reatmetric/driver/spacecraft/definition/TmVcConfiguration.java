/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

@XmlAccessorType(XmlAccessType.FIELD)
public class TmVcConfiguration {

    @XmlAttribute(required = true)
    private int id;

    @XmlAttribute(name = "process-type")
    private VirtualChannelType processType = VirtualChannelType.PACKET;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public VirtualChannelType getProcessType() {
        return processType;
    }

    public void setProcessType(VirtualChannelType processType) {
        this.processType = processType;
    }
}
