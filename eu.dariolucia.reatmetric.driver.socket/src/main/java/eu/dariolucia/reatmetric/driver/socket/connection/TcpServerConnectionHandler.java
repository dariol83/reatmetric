package eu.dariolucia.reatmetric.driver.socket.connection;

import eu.dariolucia.reatmetric.driver.socket.configuration.ConnectionConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class TcpServerConnectionHandler extends AbstractConnectionHandler {

    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;

    public TcpServerConnectionHandler(ConnectionConfiguration configuration) {
        super(configuration);
    }

    @Override
    public synchronized void openConnection() throws IOException {
        if(this.server != null) {
            return;
        }
        this.server = new ServerSocket(getConfiguration().getLocalPort(), 1);
        this.server.setSoTimeout(getConfiguration().getTimeout());
        if(getConfiguration().getRxBuffer() > 0) {
            this.server.setReceiveBufferSize(getConfiguration().getRxBuffer());
        }
        initConnection(true);
        // Now, at next operation we accept the connection and we cache it
    }

    private synchronized void initConnection(boolean silent) throws IOException {
        // Wait for connection
        if(this.socket == null) {
            try {
                this.socket = this.server.accept();
                this.socket.setSoTimeout(getConfiguration().getTimeout());
                this.socket.setKeepAlive(getConfiguration().isTcpKeepAlive());
                this.socket.setTcpNoDelay(getConfiguration().isTcpNoDelay());
                if (getConfiguration().getTxBuffer() > 0) {
                    this.socket.setSendBufferSize(getConfiguration().getTxBuffer());
                }
                if (getConfiguration().getRxBuffer() > 0) {
                    this.socket.setReceiveBufferSize(getConfiguration().getRxBuffer());
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
                    return getConfiguration().getDecodingStrategy().readMessage(is, getConfiguration());
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
