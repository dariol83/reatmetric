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

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.structure.IPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.PacketDefinitionIndexer;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ActivityHandler {

    private static final int MAX_TC_PACKET_SIZE = 65536;

    private final String driverName;
    private final Instant epoch;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final ServiceBroker serviceBroker;
    private final Definition encDecDefinitions;
    private final IPacketEncoder packetEncoder;
    // Added later with the registerModel method call
    private IProcessingModel processingModel;

    public ActivityHandler(String driverName, Instant epoch, SpacecraftConfiguration configuration, IServiceCoreContext context, ServiceBroker serviceBroker, Definition encodingDecodingDefinitions) {
        this.driverName = driverName;
        this.epoch = epoch;
        this.configuration = configuration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        this.encDecDefinitions = encodingDecodingDefinitions;
        this.packetEncoder = new DefaultPacketEncoder(new PacketDefinitionIndexer(encDecDefinitions), MAX_TC_PACKET_SIZE, epoch);
    }

    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(configuration.getTcPacketConfiguration().getActivityTcPacketType());
    }

    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        // TODO: get the encoding definition
        // TODO: encode the activity as space packet contents
        // TODO: construct the space packet using the information in the encoding definition and the configuration (override by activity properties)
        // TODO: build activity tracker
        // TODO: notify packet built to service broker
        // TODO: release packet to lower layer (TC layer), unless the activity is scheduled on-board (PUS 11, activity property)
    }
}
