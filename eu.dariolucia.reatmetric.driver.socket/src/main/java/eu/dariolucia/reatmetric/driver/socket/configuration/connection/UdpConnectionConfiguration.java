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

@XmlAccessorType(XmlAccessType.FIELD)
public class UdpConnectionConfiguration extends AbstractConnectionConfiguration {

    public UdpConnectionConfiguration() {
        //
    }

    @Override
    public ConnectionType getType() {
        return ConnectionType.UDP;
    }

    /* ***************************************************************
     * Channel operations
     * ***************************************************************/

    private volatile DatagramSocket socket;

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.socket != null) {
            return;
        }
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
    }

    @Override
    public synchronized void closeConnection() {
        if(this.socket == null) {
            return;
        }
        this.socket.close();
        this.socket = null;
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

    @Override
    public byte[] readMessage() throws IOException {
        if(isOpen()) {
            DatagramSocket s = this.socket;
            if(s != null) {
                byte[] buffer = new byte[65536];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, 0, buffer.length);
                try {
                    s.receive(receivedPacket);
                    return Arrays.copyOfRange(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getOffset() + receivedPacket.getLength());
                } catch (SocketTimeoutException e) {
                    return null;
                }
            } else {
                throw new IOException("No socket available");
            }
        } else {
            throw new IOException("Channel is not open");
        }
    }

    @Override
    public synchronized boolean isOpen() {
        return this.socket != null;
    }

}
