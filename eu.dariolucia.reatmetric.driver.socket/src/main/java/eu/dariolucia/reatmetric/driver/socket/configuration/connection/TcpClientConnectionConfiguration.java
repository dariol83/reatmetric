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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcpClientConnectionConfiguration extends AbstractConnectionConfiguration {

    @XmlAttribute(name = "tcp-keep-alive")
    private boolean tcpKeepAlive = false;

    @XmlAttribute(name = "tcp-no-delay")
    private boolean tcpNoDelay = false;

    public TcpClientConnectionConfiguration() {
        //
    }

    @Override
    public ConnectionType getType() {
        return ConnectionType.TCP;
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

    /* ***************************************************************
     * Channel operations
     * ***************************************************************/

    private volatile Socket socket;
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.socket != null) {
            return;
        }
        Socket s = new Socket();
        if(getLocalPort() != 0) {
            s.bind(new InetSocketAddress(getLocalPort()));
        }
        s.setKeepAlive(isTcpKeepAlive());
        s.setTcpNoDelay(isTcpNoDelay());
        if(getTxBuffer() > 0) {
            s.setSendBufferSize(getTxBuffer());
        }
        if(getRxBuffer() > 0) {
            s.setReceiveBufferSize(getRxBuffer());
        }
        s.connect(new InetSocketAddress(getHost(), getRemotePort()), getTimeout());
        this.socket = s;
        this.inputStream = this.socket.getInputStream();
        this.outputStream = this.socket.getOutputStream();
    }

    @Override
    public synchronized void closeConnection() throws IOException {
        if(this.socket == null) {
            return;
        }
        this.socket.close();
        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
    }

    @Override
    public boolean writeMessage(byte[] message) throws IOException {
        if(isOpen()) {
            OutputStream os = this.outputStream;
            if(os != null) {
                os.write(message);
                os.flush();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public byte[] readMessage() throws IOException {
        if(isOpen()) {
            InputStream is = this.inputStream;
            if(is != null) {
                return getDecodingStrategy().readMessage(is, this);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public synchronized boolean isOpen() {
        return this.socket != null;
    }

}
