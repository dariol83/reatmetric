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

import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TcTracker implements Serializable {

    // The following fields are effectively final, but due to the serialisation in readObject, cannot be labelled as such

    private IActivityHandler.ActivityInvocation invocation;
    private SpacePacket packet;
    private TcPacketInfo info;
    private RawData rawData;

    public TcTracker(IActivityHandler.ActivityInvocation invocation, SpacePacket packet, TcPacketInfo info, RawData rd) {
        this.invocation = invocation;
        this.packet = packet;
        this.info = info;
        this.rawData = rd;
    }

    public RawData getRawData() {
        return rawData;
    }

    public IActivityHandler.ActivityInvocation getInvocation() {
        return invocation;
    }

    public SpacePacket getPacket() {
        return packet;
    }

    public TcPacketInfo getInfo() {
        return info;
    }

    /*
     * Since this class is Serializable, but SpacePacket is NOT, we need to implement a custom serialisation.
     */

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.writeObject(invocation);
        out.writeObject(info);
        out.writeObject(rawData);
        out.writeInt(packet.getLength());
        out.writeBoolean(packet.isQualityIndicator());
        // Get annotation map: TODO: an helper method in the CCSDS library would help...
        Map<Object, Object> annotationMap = new HashMap<>();
        for(Object key : packet.getAnnotationKeys()) {
            annotationMap.put(key, packet.getAnnotationValue(key));
        }
        out.writeObject(annotationMap);
        out.write(packet.getPacket());
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        invocation = (IActivityHandler.ActivityInvocation) in.readObject();
        info = (TcPacketInfo) in.readObject();
        rawData = (RawData) in.readObject();
        int len = in.readInt();
        boolean quality = in.readBoolean();
        Map<Object, Object> annotationMap = (Map<Object, Object>) in.readObject();
        byte[] pkt = in.readNBytes(len);
        this.packet = new SpacePacket(pkt, quality);
        for(Map.Entry<Object, Object> entry : annotationMap.entrySet()) {
            this.packet.setAnnotationValue(entry.getKey(), entry.getValue());
        }
    }

    private void readObjectNoData()
            throws ObjectStreamException {

    }
}
