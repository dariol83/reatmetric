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

// TODO: make it an active object with service broker
public class TcTracker {

    private final IActivityHandler.ActivityInvocation invocation;
    private final SpacePacket packet;
    private final TcPacketInfo info;

    public TcTracker(IActivityHandler.ActivityInvocation invocation, SpacePacket packet, TcPacketInfo info) {
        this.invocation = invocation;
        this.packet = packet;
        this.info = info;
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
}
