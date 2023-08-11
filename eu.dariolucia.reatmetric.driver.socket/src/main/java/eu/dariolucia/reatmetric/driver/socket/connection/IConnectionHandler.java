package eu.dariolucia.reatmetric.driver.socket.connection;

import eu.dariolucia.reatmetric.driver.socket.configuration.ConnectionConfiguration;

import java.io.IOException;
import java.net.SocketException;

/**
 * This interface implements an abstract way to handle a connection, regardless of the underlying technology and protocol.
 */
public interface IConnectionHandler {
    ConnectionConfiguration getConfiguration();

    void openConnection() throws IOException;

    void closeConnection() throws IOException;

    boolean writeMessage(byte[] message) throws IOException;

    byte[] readMessage() throws IOException;

    boolean isOpen();
}
