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

package eu.dariolucia.reatmetric.driver.socket.types;

import eu.dariolucia.ccsds.encdec.bit.BitEncoderDecoder;
import eu.dariolucia.ccsds.encdec.definition.EncodedParameter;
import eu.dariolucia.ccsds.encdec.definition.PacketDefinition;
import eu.dariolucia.ccsds.encdec.extension.ExtensionId;
import eu.dariolucia.ccsds.encdec.extension.IEncoderExtension;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.PathLocation;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ExtensionId(id = "LengthPaddedUsAsciiString")
public class LengthPaddedUsAsciiStringEncoder implements IEncoderExtension {
    @Override
    public void encode(PacketDefinition definition, EncodedParameter parameter, PathLocation location, BitEncoderDecoder encoder, Object value) throws EncodingException {
        String toString = Objects.toString(value, "");
        byte[] asciiString = toString.getBytes(StandardCharsets.US_ASCII);
        // Write 4 bytes, length of string
        encoder.setNextIntegerUnsigned(asciiString.length, 32);
        // Write the bytes of the string
        encoder.setNextByte(asciiString, asciiString.length * 8);
        // Compute how many padding bites you need to add
        int rest = (asciiString.length % 4);
        if(rest > 0) {
            int toPad = 4 - rest;
            encoder.setNextByte(new byte[toPad], toPad * 8);
        }
        // Done
    }
}
