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
import jakarta.xml.bind.annotation.XmlAttribute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@XmlAccessorType(XmlAccessType.FIELD)
public class LengthFieldDecoding implements IDecodingStrategy {

    @XmlAttribute(name="header-nb-bytes-to-skip")
    private int headerNbBytesToSkip = 0; // in bytes

    @XmlAttribute(name="field-length", required = true)
    private int fieldLength = 0; // in bytes, max is 8

    // Endianness of the length field
    @XmlAttribute(name="big-endian")
    private boolean bigEndian = true;

    @XmlAttribute(name="field-mask")
    private long fieldMask = 0xFFFFFFFFFFFFFFFFL;

    @XmlAttribute(name="field-right-shift")
    private int fieldRightShift = 0;

    @XmlAttribute(name="field-value-offset")
    private int fieldValueOffset = 0;

    // If true, then the derived length considers also the skipped bytes, which must be then removed to complete the
    // reading of the message
    @XmlAttribute(name="consider-skipped-bytes")
    private boolean considerSkippedBytes = false;

    // If true, then the derived length considers also the field length, whose length must be then removed to complete the
    // reading of the message
    @XmlAttribute(name="consider-field-length")
    private boolean considerFieldLength = false;

    public int getHeaderNbBytesToSkip() {
        return headerNbBytesToSkip;
    }

    public void setHeaderNbBytesToSkip(int headerNbBytesToSkip) {
        this.headerNbBytesToSkip = headerNbBytesToSkip;
    }

    public int getFieldLength() {
        return fieldLength;
    }

    public void setFieldLength(int fieldLength) {
        this.fieldLength = fieldLength;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public void setBigEndian(boolean bigEndian) {
        this.bigEndian = bigEndian;
    }

    public long getFieldMask() {
        return fieldMask;
    }

    public void setFieldMask(long fieldMask) {
        this.fieldMask = fieldMask;
    }

    public int getFieldRightShift() {
        return fieldRightShift;
    }

    public void setFieldRightShift(int fieldRightShift) {
        this.fieldRightShift = fieldRightShift;
    }

    public int getFieldValueOffset() {
        return fieldValueOffset;
    }

    public void setFieldValueOffset(int fieldValueOffset) {
        this.fieldValueOffset = fieldValueOffset;
    }

    public boolean isConsiderSkippedBytes() {
        return considerSkippedBytes;
    }

    public void setConsiderSkippedBytes(boolean considerSkippedBytes) {
        this.considerSkippedBytes = considerSkippedBytes;
    }

    public boolean isConsiderFieldLength() {
        return considerFieldLength;
    }

    public void setConsiderFieldLength(boolean considerFieldLength) {
        this.considerFieldLength = considerFieldLength;
    }

    @Override
    public byte[] readMessage(InputStream is, AbstractConnectionConfiguration configuration) throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        // Read (and skip) the headerNbBytesToSkip bytes, if > 0
        if(this.headerNbBytesToSkip > 0) {
            byte[] skipBytes = is.readNBytes(this.headerNbBytesToSkip);
            if (skipBytes.length == 0) {
                throw new IOException("End of stream when reading the header bytes to skip");
            }
            buff.write(skipBytes);
        }
        if(this.fieldLength == 0) {
            throw new IOException("Wrong configuration: field-length set to 0, it must be set");
        }
        // Read fieldLength bytes
        byte[] lengthField = is.readNBytes(this.fieldLength);
        if(lengthField.length == 0) {
            throw new IOException("End of stream when reading the length field");
        }
        buff.write(lengthField);
        // Depending on the endianness, compute the number
        long lengthValue = 0;
        if(bigEndian) {
            // Big Endian
            for(int i = 0; i < lengthField.length; ++i) {
                lengthValue |= (lengthField[i] & 0x000000FF);
                if(i != lengthField.length - 1) {
                    lengthValue <<= 8;
                }
            }
        } else {
            // Little Endian
            for(int i = lengthField.length - 1; i >= 0; --i) {
                lengthValue |= (lengthField[i] & 0x000000FF);
                if(i != 0) {
                    lengthValue <<= 8;
                }
            }
        }
        // Apply mask
        lengthValue &= this.fieldMask;
        // Apply right shift
        lengthValue >>= this.fieldRightShift;
        // Add offset
        lengthValue += this.fieldValueOffset;
        // Now lengthValue contains the number of bytes that specify the length of the data
        if(this.considerSkippedBytes) {
            lengthValue -= this.headerNbBytesToSkip;
        }
        if(this.considerFieldLength) {
            lengthValue -= this.fieldLength;
        }
        // Read the message
        byte[] restOfMessage = is.readNBytes((int) lengthValue);
        if(restOfMessage.length == 0) {
            throw new IOException("End of stream when reading the message (" + lengthValue + " bytes)");
        }
        buff.write(restOfMessage);
        // Return the message
        return buff.toByteArray();
    }
}
