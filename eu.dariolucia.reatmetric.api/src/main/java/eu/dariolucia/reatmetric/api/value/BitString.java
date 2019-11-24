/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.value;

import java.util.Arrays;
import java.util.Objects;

/**
 * A bit string class. A bit string is defined by a byte[] and the number of bits delivering the information.
 */
public class BitString {

    private final byte[] data;
    private final int length;

    /**
     * Construct a bit string from the provided byte[], and considering the specified number of bits.
     *
     * @param data the array containing the information
     * @param length the length of the bit string in bits
     */
    public BitString(byte[] data, int length) {
        if(data.length * 8 < length) {
            throw new IllegalArgumentException("Number of bits in provided array is less than the length in bits of the " +
                    "BitStream: data length " + data.length + ", BitStream length " + length);
        }
        this.data = data;
        this.length = length;
    }

    /**
     * Return the underlying data.
     *
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Return the length of the bit string in bits.
     *
     * @return the length of the bit string in bits
     */
    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitString bitString = (BitString) o;
        return length == bitString.length &&
                Arrays.equals(data, bitString.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(length);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; ++i) {
            int byteIdx = i / Byte.SIZE;
            int bitIdx = i % Byte.SIZE;
            int val = this.data[byteIdx] & ((byte) ((1 << (7 - bitIdx)) & 0xFF));
            sb.append(val == 0 ? "0" : "1");
        }
        return sb.toString();
    }

    /**
     * This method parses a BitString encoded as a string. The expected format is a sequence of 0s and 1s. The data will
     * be converted into a byte array, and the length of the BitString will be set equal to the length of the string.
     *
     * The first character (s.charAt(0)) is the most significant bit.
     *
     * @param s the BitString encoded as string of 0s and 1s
     * @return the BitString object
     * @throws IllegalArgumentException if the format is not the expected one
     */
    public static BitString parse(String s) {
        byte[] data = new byte[(int) Math.ceil(s.length() / 8.0)];
        int length = s.length();
        int currentByteIdx = 0;
        int currentBitIdx = 0;
        for(int i = 0; i < s.length(); ++i) {
            // read the char
            char theChar = s.charAt(i);
            switch (theChar) {
                case '0':
                    // Do not set anything
                    // Advance
                    ++currentBitIdx;
                    if(currentBitIdx == 8) {
                        currentBitIdx = 0;
                        ++currentByteIdx;
                    }
                    break;
                case '1':
                    // Set the bit currentBitIdx at byte currentByteIdx to 1
                    data[currentByteIdx] |= (byte) ((1 << (7 - currentBitIdx)) & 0xFF);
                    // Advance
                    ++currentBitIdx;
                    if(currentBitIdx == 8) {
                        currentBitIdx = 0;
                        ++currentByteIdx;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Cannot parse string: " + s);
            }
        }
        return new BitString(data, length);
    }
}
