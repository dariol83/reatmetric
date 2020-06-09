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

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.time.Instant;

/**
 * This class implements the ECSS PUS 5 on-board event service.
 */
public class OnboardEventService extends AbstractPacketService<Object> {

    @Override
    public void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        // Create the event
        EventOccurrence eo = EventOccurrence.of((int) decoded.getDefinition().getExternalId(),
                packetRawData.getGenerationTime(),
                packetRawData.getReceptionTime(),
                packetRawData.getInternalId(), null,
                decoded.getDecodedItemsAsMap(),
                packetRawData.getRoute(),
                packetRawData.getSource(), null);
        // Inject
        processingModel().raiseEvent(eo);
    }

    @Override
    public void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // Not used
    }

    @Override
    public String getName() {
        return "Onboard Event Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, sp, pusType, pusSubtype, destination, source) -> pusType != null && pusType == 5;
    }

    @Override
    public int getServiceType() {
        return 5;
    }

    @Override
    public boolean isDirectHandler(TcTracker trackedTc) {
        return false;
    }

    @Override
    public void dispose() {
        // Nothing here
    }

    @Override
    protected Object loadConfiguration(String serviceConfigurationPath) {
        return null;
    }
}
