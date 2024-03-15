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
import eu.dariolucia.ccsds.tmtc.util.AnnotatedObject;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.io.Serializable;

public class TcPacketTracker extends AbstractTcTracker implements Serializable {

    private final SpacePacket packet;

    public TcPacketTracker(IActivityHandler.ActivityInvocation invocation, TcPacketInfo info, RawData rd, SpacePacket packet) {
        super(invocation, info, rd);
        this.packet = packet;
    }

    public SpacePacket getPacket() {
        return packet;
    }

    @Override
    public AnnotatedObject getObject() {
        return getPacket();
    }
}
