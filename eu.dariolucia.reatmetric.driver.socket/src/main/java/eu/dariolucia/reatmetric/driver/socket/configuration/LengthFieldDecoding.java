package eu.dariolucia.reatmetric.driver.socket.configuration;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@XmlAccessorType(XmlAccessType.FIELD)
public class LengthFieldDecoding implements IDecodingStrategy {

    @XmlAttribute
    private int headerNbBytesToSkip = 0; // in bytes

    @XmlAttribute
    private int fieldLength = 0; // in bytes, max is 8

    // Endianness of the length field
    @XmlAttribute
    private boolean bigEndian = true;

    @XmlAttribute
    private long fieldMask = 0xFFFFFFFFFFFFFFFFL;

    @XmlAttribute
    private int fieldRightShift = 0;

    @XmlAttribute
    private int fieldValueOffset = 0;

    // If true, then the derived length considers also the skipped bytes, which must be then removed to complete the
    // reading of the message
    @XmlAttribute
    private boolean considerSkippedBytes = false;

    // If true, then the derived length considers also the field length, whose length must be then removed to complete the
    // reading of the message
    @XmlAttribute
    private boolean considerFieldLength = false;

    @Override
    public byte[] readMessage(InputStream is, ConnectionConfiguration configuration) throws IOException {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        // Read (and skip) the headerNbBytesToSkip bytes
        buff.write(is.readNBytes(this.headerNbBytesToSkip));
        // Read fieldLength bytes
        byte[] lengthField = is.readNBytes(this.fieldLength);
        buff.write(lengthField);
        // Depending on the endianness, compute the number
        long lengthValue = 0;
        if(bigEndian) {
            // Big Endian
            for(int i = 0; i < lengthField.length; ++i) {
                lengthValue |= (lengthField[i] & 0x000000FF);
                lengthValue <<= 8;
            }
        } else {
            // Little Endian
            for(int i = lengthField.length - 1; i >= 0; --i) {
                lengthValue |= (lengthField[i] & 0x000000FF);
                lengthValue <<= 8;
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
        buff.write(is.readNBytes((int) lengthValue));
        // Return the message
        return buff.toByteArray();
    }
}
