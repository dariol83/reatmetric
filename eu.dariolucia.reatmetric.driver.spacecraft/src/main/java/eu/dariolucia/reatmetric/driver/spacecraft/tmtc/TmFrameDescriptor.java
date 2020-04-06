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
