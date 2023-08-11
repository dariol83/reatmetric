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
