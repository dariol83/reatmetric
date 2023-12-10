/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

@XmlAccessorType(XmlAccessType.FIELD)
public class SecurityDataLinkConfiguration {

    @XmlAttribute(name = "handler", required = true)
    private String handler = "";

    /**
     * This element contains ;-separated entry, each with :-separated key-value pairs.
     * The keys are either: an integer number > 0, or the string SALT.
     * The values are: strings for integer number keys, a sequence of hex-encoded bytes for SALT key
     */
    @XmlElement(name = "configuration", required = true)
    private String configuration = "";

    public String getHandler() {
        return handler;
    }

    public SecurityDataLinkConfiguration setHandler(String handler) {
        this.handler = handler;
        return this;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
