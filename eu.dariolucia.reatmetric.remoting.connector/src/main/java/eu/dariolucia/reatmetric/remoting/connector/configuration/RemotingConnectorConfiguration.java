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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "connectors", namespace = "http://dariolucia.eu/reatmetric/remoting/connector/configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class RemotingConnectorConfiguration {

    public static RemotingConnectorConfiguration load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(RemotingConnectorConfiguration.class);
        Unmarshaller u = jc.createUnmarshaller();
        RemotingConnectorConfiguration configuration = (RemotingConnectorConfiguration) u.unmarshal(is);
        return configuration;
    }

    @XmlElement(name = "connector", required = true)
    private List<ConnectorConfiguration> connectors = new LinkedList<>();

    public RemotingConnectorConfiguration() {
    }

    public List<ConnectorConfiguration> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<ConnectorConfiguration> connectors) {
        this.connectors = connectors;
    }
}
