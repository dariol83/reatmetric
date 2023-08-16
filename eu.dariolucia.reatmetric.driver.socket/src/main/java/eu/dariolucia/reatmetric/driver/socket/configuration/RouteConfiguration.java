/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class RouteConfiguration {

    @XmlAttribute(required = true)
    private String route;

    @XmlIDREF
    @XmlAttribute
    private ConnectionConfiguration defaultConnection = null;

    @XmlElement(name = "inbound")
    private List<InboundMessageMapping> inboundMessageMappings = new LinkedList<>();

    @XmlElement(name = "outbound")
    private List<OutboundMessageMapping> outboundMessageMappings = new LinkedList<>();

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public List<InboundMessageMapping> getInboundMessages() {
        return inboundMessageMappings;
    }

    public void setInboundMessages(List<InboundMessageMapping> inboundMessageMappings) {
        this.inboundMessageMappings = inboundMessageMappings;
    }

    public List<OutboundMessageMapping> getOutboundMessages() {
        return outboundMessageMappings;
    }

    public void setOutboundMessages(List<OutboundMessageMapping> outboundMessageMappings) {
        this.outboundMessageMappings = outboundMessageMappings;
    }

    public ConnectionConfiguration getDefaultConnection() {
        return defaultConnection;
    }

    public void setDefaultConnection(ConnectionConfiguration defaultConnection) {
        this.defaultConnection = defaultConnection;
    }
}
