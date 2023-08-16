/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ProtocolConfiguration {

    @XmlAttribute(name = "entity-offset")
    private int entityOffset = 0;

    @XmlElement(name = "route")
    private List<RouteConfiguration> routes = new LinkedList<>();

    public List<RouteConfiguration> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteConfiguration> routes) {
        this.routes = routes;
    }

    // TODO
    // List of routes. For each route

        // List of inbound messages from a connection and what to do with each (parameters, events)

        // List of outbound messages to a connection - commands - with dispatch strategy
        // (activity-driver, automated with period) and ways of acknowledging
        // (activity-driver, automated with period) and ways of acknowledging
}
