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
public class CdsConfiguration extends ObtConfiguration {

    @XmlAttribute(name = "short_days")
    private boolean shortDays = true; // true: 16 bits, false: 24 bits

    @XmlAttribute(name = "subtime_byte_res")
    private int subtimeResolution = 2; // 0, 2 (micro in milli) or 4 (pico in milli)

    public CdsConfiguration() {
    }

    public boolean isShortDays() {
        return shortDays;
    }

    public void setShortDays(boolean shortDays) {
        this.shortDays = shortDays;
    }

    public int getSubtimeResolution() {
        return subtimeResolution;
    }

    public void setSubtimeResolution(int subtimeResolution) {
        this.subtimeResolution = subtimeResolution;
    }
}
