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

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class UdpConnectionConfiguration extends AbstractConnectionConfiguration {

    private static final Logger LOG = Logger.getLogger(UdpConnectionConfiguration.class.getName());

    @Override
    public ConnectionType getType() {
        return ConnectionType.UDP;
    }

    /* ***************************************************************
     * Channel operations
     * ***************************************************************/

    private transient volatile DatagramSocket socket;
    private transient final byte[] buffer = new byte[65536];

    @Override
    protected void connectionLoop() {
        boolean suppressFailure = false;
        while(isOpen()) {
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
        DatagramSocket is = this.socket;
        if(is == null) {
            throw new IOException("No datagram socket found");
        }
        try {
            // Read the message
            byte[] message = readFromSocket(this.socket);
            // At this stage, whatever other exception you might have, it is not related to the connection, so log and go ahead
            forwardToRoute(message);
        } catch (SocketTimeoutException e) {
            // Just return, this method will be immediately called again
        }
        // If you have any other exception, then it is bad and the connection will be cleared
    }

    private byte[] readFromSocket(DatagramSocket s) throws IOException {
        if(s != null) {
            DatagramPacket receivedPacket = new DatagramPacket(buffer, 0, buffer.length);
            s.receive(receivedPacket);
            return Arrays.copyOfRange(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getOffset() + receivedPacket.getLength());
        } else {
            throw new IOException("No socket available");
        }
    }

    private void establishConnection() throws IOException {
        // If there is no socket
        if(this.socket == null) {
            try {
                DatagramSocket s = new DatagramSocket();
                if(getLocalPort() != 0) {
                    s.bind(new InetSocketAddress(getLocalPort()));
                }
                if(getTxBuffer() > 0) {
                    s.setSendBufferSize(getTxBuffer());
                }
                if(getRxBuffer() > 0) {
                    s.setReceiveBufferSize(getRxBuffer());
                }
                s.connect(InetAddress.getByName(getHost()), getRemotePort());
                this.socket = s;
                setActive(true);
            } catch (IOException e) {
                cleanup();
                throw e;
            }
        }
    }

    private void cleanup() {
        // Clean-up
        if(this.socket != null) {
            this.socket.close();
        }
        this.socket = null;
        setActive(false);
    }

    @Override
    public boolean writeMessage(byte[] message) throws IOException {
        if(isOpen()) {
            DatagramSocket s = this.socket;
            if(s != null) {
                s.send(new DatagramPacket(message, 0, message.length));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
