/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;

import java.util.Arrays;
import java.util.Collections;

public class OnboardEventService implements IServicePacketSubscriber {

    private final SpacecraftConfiguration configuration;
    private final ServiceBroker serviceBroker;
    private final IProcessingModel processingModel;

    public OnboardEventService(SpacecraftConfiguration configuration, IServiceCoreContext context, ServiceBroker serviceBroker) {
        this.configuration = configuration;
        this.serviceBroker = serviceBroker;
        this.processingModel = context.getProcessingModel();
        subscribeToBroker();
    }

    private void subscribeToBroker() {
        // Subscribe to service broker to intercept event 5 packets (PUS type == 5)
        serviceBroker.register(this, this::packetFilter);
    }

    private boolean packetFilter(RawData rawData, SpacePacket spacePacket, Integer type, Integer subtype, Integer destination, Integer source) {
        return type != null && type == 5;
    }

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
        processingModel.raiseEvent(eo);
    }

    public void dispose() {
        serviceBroker.deregister(this);
    }
}
