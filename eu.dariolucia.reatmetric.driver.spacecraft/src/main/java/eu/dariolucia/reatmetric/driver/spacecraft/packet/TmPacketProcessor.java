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

package eu.dariolucia.reatmetric.driver.spacecraft.packet;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.ccsds.encdec.value.BitString;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPacketConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPusConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.TimeCorrelationService;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmPacketProcessor implements IRawDataSubscriber {

    private static final Logger LOG = Logger.getLogger(TmPacketProcessor.class.getName());

    private final String spacecraft;
    private final Instant epoch;
    private final IProcessingModel processingModel;
    private final IPacketDecoder packetDecoder;
    private final IRawDataBroker broker;
    private final TmPacketConfiguration configuration;
    private final TimeCorrelationService timeCorrelationService;
    private final ServiceBroker serviceBroker;
    private final boolean[] processedVCs;

    public TmPacketProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IPacketDecoder packetDecoder, TimeCorrelationService timeCorrelationService, ServiceBroker serviceBroker) {
        this.spacecraft = String.valueOf(configuration.getId());
        this.epoch = configuration.getEpoch() == null ? null : Instant.ofEpochMilli(configuration.getEpoch().getTime());
        this.processingModel = context.getProcessingModel();
        this.packetDecoder = packetDecoder;
        this.broker = context.getRawDataBroker();
        this.configuration = configuration.getTmPacketConfiguration();
        this.timeCorrelationService = timeCorrelationService;
        this.serviceBroker = serviceBroker;
        this.processedVCs = new boolean[64];
        for(int i = 0; i < this.processedVCs.length; ++i) {
            if(this.configuration.getProcessVcs() != null && this.configuration.getProcessVcs().contains(i)) {
                this.processedVCs[i] = true;
            }
        }
    }

    public void initialise() {
        subscribeToBroker();
    }

    private void subscribeToBroker() {
        // We want to receive only good space packets, quality must be good, from the configured spacecraft, not idle
        String typeName = Constants.T_TM_PACKET;
        RawDataFilter filter = new RawDataFilter(true, null, null, Collections.singletonList(typeName), Collections.singletonList(spacecraft), Collections.singletonList(Quality.GOOD));
        // We want to get only packets that need to be processed, according to VC ID
        broker.subscribe(this, null, filter, buildPostFilter());
    }

    private Predicate<RawData> buildPostFilter() {
        if(configuration.getProcessVcs() != null) {
            return o -> {
                // If the packet has VC information, then check if it falls in the provided set. If the packet has no VC information, then discard it.
                SpacePacket spacePacket = (SpacePacket) o.getData();
                if (spacePacket != null) {
                    Integer vcId = (Integer) spacePacket.getAnnotationValue(Constants.ANNOTATION_VCID);
                    return vcId != null && processedVCs[vcId];
                } else {
                    return false;
                }
            };
        } else {
            // No filter at packet level
            return o -> true;
        }
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        for(RawData rd : messages) {
            try {
                // To correctly apply generation time derivation ,we need to remember which raw data is used, as we need the generation time of the packet.
                // We build an anonymous class for this purpose.
                ParameterTimeGenerationComputer timeGenerationComputer = new ParameterTimeGenerationComputer(rd);
                // At this stage, we need to have the TmPusHeader available, or a clear indication that the packet does not have such header
                // If the RawData data property has no SpacePacket, we have to instantiate one
                SpacePacket spacePacket = (SpacePacket) rd.getData();
                if (spacePacket == null) {
                    spacePacket = new SpacePacket(rd.getContents(), rd.getQuality() == Quality.GOOD);
                }

                // If the header is already part of the SpacePacket annotation, then good. If not, we have to compute it (we are in playback, maybe)
                TmPusHeader pusHeader = (TmPusHeader) spacePacket.getAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER);
                if (pusHeader == null && spacePacket.isSecondaryHeaderFlag()) {
                    TmPusConfiguration conf = configuration.getPusConfigurationFor(spacePacket.getApid());
                    if (conf != null) {
                        pusHeader = TmPusHeader.decodeFrom(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(), conf.getObtConfiguration() != null && conf.getObtConfiguration().isExplicitPField(), epoch, conf.getTimeDescriptor());
                        spacePacket.setAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER, pusHeader);
                    }
                }

                // Expectation is that the definitions refer to the start of the space packet
                int offset = 0;
                DecodingResult result = null;
                try {
                    result = packetDecoder.decode(rd.getName(), rd.getContents(), offset, rd.getContents().length - offset, timeGenerationComputer);
                    forwardParameterResult(rd, result.getDecodedParameters());
                } catch (DecodingException e) {
                    LOG.log(Level.SEVERE, "Cannot decode packet " + rd.getName() + " from route " + rd.getRoute() + ": " + e.getMessage(), e);
                }
                // Finally, notify all services about the new TM packet
                notifyExtensionServices(rd, spacePacket, pusHeader, result);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Unforeseen exception when processing packet " + rd.getName() + " from route " + rd.getRoute() + ": " + e.getMessage(), e);
            }
        }
    }

    private void notifyExtensionServices(RawData rd, SpacePacket spacePacket, TmPusHeader pusHeader, DecodingResult result) {
        this.serviceBroker.distributeTmPacket(rd, spacePacket, pusHeader, result);
    }

    private void forwardParameterResult(RawData packet, List<ParameterValue> decodedParameters) {
        List<ParameterSample> samples = new ArrayList<>(decodedParameters.size());
        for(ParameterValue pv : decodedParameters) {
            samples.add(mapSample(packet, pv));
        }
        // Inject
        processingModel.injectParameters(samples);
    }

    private ParameterSample mapSample(RawData packet, ParameterValue pv) {
        if(pv.getValue() instanceof BitString) {
            // Conversion is necessary at this stage, due to different types (avoid dependency of reatmetric.api to ccsds.encdec
            eu.dariolucia.reatmetric.api.value.BitString bitString = new eu.dariolucia.reatmetric.api.value.BitString(((BitString) pv.getValue()).getData(), ((BitString) pv.getValue()).getLength());
            return ParameterSample.of((int) (pv.getExternalId() + configuration.getParameterIdOffset()), pv.getGenerationTime(), packet.getReceptionTime(), packet.getInternalId(), bitString, packet.getRoute(), null);
        } else {
            return ParameterSample.of((int) (pv.getExternalId() + configuration.getParameterIdOffset()), pv.getGenerationTime(), packet.getReceptionTime(), packet.getInternalId(), pv.getValue(), packet.getRoute(), null);
        }
    }

    public Instant extractPacketGenerationTime(AbstractTransferFrame abstractTransferFrame, SpacePacket spacePacket) {
        // 1. extract OBT according to PUS configuration (per APID)
        if(spacePacket.isSecondaryHeaderFlag()) {
            TmPusConfiguration conf = configuration.getPusConfigurationFor(spacePacket.getApid());
            if (conf != null) {
                TmPusHeader pusHeader = TmPusHeader.decodeFrom(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(), conf.getObtConfiguration() != null && conf.getObtConfiguration().isExplicitPField(), epoch, conf.getTimeDescriptor());
                spacePacket.setAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER, pusHeader);
                Instant generationTime = pusHeader.getAbsoluteTime();
                // 2. apply time correlation
                if(generationTime != null) {
                    return timeCorrelationService.toUtc(generationTime);
                }
            }
        }
        // In case the packet time cannot be derived, then use the frame generation time, which is the best approximation possible
        return (Instant) abstractTransferFrame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME);
    }

    public Quality checkPacketQuality(AbstractTransferFrame abstractTransferFrame, SpacePacket spacePacket) {
        // TODO: if tmPecPresent in PUS configuration is CRC or ISO, check packet PEC: implement in ccsds.encdec (ISO, CRC)
        return Quality.GOOD;
    }

    public void dispose() {
        this.broker.unsubscribe(this);
    }

    public LinkedHashMap<String, String> renderTmPacket(RawData rawData) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        SpacePacket sp = (SpacePacket) rawData.getData();
        if(sp == null) {
            sp = new SpacePacket(rawData.getContents(), rawData.getQuality().equals(Quality.GOOD));
        }
        TmPusHeader pusHeader = (TmPusHeader) sp.getAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER);
        if (pusHeader == null && sp.isSecondaryHeaderFlag()) {
            TmPusConfiguration conf = configuration.getPusConfigurationFor(sp.getApid());
            if (conf != null) {
                pusHeader = TmPusHeader.decodeFrom(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(), conf.getObtConfiguration() != null && conf.getObtConfiguration().isExplicitPField(), epoch, conf.getTimeDescriptor());
            }
        }
        TmFrameDescriptor frameDescriptor = (TmFrameDescriptor) rawData.getExtension();
        toReturn.put("TM Space Packet", null);
        toReturn.put("APID", String.valueOf(sp.getApid()));
        toReturn.put("SCC", String.valueOf(sp.getPacketSequenceCount()));
        toReturn.put("Sequence Flag", String.valueOf(sp.getSequenceFlag()));
        toReturn.put("Secondary Header Flag", String.valueOf(sp.isSecondaryHeaderFlag()));
        toReturn.put("Idle Packet", String.valueOf(sp.isIdle()));
        toReturn.put("Length", String.valueOf(sp.getLength()));
        if(pusHeader != null) {
            toReturn.put("PUS Header Information", null);
            toReturn.put("Type", String.valueOf(pusHeader.getServiceType()));
            toReturn.put("Subtype", String.valueOf(pusHeader.getServiceSubType()));
            toReturn.put("Destination ID", String.valueOf(pusHeader.getDestinationId()));
            toReturn.put("Packet Subcounter", String.valueOf(pusHeader.getPacketSubCounter()));
            toReturn.put("OBT", String.valueOf(pusHeader.getAbsoluteTime()));
        }
        if(frameDescriptor != null) {
            toReturn.put("Parent (first) TM Frame", null);
            toReturn.put("Virtual Channel ID", String.valueOf(frameDescriptor.getVirtualChannelId()));
            toReturn.put("Virtual Channel Counter", String.valueOf(frameDescriptor.getVirtualChannelFrameCounter()));
            toReturn.put("Earth-Reception Time", String.valueOf(frameDescriptor.getEarthReceptionTime()));
        }
        return toReturn;
    }

    public LinkedHashMap<String, String> renderBadPacket(RawData rawData) {
        return null;
    }
}
