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

package eu.dariolucia.reatmetric.remoting.connector.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ConnectorConfiguration {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "host", required = true)
    private String host;

    @XmlAttribute(name = "port", required = true)
    private int port;

    public ConnectorConfiguration() {
    }

    public String getName() {
        return name;
    }

    public ConnectorConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public String getHost() {
        return host;
    }

    public ConnectorConfiguration setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public ConnectorConfiguration setPort(int port) {
        this.port = port;
        return this;
    }
}
