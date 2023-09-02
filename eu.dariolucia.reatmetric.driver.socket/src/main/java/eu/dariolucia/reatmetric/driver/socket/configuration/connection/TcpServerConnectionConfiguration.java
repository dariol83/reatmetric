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

import jakarta.xml.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcpServerConnectionConfiguration extends AbstractConnectionConfiguration {

    @XmlAttribute(name = "tcp-keep-alive")
    private boolean tcpKeepAlive = false;

    @XmlAttribute(name = "tcp-no-delay")
    private boolean tcpNoDelay = false;

    public TcpServerConnectionConfiguration() {
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

    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.server != null) {
            return;
        }
        this.server = new ServerSocket(getLocalPort(), 1);
        this.server.setSoTimeout(getTimeout());
        if(getRxBuffer() > 0) {
            this.server.setReceiveBufferSize(getRxBuffer());
        }
        initConnection(true);
        // Now, at next operation we accept the connection and we cache it
    }

    private synchronized void initConnection(boolean silent) throws IOException {
        // Wait for connection
        if(this.socket == null) {
            try {
                this.socket = this.server.accept();
                this.socket.setSoTimeout(getTimeout());
                this.socket.setKeepAlive(isTcpKeepAlive());
                this.socket.setTcpNoDelay(isTcpNoDelay());
                if (getTxBuffer() > 0) {
                    this.socket.setSendBufferSize(getTxBuffer());
                }
                if (getRxBuffer() > 0) {
                    this.socket.setReceiveBufferSize(getRxBuffer());
                }
                this.inputStream = this.socket.getInputStream();
                this.outputStream = this.socket.getOutputStream();
            } catch (SocketTimeoutException e) {
                if(!silent) {
                    throw e;
                }
            }
        }
    }

    @Override
    public synchronized void closeConnection() throws IOException {
        if(this.server == null) {
            return;
        }
        this.server.close();
        this.server = null;
        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
    }

    @Override
    public boolean writeMessage(byte[] message) throws IOException {
        if(isOpen()) {
            initConnection(false);
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
            initConnection(false);
            InputStream is = this.inputStream;
            if(is != null) {
                try {
                    return getDecodingStrategy().readMessage(is, this);
                } catch (SocketTimeoutException e) {
                    return null;
                }
            } else {
                throw new IOException("No stream available");
            }
        } else {
            throw new IOException("Channel is not open");
        }
    }

    @Override
    public synchronized boolean isOpen() {
        return this.server != null;
    }

}
