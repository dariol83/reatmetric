package eu.dariolucia.reatmetric.driver.socket.configuration;

import java.io.IOException;
import java.io.InputStream;

public interface IDecodingStrategy {

    byte[] readMessage(InputStream is, ConnectionConfiguration configuration) throws IOException;

}
