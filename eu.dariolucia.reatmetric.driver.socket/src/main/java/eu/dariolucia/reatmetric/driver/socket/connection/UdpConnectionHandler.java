/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.socket.connection;

import eu.dariolucia.reatmetric.driver.socket.configuration.ConnectionConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;

public class UdpConnectionHandler extends AbstractConnectionHandler {

    private volatile DatagramSocket socket;

    public UdpConnectionHandler(ConnectionConfiguration configuration) {
        super(configuration);
    }

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.socket != null) {
            return;
        }
        DatagramSocket s = new DatagramSocket();
        if(getConfiguration().getLocalPort() != 0) {
            s.bind(new InetSocketAddress(getConfiguration().getLocalPort()));
        }
        if(getConfiguration().getTxBuffer() > 0) {
            s.setSendBufferSize(getConfiguration().getTxBuffer());
        }
        if(getConfiguration().getRxBuffer() > 0) {
            s.setReceiveBufferSize(getConfiguration().getRxBuffer());
        }
        s.connect(InetAddress.getByName(getConfiguration().getHost()), getConfiguration().getRemotePort());
        this.socket = s;
    }

    @Override
    public synchronized void closeConnection() throws IOException {
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
