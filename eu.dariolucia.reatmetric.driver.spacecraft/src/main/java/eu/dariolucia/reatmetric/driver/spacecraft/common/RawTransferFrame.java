/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.common;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;

/**
 * This class defines a raw transfer frame, i.e. a space data link protocol unit with no information and not compatible/defined
 * according to the CCSDS standards.
 */
public class RawTransferFrame extends AbstractTransferFrame {

    private final boolean idle;
    private final String externalTmType;

    /**
     * Constructor of a raw transfer frame. Very simple, as more at this stage you cannot know.
     *
     * @param spacecraftId  the spacecraft ID
     * @param frame         the frame data
     * @param idle          if idle frame
     * @param valid         if valid
     */
    public RawTransferFrame(short spacecraftId, String externalTmType, byte[] frame, boolean idle, boolean valid) {
        super(frame, false);
        this.dataFieldLength = frame.length;
        this.dataFieldStart = 0;
        this.ocfPresent = false;
        this.ocfStart = -1;
        this.spacecraftId = spacecraftId;
        this.valid = valid;
        this.transferFrameVersionNumber = -1;
        this.virtualChannelFrameCount = -1;
        this.virtualChannelId = -1;
        this.idle = idle;
        this.externalTmType = externalTmType;
    }

    public String getExternalTmType() {
        return externalTmType;
    }

    @Override
    public boolean isIdleFrame() {
        return idle;
    }
}
