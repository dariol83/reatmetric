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

package eu.dariolucia.reatmetric.driver.socket.configuration;

import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.TcpClientConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.TcpServerConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.UdpConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.AsciiMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.BinaryMessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition;
import eu.dariolucia.reatmetric.driver.socket.configuration.protocol.ProtocolConfiguration;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "socket", namespace = "http://dariolucia.eu/reatmetric/driver/socket")
@XmlAccessorType(XmlAccessType.FIELD)
public class SocketConfiguration {
    public static SocketConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SocketConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SocketConfiguration sc = (SocketConfiguration) u.unmarshal(is);
            sc.initialise();
            return sc;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElementWrapper(name = "connections")
    @XmlElements({
            @XmlElement(name="tcp-server",type= TcpServerConnectionConfiguration.class),
            @XmlElement(name="tcp-client",type= TcpClientConnectionConfiguration.class),
            @XmlElement(name="udp",type= UdpConnectionConfiguration.class)
    })
    private List<AbstractConnectionConfiguration> connections = new LinkedList<>();

    @XmlElementWrapper(name = "messages")
    @XmlElements({
            @XmlElement(name="ascii",type= AsciiMessageDefinition.class),
            @XmlElement(name="binary",type= BinaryMessageDefinition.class)
    })
    private List<MessageDefinition<?>> messageDefinitions = null;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private List<ProtocolConfiguration> protocols = new LinkedList<>();

    private void initialise() {
        // TODO
    }
}
