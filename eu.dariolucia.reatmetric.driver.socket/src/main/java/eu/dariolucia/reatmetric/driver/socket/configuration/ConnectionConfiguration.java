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

import eu.dariolucia.reatmetric.driver.socket.connection.IConnectionHandler;
import eu.dariolucia.reatmetric.driver.socket.connection.TcpClientConnectionHandler;
import eu.dariolucia.reatmetric.driver.socket.connection.TcpServerConnectionHandler;
import eu.dariolucia.reatmetric.driver.socket.connection.UdpConnectionHandler;
import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class ConnectionConfiguration {

    @XmlID
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute
    private ConnectionType type = ConnectionType.TCP;

    @XmlAttribute
    private InitType init = InitType.CONNECTOR;

    @XmlAttribute
    private RoleType role = RoleType.CLIENT;

    @XmlAttribute(required = true)
    private ProtocolType protocol;

    // Only considered if protocol == ASCII
    @XmlAttribute(name = "ascii-encoding")
    private AsciiEncoding asciiEncoding = AsciiEncoding.US_ASCII;

    // Special value: * in case of role == SERVER to indicate bind on all interfaces
    @XmlAttribute(required = true)
    private String host;

    // Special value: 0 in case of role == CLIENT to indicate auto-assignment
    @XmlAttribute(required = true)
    private int localPort;

    // Special value: 0 in case of role == SERVER
    @XmlAttribute(required = true)
    private int remotePort;

    @XmlAttribute(name = "timeout")
    private int timeout = 5000; // timeout in ms

    @XmlAttribute(name = "tx-buffer")
    private int txBuffer = 0; // use OS default

    @XmlAttribute(name = "rx-buffer")
    private int rxBuffer = 0; // use OS default

    @XmlAttribute(name = "tcp-keep-alive")
    private boolean tcpKeepAlive = false;

    @XmlAttribute(name = "tcp-no-delay")
    private boolean tcpNoDelay = false;

    @XmlElements({
            @XmlElement(name="datagramDecoding",type= DatagramDecoding.class),
            @XmlElement(name="fixedLengthDecoding",type= FixedLengthDecoding.class),
            @XmlElement(name="asciiDelimiterDecoding",type= AsciiDelimiterDecoding.class),
            @XmlElement(name="lengthFieldDecoding",type= LengthFieldDecoding.class),
            @XmlElement(name="binaryDelimiterDecoding",type= BinaryDelimiterDecoding.class)
    })
    private IDecodingStrategy decodingStrategy;

    public ConnectionConfiguration() {
        //
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConnectionType getType() {
        return type;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    public InitType getInit() {
        return init;
    }

    public void setInit(InitType init) {
        this.init = init;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public AsciiEncoding getAsciiEncoding() {
        return asciiEncoding;
    }

    public void setAsciiEncoding(AsciiEncoding asciiEncoding) {
        this.asciiEncoding = asciiEncoding;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTxBuffer() {
        return txBuffer;
    }

    public void setTxBuffer(int txBuffer) {
        this.txBuffer = txBuffer;
    }

    public int getRxBuffer() {
        return rxBuffer;
    }

    public void setRxBuffer(int rxBuffer) {
        this.rxBuffer = rxBuffer;
    }

    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }

    public void setTcpKeepAlive(boolean tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public IDecodingStrategy getDecodingStrategy() {
        return decodingStrategy;
    }

    public void setDecodingStrategy(IDecodingStrategy decodingStrategy) {
        this.decodingStrategy = decodingStrategy;
    }

    /* ***************************************************************
     * Channel operations
     * ***************************************************************/

    private transient IConnectionHandler handler;

    /**
     * Factory method that returns an instance of the IConnectionHandler used to control the connection.
     *
     * @return the connection handler
     */
    public IConnectionHandler getConnectionHandler() {
        if(handler != null) {
            return handler;
        }
        if(type == ConnectionType.TCP) {
            if(role == RoleType.CLIENT) {
                handler = new TcpClientConnectionHandler(this);
            } else if(role == RoleType.SERVER) {
                handler = new TcpServerConnectionHandler(this);
            } else {
                throw new IllegalStateException("Role " + role + " for handler type TCP not supported");
            }
        } else if(type == ConnectionType.UDP) {
            handler = new UdpConnectionHandler(this);
        } else {
            throw new IllegalStateException("Handler type " + type + " not supported");
        }
        return handler;
    }
}
