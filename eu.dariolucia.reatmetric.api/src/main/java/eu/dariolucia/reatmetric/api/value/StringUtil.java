/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.value;

/**
 * This class contains a set of utility functions to work with String objects.
 */
public class StringUtil {

    private StringUtil() {
        // Private constructor
    }

    /**
     * Convert an hex dump string into a byte array.
     *
     * @param hexDump the hex dump string
     * @return the byte array
     */
    public static byte[] toByteArray(String hexDump) {
        int length = hexDump.length();
        byte[] toReturn = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            toReturn[i / 2] = (byte) ((Character.digit(hexDump.charAt(i), 16) << 4) + Character.digit(hexDump.charAt(i + 1), 16));
        }
        return toReturn;
    }

    // Stackoverflow snippet (Creative Common License):
    // https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convert a byte array into an hex dump string.
     *
     * @param data the byte array
     * @return the string (hex dump)
     */
    public static String toHexDump(byte[] data) {
        // Number of characters is twice the number of bytes, 0xdd
        char[] charOfOutput = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            // MSbits (4) value
            charOfOutput[i * 2] = HEX_ARRAY[v >>> 4];
            // LSbits (4) value
            charOfOutput[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(charOfOutput);
    }
}
