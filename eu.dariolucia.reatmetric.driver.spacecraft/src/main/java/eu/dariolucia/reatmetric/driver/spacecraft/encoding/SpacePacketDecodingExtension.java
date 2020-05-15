/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.encoding;

import eu.dariolucia.ccsds.encdec.bit.BitEncoderDecoder;
import eu.dariolucia.ccsds.encdec.definition.EncodedParameter;
import eu.dariolucia.ccsds.encdec.definition.PacketDefinition;
import eu.dariolucia.ccsds.encdec.extension.ExtensionId;
import eu.dariolucia.ccsds.encdec.extension.IDecoderExtension;
import eu.dariolucia.ccsds.encdec.extension.IEncoderExtension;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.PathLocation;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;

import java.nio.ByteBuffer;

@ExtensionId(id="eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket")
public class SpacePacketDecodingExtension implements IDecoderExtension {

    @Override
    public Object decode(PacketDefinition definition, EncodedParameter parameter, PathLocation location, BitEncoderDecoder decoder) throws DecodingException {
        try {
            // Remember the decoder position
            int decPos = decoder.getCurrentBitIndex();
            // Read and interpret the packet header, get the length
            byte[] spacePacketHeader = decoder.getNextByte(SpacePacket.SP_PRIMARY_HEADER_LENGTH * Byte.SIZE);
            // Reset the position, read the packet bytes
            decoder.setCurrentBitIndex(decPos);
            ByteBuffer bb = ByteBuffer.wrap(spacePacketHeader);
            bb.getInt();
            int pktDataLen = Short.toUnsignedInt(bb.getShort()) + 1;
            int packetLength = pktDataLen + SpacePacket.SP_PRIMARY_HEADER_LENGTH;
            // Return the SpacePacket
            return new SpacePacket(decoder.getNextByte(packetLength * Byte.SIZE), true);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Well, gone too far
            throw new DecodingException("Cannot decode space packet due to sizing issue", e);
        }
    }
}
