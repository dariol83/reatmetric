/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.packet;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
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
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPusConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPacketConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.impl.TimeCorrelationService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmPacketProcessor implements IRawDataSubscriber {

    private static final Logger LOG = Logger.getLogger(TmPacketProcessor.class.getName());

    private final Instant epoch;
    private final IProcessingModel processingModel;
    private final IPacketDecoder packetDecoder;
    private final IRawDataBroker broker;
    private final TmPacketConfiguration configuration;
    private final TimeCorrelationService timeCorrelationService;
    private final ServiceBroker serviceBroker;

    public TmPacketProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IPacketDecoder packetDecoder, TimeCorrelationService timeCorrelationService, ServiceBroker serviceBroker) {
        this.epoch = configuration.getEpoch();
        this.processingModel = context.getProcessingModel();
        this.packetDecoder = packetDecoder;
        this.broker = context.getRawDataBroker();
        this.configuration = configuration.getTmPacketConfiguration();
        this.timeCorrelationService = timeCorrelationService;
        this.serviceBroker = serviceBroker;
    }

    public void initialise() {
        subscribeToBroker();
    }

    private void subscribeToBroker() {
        // We want to receive only good space packets, quality must be good, from the configured spacecraft, not idle
        String typeName = Constants.T_TM_PACKET;
        RawDataFilter filter = new RawDataFilter(true, null, null, Collections.singletonList(typeName), null, Collections.singletonList(Quality.GOOD));
        broker.subscribe(this, null, filter, null);
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
                if(spacePacket == null) {
                    spacePacket = new SpacePacket(rd.getContents(), rd.getQuality() == Quality.GOOD);
                }
                // If the header is already part of the SpacePacket annotation, then good. If not, we have to compute it (we are in playback, maybe)
                TmPusHeader pusHeader = (TmPusHeader) spacePacket.getAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER);
                TmPusConfiguration conf = null;
                if(pusHeader == null && spacePacket.isSecondaryHeaderFlag()) {
                    conf = configuration.getPusConfigurationFor(spacePacket.getApid());
                    if (conf != null) {
                        pusHeader = TmPusHeader.decodeFrom(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(), conf.getObtConfiguration() != null && conf.getObtConfiguration().isExplicitPField(), epoch, conf.getTimeDescriptor());
                        spacePacket.setAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER, pusHeader);
                    }
                }
                // Use offset and length, check pusHeader.getEncodedLength to understand where you have to start decoding from
                int offset = SpacePacket.SP_PRIMARY_HEADER_LENGTH + (pusHeader != null ? pusHeader.getEncodedLength() : 0);
                DecodingResult result = packetDecoder.decode(rd.getName(), rd.getContents(), offset, rd.getContents().length - offset, timeGenerationComputer);
                forwardParameterResult(rd, result.getDecodedParameters());
                // Finally, notify all services about the new TM packet
                notifyExtensionServices(rd, spacePacket, pusHeader, result);
            } catch (DecodingException e) {
                LOG.log(Level.SEVERE, "Cannot decode packet " + rd.getName() + " from route " + rd.getRoute() + ": " + e.getMessage(), e);
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
        return ParameterSample.of((int) (pv.getExternalId() + configuration.getParameterIdOffset()), pv.getGenerationTime(), packet.getReceptionTime(), packet.getInternalId(), pv.getValue(), packet.getRoute(), null);
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
}
