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
import eu.dariolucia.ccsds.encdec.extension.IEncoderExtension;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.PathLocation;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;

@ExtensionId(id="eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket")
public class SpacePacketEncodingExtension implements IEncoderExtension {

    @Override
    public void encode(PacketDefinition definition, EncodedParameter parameter, PathLocation location, BitEncoderDecoder encoder, Object value) throws EncodingException {
        if(value == null) {
            throw new EncodingException("Null value for parameter " + parameter.getId() + " cannot be casted to a SpacePacket");
        }
        if(value instanceof SpacePacket) {
            SpacePacket packet = (SpacePacket) value;
            encoder.setNextByte(packet.getPacket(), packet.getPacket().length * 8);
        } else {
            throw new EncodingException("Object of class " + value.getClass().getName() + " for parameter " + parameter.getId() + " cannot be casted to a SpacePacket");
        }
    }
}
