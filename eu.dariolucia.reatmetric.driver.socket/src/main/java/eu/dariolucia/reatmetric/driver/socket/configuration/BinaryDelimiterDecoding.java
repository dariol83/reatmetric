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

package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This decoder reads all characters in the underlying channel and stops when the specified delimiter is found.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BinaryDelimiterDecoding implements IDecodingStrategy {

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(ByteArrayToStringAdapter.class)
    private byte[] startSequence;

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(ByteArrayToStringAdapter.class)
    private byte[] endSequence;

    @Override
    public byte[] readMessage(InputStream is, ConnectionConfiguration configuration) throws IOException {
        // Allocate a temporary buffer
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        // Reset internals
        int currentDetectionStartIndex = 0;
        boolean startSequenceEncountered = false;
        // Start reading
        boolean endOfStreamReached = false;
        // Look for the start of the message
        while (!endOfStreamReached && !startSequenceEncountered) {
            // Read one byte, until the end of the stream
            int byteRead = is.read();
            if (byteRead == -1) {
                // End of stream
                endOfStreamReached = true;
            } else {
                byte theByte = (byte) (byteRead & 0xFF);
                // Check if the byte is equal to the byte pointed by currentDetectionStartIndex
                if(theByte == this.startSequence[currentDetectionStartIndex]) {
                    // Increment the currentDetectionStartIndex by one
                    currentDetectionStartIndex++;
                    // Check if the start sequence is now fully encountered
                    if(currentDetectionStartIndex == this.startSequence.length) {
                        startSequenceEncountered = true;
                    }
                    // Add the byte to the output
                    buff.write(theByte);
                } else {
                    // Reset the currentDetectionStartIndex
                    currentDetectionStartIndex = 0;
                    // Flush the buffer
                    buff.reset();
                }
            }
        }
        if(startSequenceEncountered) {
            int currentDetectionEndIndex = 0;
            // Look for the end of the message
            while (!endOfStreamReached) {
                // Read one byte, until the end of the stream
                int byteRead = is.read();
                if (byteRead == -1) {
                    // End of stream
                    endOfStreamReached = true;
                } else {
                    byte theByte = (byte) (byteRead & 0xFF);
                    // Check if the byte is equal to the byte pointed by currentDetectionEndIndex
                    if(theByte == this.endSequence[currentDetectionEndIndex]) {
                        // Increment the currentDetectionEndIndex by one
                        currentDetectionEndIndex++;
                        // Check if a full message must be returned after this byte
                        if(currentDetectionEndIndex == this.endSequence.length) {
                            endOfStreamReached = true;
                        }
                    } else {
                        // Reset the currentDetectionIndex
                        currentDetectionEndIndex = 0;
                    }
                    // Add the byte to the output
                    buff.write(theByte);
                }
            }
        }
        // Return the byte array
        return buff.toByteArray();
    }
}
