package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This decoder reads all characters in the underlying channel and stops when the specified delimiter is found.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AsciiDelimiterDecoding implements IDecodingStrategy {

    @XmlAttribute(required = true)
    private String delimiterCharacters;

    private transient byte[] delimiterSequence;

    @Override
    public byte[] readMessage(InputStream is, ConnectionConfiguration configuration) throws IOException {
        // First of all, convert the delimiterCharacters into the equivalent byte array
        convertDelimiter(configuration);
        // Allocate a temporary buffer
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        // Reset internals
        int currentDetectionIndex = 0;
        // Start reading
        boolean endOfStreamReached = false;
        while (!endOfStreamReached) {
            // Read one byte, until the end of the stream
            int byteRead = is.read();
            if (byteRead == -1) {
                // End of stream
                endOfStreamReached = true;
            } else {
                byte theByte = (byte) (byteRead & 0xFF);
                // Check if the byte is equal to the byte pointed by currentDetectionIndex
                if(theByte == this.delimiterSequence[currentDetectionIndex]) {
                    // Increment the currentDetectionIndex by one
                    currentDetectionIndex++;
                    // Check if a full message must be returned after this byte
                    if(currentDetectionIndex == this.delimiterSequence.length) {
                        endOfStreamReached = true;
                    }
                } else {
                    // Reset the currentDetectionIndex
                    currentDetectionIndex = 0;
                }
                // Add the byte to the output
                buff.write(theByte);
            }
        }
        // Return the byte array
        return buff.toByteArray();
    }

    private void convertDelimiter(ConnectionConfiguration configuration) {
        if(this.delimiterSequence == null) {
            this.delimiterSequence = delimiterCharacters.getBytes(configuration.getAsciiEncoding().getCharset());
        }
    }
}
