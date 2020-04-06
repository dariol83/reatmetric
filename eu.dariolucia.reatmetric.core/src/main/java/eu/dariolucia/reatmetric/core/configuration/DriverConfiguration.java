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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DriverConfiguration {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "configuration", required = true)
    private String configuration;

    public DriverConfiguration() {
    }

    public String getName() {
        return name;
    }

    public DriverConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public DriverConfiguration setType(String type) {
        this.type = type;
        return this;
    }

    public String getConfiguration() {
        return configuration;
    }

    public DriverConfiguration setConfiguration(String configuration) {
        this.configuration = configuration;
        return this;
    }
}
