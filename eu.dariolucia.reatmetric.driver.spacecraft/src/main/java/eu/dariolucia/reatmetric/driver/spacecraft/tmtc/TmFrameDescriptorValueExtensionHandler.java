/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import eu.dariolucia.reatmetric.api.value.IValueExtensionHandler;

import java.nio.ByteBuffer;
import java.time.Instant;

public class TmFrameDescriptorValueExtensionHandler implements IValueExtensionHandler {
    @Override
    public Class<?> typeClass() {
        return TmFrameDescriptor.class;
    }

    @Override
    public short typeId() {
        return TmFrameDescriptor.TYPE_ID;
    }

    @Override
    public String toString(Object v) {
        TmFrameDescriptor desc = (TmFrameDescriptor) v;
        return "[" + desc.getVirtualChannelId() + "," + desc.getVirtualChannelFrameCounter() + "," + desc.getEarthReceptionTime().getEpochSecond() +"," + desc.getEarthReceptionTime().getNano() + "]";
    }

    @Override
    public Object parse(String s) {
        if(!s.startsWith("[") || !s.endsWith("]")) {
            return new IllegalArgumentException("String " + s + " cannot be parsed as TmFrameDescriptor");
        }
        // Remove the two square brackets
        s = s.substring(1, s.length() - 1);
        // Get the first number
        int idx = s.indexOf(',');
        int vc = Integer.parseInt(s,0,idx,10);
        int oldIdx = idx;
        idx = s.indexOf(',',idx + 1);
        int vcc = Integer.parseInt(s,oldIdx + 1,idx + 1,10);
        oldIdx = idx;
        idx = s.indexOf(',',idx + 1);
        long epochSecond = Long.parseLong(s,oldIdx + 1,idx + 1,10);
        oldIdx = idx;
        idx = s.indexOf(',',idx + 1);
        long epochNano = Long.parseLong(s,oldIdx + 1,idx + 1,10);
        return new TmFrameDescriptor(vc, vcc, Instant.ofEpochSecond(epochSecond, epochNano));
    }

    @Override
    public byte[] serialize(Object v) {
        TmFrameDescriptor desc = (TmFrameDescriptor) v;
        ByteBuffer bb = ByteBuffer.allocate(22);
        bb.putShort((short)desc.getVirtualChannelId());
        bb.putInt(desc.getVirtualChannelFrameCounter());
        bb.putLong(desc.getEarthReceptionTime().getEpochSecond());
        bb.putLong(desc.getEarthReceptionTime().getNano());
        return bb.array();
    }

    @Override
    public Object deserialize(byte[] b, int offset, int length) {
        ByteBuffer bb = ByteBuffer.wrap(b, offset, length);
        int vc = Short.toUnsignedInt(bb.getShort());
        int vcc = bb.getInt();
        long epochSecond = bb.getLong();
        long nano = bb.getLong();
        return new TmFrameDescriptor(vc, vcc, Instant.ofEpochSecond(epochSecond, nano));
    }
}
