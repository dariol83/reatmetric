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
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class TcpClientConnectionConfiguration extends AbstractConnectionConfiguration {

    private static final Logger LOG = Logger.getLogger(TcpClientConnectionConfiguration.class.getName());

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
    private volatile boolean active;

    private volatile boolean running;
    private volatile Thread readingThread;

    @Override
    public synchronized void openConnection() {
        if(this.running) {
            return;
        }
        this.running = true;
        this.readingThread = new Thread(this::connectionLoop);
        this.readingThread.setDaemon(true);
        this.readingThread.start();
    }

    private void connectionLoop() {
        boolean suppressFailure = false;
        while(running) {
            try {
                establishConnection();
                // Reset suppression of error messages
                suppressFailure = false;
                try {
                    readAndForward();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, String.format("%s: error while reading from connection to %s:%d: %s", getName(), getHost(), getRemotePort(), e.getMessage()), e);
                    // Cleanup and retry immediately
                    cleanup();
                }
            } catch (Exception e) {
                if(!suppressFailure) {
                    LOG.log(Level.WARNING, String.format("%s: cannot establish connection to %s:%d: %s", getName(), getHost(), getRemotePort(), e.getMessage()), e);
                    // Suppress failure, avoid spamming
                    suppressFailure = true;
                }
                // Wait and retry (hardcoded for now)
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
        }
        cleanup();
    }

    private void readAndForward() throws IOException {
        // If you are here, it means that the socket exists
        InputStream is = this.inputStream;
        if(is == null) {
            throw new IOException("No input stream from socket");
        }
        try {
            // Read the message
            byte[] message = getDecodingStrategy().readMessage(is, this);
            // At this stage, whatever other exception you might have, it is not related to the connection, so log and go ahead
            try {
                // If the protocol is ASCII, convert it to string and forward it to the route
                if (getProtocol() == ProtocolType.ASCII) {
                    String messageString = new String(message, getAsciiEncoding().getCharset());
                    getRoute().onAsciiMessageReceived(messageString);
                } else {
                    // Otherwise inform the route directly
                    getRoute().onBinaryMessageReceived(message);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Unexpected exception while processing message from connection " + getName() + ": " + e.getMessage());
            }
        } catch (SocketTimeoutException e) {
            // Just return, this method will be immediately called again
        }
        // If you have any other exception, then it is bad and the connection will be cleared
    }

    private void establishConnection() throws IOException {
        // If there is no socket
        if(this.socket == null) {
            try {
                Socket s = new Socket();
                if (getLocalPort() != 0) {
                    s.bind(new InetSocketAddress(getLocalPort()));
                }
                s.setSoTimeout(getTimeout());
                s.setKeepAlive(isTcpKeepAlive());
                s.setTcpNoDelay(isTcpNoDelay());
                if (getTxBuffer() > 0) {
                    s.setSendBufferSize(getTxBuffer());
                }
                if (getRxBuffer() > 0) {
                    s.setReceiveBufferSize(getRxBuffer());
                }
                s.connect(new InetSocketAddress(getHost(), getRemotePort()), getTimeout());
                this.socket = s;
                this.inputStream = this.socket.getInputStream();
                this.outputStream = this.socket.getOutputStream();
                this.active = true;
            } catch (IOException e) {
                cleanup();
                throw e;
            }
        }
    }

    private void cleanup() {
        // Clean-up
        if(this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if(this.outputStream != null) {
            try {
                this.outputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        if(this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
        this.active = false;
    }

    @Override
    public synchronized void closeConnection() {
        if(this.running) {
            this.running = false;
            this.readingThread.interrupt();
            try {
                this.readingThread.join();
            } catch (InterruptedException e) {
                // Ignore
            }
            this.readingThread = null;
        }
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
    public boolean isOpen() {
        return this.running;
    }

}
