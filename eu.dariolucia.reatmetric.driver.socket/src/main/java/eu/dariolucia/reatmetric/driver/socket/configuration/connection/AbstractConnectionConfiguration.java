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

package eu.dariolucia.reatmetric.driver.socket.configuration.connection;

import eu.dariolucia.reatmetric.driver.socket.configuration.decoding.*;
import eu.dariolucia.reatmetric.driver.socket.configuration.protocol.RouteConfiguration;
import jakarta.xml.bind.annotation.*;

import java.io.IOException;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class AbstractConnectionConfiguration {

    @XmlID
    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "source", required = true)
    private String source;

    @XmlAttribute
    private InitType init = InitType.CONNECTOR;

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

    @XmlElements({
            @XmlElement(name="datagramDecoding",type= DatagramDecoding.class),
            @XmlElement(name="fixedLengthDecoding",type= FixedLengthDecoding.class),
            @XmlElement(name="asciiDelimiterDecoding",type= AsciiDelimiterDecoding.class),
            @XmlElement(name="lengthFieldDecoding",type= LengthFieldDecoding.class),
            @XmlElement(name="binaryDelimiterDecoding",type= BinaryDelimiterDecoding.class)
    })
    private IDecodingStrategy decodingStrategy;

    @XmlElement(required = true)
    private RouteConfiguration route;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract ConnectionType getType();

    public InitType getInit() {
        return init;
    }

    public void setInit(InitType init) {
        this.init = init;
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

    public IDecodingStrategy getDecodingStrategy() {
        return decodingStrategy;
    }

    public void setDecodingStrategy(IDecodingStrategy decodingStrategy) {
        this.decodingStrategy = decodingStrategy;
    }

    public RouteConfiguration getRoute() {
        return route;
    }

    public void setRoute(RouteConfiguration route) {
        this.route = route;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /* ***************************************************************
     * Channel operations
     * ***************************************************************/

    public abstract void openConnection() throws IOException;

    public abstract void closeConnection() throws IOException;

    public abstract boolean writeMessage(byte[] message) throws IOException;

    public abstract byte[] readMessage() throws IOException; // TODO: remove

    public abstract boolean isOpen();
}
