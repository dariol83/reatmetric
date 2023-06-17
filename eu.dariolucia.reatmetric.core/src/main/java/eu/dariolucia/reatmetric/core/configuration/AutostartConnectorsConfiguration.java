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

package eu.dariolucia.reatmetric.core.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class AutostartConnectorsConfiguration {

    @XmlAttribute(name = "startup")
    private boolean startup = false;

    @XmlAttribute(name = "reconnect")
    private boolean reconnect = false;

    @XmlElement(name = "startup-exclusion")
    private List<String> startupExclusions = new LinkedList<>();

    @XmlElement(name = "reconnect-exclusion")
    private List<String> reconnectExclusions = new LinkedList<>();

    public AutostartConnectorsConfiguration() {
    }

    public boolean isStartup() {
        return startup;
    }

    public void setStartup(boolean startup) {
        this.startup = startup;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public List<String> getStartupExclusions() {
        return startupExclusions;
    }

    public void setStartupExclusions(List<String> startupExclusions) {
        this.startupExclusions = startupExclusions;
    }

    public List<String> getReconnectExclusions() {
        return reconnectExclusions;
    }

    public void setReconnectExclusions(List<String> reconnectExclusions) {
        this.reconnectExclusions = reconnectExclusions;
    }
}
