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

package eu.dariolucia.reatmetric.driver.spacecraft.services;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.AbstractTcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.VirtualChannelUnit;

import java.time.Instant;

public interface IServicePacketSubscriber {

    void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded);

    void onTmVcUnit(RawData packetRawData, VirtualChannelUnit unit, DecodingResult decoded);

    void onTcUpdate(TcPhase phase, Instant phaseTime, AbstractTcTracker tcTracker);
}
