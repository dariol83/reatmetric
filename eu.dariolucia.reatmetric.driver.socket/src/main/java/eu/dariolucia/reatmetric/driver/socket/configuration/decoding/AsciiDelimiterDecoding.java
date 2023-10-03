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

package eu.dariolucia.reatmetric.driver.socket.configuration.decoding;

import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This decoder reads all characters in the underlying channel and stops when the specified delimiter is found.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AsciiDelimiterDecoding implements IDecodingStrategy {

    @XmlElement(name="delimiter")
    private String delimiterCharacters;

    public String getDelimiterCharacters() {
        return delimiterCharacters;
    }

    public void setDelimiterCharacters(String delimiterCharacters) {
        this.delimiterCharacters = delimiterCharacters;
    }

    private transient byte[] delimiterSequence;

    @Override
    public byte[] readMessage(InputStream is, AbstractConnectionConfiguration configuration) throws IOException {
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

    private void convertDelimiter(AbstractConnectionConfiguration configuration) {
        if(this.delimiterSequence == null) {
            String delimiterCharactersReplaced = delimiterCharacters.replace("\\n", "\n")
                    .replace("\\r", "\r").replace("\\t", "\t");
            this.delimiterSequence = delimiterCharactersReplaced.getBytes(configuration.getAsciiEncoding().getCharset());
        }
    }
}
