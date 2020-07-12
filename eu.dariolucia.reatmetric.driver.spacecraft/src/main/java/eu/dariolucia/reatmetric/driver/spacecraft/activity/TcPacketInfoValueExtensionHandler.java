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

package eu.dariolucia.reatmetric.driver.spacecraft.activity;

import eu.dariolucia.ccsds.encdec.pus.AckField;
import eu.dariolucia.ccsds.encdec.pus.TcPusHeader;
import eu.dariolucia.reatmetric.api.value.IValueExtensionHandler;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.PacketErrorControlType;

import java.nio.ByteBuffer;

public class TcPacketInfoValueExtensionHandler implements IValueExtensionHandler {
    @Override
    public Class<?> typeClass() {
        return TcPacketInfo.class;
    }

    @Override
    public short typeId() {
        return TcPacketInfo.TYPE_ID;
    }

    @Override
    public String toString(Object v) {
        TcPacketInfo desc = (TcPacketInfo) v;
        return "[" + desc.getApid() + "," + desc.getMap() + "," + desc.getChecksumType() +"," + pusHeaderToString(desc.getPusHeader()) + "]";
    }

    private String pusHeaderToString(TcPusHeader pusHeader) {
        if(pusHeader == null) {
            return "";
        } else {
            return "[" + pusHeader.getServiceType() + "," + pusHeader.getServiceSubType() + "," + pusHeader.getAckField().getBitRepresentation() + "," + pusHeader.getSourceId() + "," + pusHeader.getEncodedLength() + "]";
        }
    }

    @Override
    public Object parse(String s) {
        if(!s.startsWith("[") || !s.endsWith("]")) {
            throw new IllegalArgumentException("String " + s + " cannot be parsed as TcPacketInfo");
        }
        // Remove the two square brackets
        s = s.substring(1, s.length() - 1);
        // Get the first number
        int idx = s.indexOf(',');
        int apid = Integer.parseInt(s,0,idx,10);
        int oldIdx = idx;
        idx = s.indexOf(',',idx + 1);
        int map = Integer.parseInt(s,oldIdx + 1,idx + 1,10);
        oldIdx = idx;
        idx = s.indexOf(',',idx + 1);
        PacketErrorControlType ctrlType = PacketErrorControlType.valueOf(s.substring(oldIdx + 1,idx));
        String pusHeaderStr = s.substring(idx + 1);
        TcPusHeader pusHeader = pusHeaderParse(pusHeaderStr);
        return new TcPacketInfo(apid, pusHeader, map, ctrlType);
    }

    private TcPusHeader pusHeaderParse(String s) {
        if(!s.startsWith("[") || !s.endsWith("]")) {
            throw new IllegalArgumentException("String " + s + " cannot be parsed as TcPusHeader");
        }
        // Remove the two square brackets
        s = s.substring(1, s.length() - 1);
        String[] split = s.split(",");
        short type = Short.parseShort(split[0]);
        short subtype = Short.parseShort(split[1]);
        byte ackField = Byte.parseByte(split[2]);
        Integer sourceId = split[3].equals("null") ? null : Integer.parseInt(split[3]);
        Integer encodedLength = split[4].equals("null") ? null : Integer.parseInt(split[4]);
        return new TcPusHeader((byte) 1, new AckField(ackField), type, subtype, sourceId, encodedLength);
    }

    @Override
    public byte[] serialize(Object v) {
        TcPacketInfo desc = (TcPacketInfo) v;
        ByteBuffer bb = ByteBuffer.allocate(23);
        bb.putShort((short)desc.getApid());
        bb.putInt(desc.getMap());
        bb.putInt(desc.getChecksumType().ordinal());
        if(desc.getPusHeader() != null) {
            bb.putShort(desc.getPusHeader().getServiceType());
            bb.putShort(desc.getPusHeader().getServiceSubType());
            bb.put(desc.getPusHeader().getAckField().getBitRepresentation());
            bb.putInt(desc.getPusHeader().getSourceId() != null ? desc.getPusHeader().getSourceId() : -1);
            bb.putInt(desc.getPusHeader().getEncodedLength() != null ? desc.getPusHeader().getEncodedLength() : -1);
        }
        return bb.array();
    }

    @Override
    public Object deserialize(byte[] b, int offset, int length) {
        ByteBuffer bb = ByteBuffer.wrap(b, offset, length);
        short apid = bb.getShort();
        int map = bb.getInt();
        PacketErrorControlType ctrlType = PacketErrorControlType.values()[bb.getInt()];

        short type = bb.getShort();
        short subtype = bb.getShort();
        byte ack = bb.get();
        int sourceId = bb.getInt();
        int encLength = bb.getInt();
        TcPusHeader tcPusHeader = null;
        if(type != 0 || subtype != 0) {
            tcPusHeader = new TcPusHeader((byte) 1, new AckField(ack), type, subtype, sourceId == -1 ? null : sourceId, encLength == -1 ? null : encLength);
        }

        return new TcPacketInfo(apid, tcPusHeader, map, ctrlType);
    }
}
