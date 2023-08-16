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
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClientConnectionHandler extends AbstractConnectionHandler {

    private volatile Socket socket;
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    public TcpClientConnectionHandler(ConnectionConfiguration configuration) {
        super(configuration);
    }

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.socket != null) {
            return;
        }
        Socket s = new Socket();
        if(getConfiguration().getLocalPort() != 0) {
            s.bind(new InetSocketAddress(getConfiguration().getLocalPort()));
        }
        s.setKeepAlive(getConfiguration().isTcpKeepAlive());
        s.setTcpNoDelay(getConfiguration().isTcpNoDelay());
        if(getConfiguration().getTxBuffer() > 0) {
            s.setSendBufferSize(getConfiguration().getTxBuffer());
        }
        if(getConfiguration().getRxBuffer() > 0) {
            s.setReceiveBufferSize(getConfiguration().getRxBuffer());
        }
        s.connect(new InetSocketAddress(getConfiguration().getHost(), getConfiguration().getRemotePort()), getConfiguration().getTimeout());
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
                return getConfiguration().getDecodingStrategy().readMessage(is, getConfiguration());
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
