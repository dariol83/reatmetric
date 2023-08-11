package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.io.IOException;
import java.io.InputStream;

/**
 * This decoder reads a fixed number of bytes from the underlying channel.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FixedLengthDecoding implements IDecodingStrategy {

    @XmlAttribute(required = true)
    private int length;

    @Override
    public byte[] readMessage(InputStream is, ConnectionConfiguration configuration) throws IOException {
        return is.readNBytes(length);
    }
}
