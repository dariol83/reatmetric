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
import eu.dariolucia.ccsds.encdec.extension.IDecoderExtension;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.PathLocation;

import java.nio.charset.StandardCharsets;

@ExtensionId(id = "LengthPaddedUsAsciiString")
public class LengthPaddedUsAsciiStringDecoder implements IDecoderExtension {

    @Override
    public Object decode(PacketDefinition definition, EncodedParameter parameter, PathLocation location, BitEncoderDecoder decoder) throws DecodingException {
        int length = decoder.getNextIntegerUnsigned(32);
        if(length == 0) {
            return "";
        } else {
            byte[] readAscii = decoder.getNextByte(length * 8);
            // Read also the padding if any
            int rest = (length % 4);
            if(rest > 0) {
                int pad = 4 - rest;
                decoder.getNextByte(pad * 8);
            }
            // Convert to string
            return new String(readAscii, StandardCharsets.US_ASCII);
        }
    }
}
