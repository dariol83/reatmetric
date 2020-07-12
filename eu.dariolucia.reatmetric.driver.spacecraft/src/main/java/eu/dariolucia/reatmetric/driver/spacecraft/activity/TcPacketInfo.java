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
import eu.dariolucia.reatmetric.driver.spacecraft.definition.PacketErrorControlType;

import java.util.HashMap;
import java.util.Map;

public class TcPacketInfo {

    public static final short TYPE_ID = 101;

    public static final String APID_KEY = "APID";
    public static final String PUSTYPE_KEY = "PUSTYPE";
    public static final String PUSSUBTYPE_KEY = "PUSSUBTYPE";
    public static final String ACKS_KEY = "ACKS";
    public static final String SOURCEID_KEY = "SOURCEID";
    public static final String MAPID_KEY = "MAPID";

    private final int apid;
    private final TcPusHeader pusHeader;
    private final int map;
    private final boolean mapUsed;
    private final PacketErrorControlType checksumType;

    public TcPacketInfo(String str, String overriddenAckFields, Integer overridenSourceId, Integer overridenMapId, Integer sourceIdDefaultValue, PacketErrorControlType checksumType) {
        this.checksumType = checksumType;
        String[] tokens = str.split("\\.", -1);
        Map<String, String> keyValueMap = new HashMap<>();
        for(String ton : tokens) {
            String key = ton.substring(0, ton.indexOf('='));
            String value = ton.substring(key.length() + 1);
            keyValueMap.put(key, value);
        }
        // Look for APID
        this.apid = Integer.parseInt(keyValueMap.get(APID_KEY));
        // Look for PUS Header information
        String pTypeStr = keyValueMap.get(PUSTYPE_KEY);
        String pSubtypeStr = keyValueMap.get(PUSSUBTYPE_KEY);
        String pAckStr = keyValueMap.get(ACKS_KEY);
        String pSourceIdStr = keyValueMap.get(SOURCEID_KEY);
        if(pTypeStr != null && pSubtypeStr != null) {
            int type = Integer.parseInt(pTypeStr);
            int subtype = Integer.parseInt(pSubtypeStr);
            AckField ackField = null;
            if(overriddenAckFields != null) {
                ackField = new AckField(overriddenAckFields.charAt(0) == 'X',
                        overriddenAckFields.charAt(1) == 'X',
                        overriddenAckFields.charAt(2) == 'X',
                        overriddenAckFields.charAt(3) == 'X');
            } else if(pAckStr != null) {
                ackField = new AckField(pAckStr.charAt(0) == 'X',
                        pAckStr.charAt(1) == 'X',
                        pAckStr.charAt(2) == 'X',
                        pAckStr.charAt(3) == 'X');
            } else {
                // No acks
                ackField = new AckField(false, false, false, false);
            }

            Integer sourceId = null;
            if(overridenSourceId != null) {
                sourceId = overridenSourceId;
            } else if(pSourceIdStr != null) {
                sourceId = Integer.parseInt(pSourceIdStr);
            } else {
                sourceId = sourceIdDefaultValue;
            }
            this.pusHeader = new TcPusHeader((byte) 1, ackField, (short) type, (short) subtype, sourceId, null);
        } else {
            this.pusHeader = null;
        }

        String mapIdStr = keyValueMap.get(MAPID_KEY);
        if(overridenMapId != null) {
            this.map = overridenMapId;
            this.mapUsed = true;
        } else if(mapIdStr != null) {
            this.map = Integer.parseInt(mapIdStr);
            this.mapUsed = true;
        } else {
            this.map = -1;
            this.mapUsed = false;
        }
    }

    public TcPacketInfo(int apid, TcPusHeader pusHeader, int map, PacketErrorControlType checksumType) {
        this.apid = apid;
        this.pusHeader = pusHeader;
        this.map = map;
        this.mapUsed = this.map > -1;
        this.checksumType = checksumType;
    }

    public TcPusHeader getPusHeader() {
        return pusHeader;
    }

    public int getMap() {
        return map;
    }

    public boolean isMapUsed() {
        return mapUsed;
    }

    public int getApid() {
        return apid;
    }

    public PacketErrorControlType getChecksumType() {
        return checksumType;
    }

}
