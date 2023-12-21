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

package eu.dariolucia.reatmetric.driver.socket;

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketDecoder;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryDecodingTest {

    @Test
    public void testBinaryDecoding() throws Exception {
        String toDecode = "52454154" + // preamble
                "00000048" + // length: 72 bytes
                "0000FFFF" + // filler
                "00000002" + // device_subsystem
                "80000001" + // operation
                "00000001" + // status_val
                "0000000000000BB8" + // freq_val
                "4034333333333333" + // temp_val (double)
                "0000000000000000" + // offset_val
                "00000000" + // mode_val
                "00000007" + // summary_val: length
                "4E6F6D696E616C" + // summary_val: data
                "00" + // summary_val: padding
                "00000001" + // sweep_val
                "4D455452";
        byte[] message = StringUtil.toByteArray(toDecode);
        String messageId = "TLM_SUB2";
        Definition packetDef = Definition.load(getClass().getClassLoader().getResourceAsStream("binary_double/binary_double_messages_tlm.xml"));
        IPacketDecoder decoder = new DefaultPacketDecoder(packetDef);
        DecodingResult result = decoder.decode(messageId, message);
        assertEquals(1, result.getDecodedItemsAsMap().get("TLM_SUB2.sweep_val"));
    }

    @Test
    public void testBinaryDecoding2() throws Exception {
        String toDecode = "52454154" +
                "00000047" + // length: 71 bytes
                "00000002" + // device_subsystem
                "80000001" + // operation
                "00000002" + // request
                "00000001" + // status_val
                "0000000000000BB8" + // freq_val
                "404DA916872B020C" + // temp_val (double)
                "0000000000000000" + // offset_val
                "00000000" +
                "00000007" + // summary_val_length
                "4E6F6D696E616C" + // summary_val
                "00000000" + // sweep_val
                "4D455452";

        byte[] message = StringUtil.toByteArray(toDecode);
        String messageId = "TLM_SUB2";
        Definition packetDef = Definition.load(getClass().getClassLoader().getResourceAsStream("binary_single/binary_single_messages.xml"));
        IPacketDecoder decoder = new DefaultPacketDecoder(packetDef);
        DecodingResult result = decoder.decode(messageId, message);
        assertEquals(0, result.getDecodedItemsAsMap().get("TLM_SUB2.sweep_val"));
    }
}
