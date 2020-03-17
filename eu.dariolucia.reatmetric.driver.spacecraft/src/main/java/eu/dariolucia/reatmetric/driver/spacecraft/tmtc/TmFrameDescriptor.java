/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.tmtc;

import java.io.Serializable;
import java.time.Instant;

/**
 * Extension object attached to a {@link eu.dariolucia.reatmetric.api.rawdata.RawData} that contains a space packet, with
 * the frame information.
 */
public class TmFrameDescriptor implements Serializable {

    public static final short TYPE_ID = 100;

    private final int virtualChannelId;
    private final int virtualChannelFrameCounter;
    private final Instant earthReceptionTime;

    public TmFrameDescriptor(int virtualChannelId, int virtualChannelFrameCounter, Instant earthReceptionTime) {
        this.virtualChannelId = virtualChannelId;
        this.virtualChannelFrameCounter = virtualChannelFrameCounter;
        this.earthReceptionTime = earthReceptionTime;
    }

    public int getVirtualChannelId() {
        return virtualChannelId;
    }

    public int getVirtualChannelFrameCounter() {
        return virtualChannelFrameCounter;
    }

    public Instant getEarthReceptionTime() {
        return earthReceptionTime;
    }

    @Override
    public String toString() {
        return "TmFrameDescriptor{" +
                "virtualChannelId=" + virtualChannelId +
                ", virtualChannelFrameCounter=" + virtualChannelFrameCounter +
                ", earthReceptionTime=" + earthReceptionTime +
                '}';
    }
}
