package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

import java.io.InputStream;

@XmlAccessorType(XmlAccessType.FIELD)
public class DatagramDecoding implements IDecodingStrategy {

    @Override
    public byte[] readMessage(InputStream is, ConnectionConfiguration configuration) {
        throw new UnsupportedOperationException("This operation shall never be called");
    }
}
