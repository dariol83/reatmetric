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
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPusConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPacketConfiguration;

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

    public TmPacketProcessor(Instant epoch, IProcessingModel processingModel, IPacketDecoder packetDecoder, IRawDataBroker broker, TmPacketConfiguration configuration) {
        this.epoch = epoch;
        this.processingModel = processingModel;
        this.packetDecoder = packetDecoder;
        this.broker = broker;
        this.configuration = configuration;
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
                DecodingResult result = packetDecoder.decode(rd.getName(), rd.getContents(), timeGenerationComputer);
                forwardParameterResult(rd, result.getDecodedParameters());
                notifyExtensionServices(rd, result); // TODO: notify the decoding result and the packet to the registered PUS services
            } catch (DecodingException e) {
                LOG.log(Level.SEVERE, "Cannot decode packet " + rd.getName() + " from route " + rd.getRoute() + ": " + e.getMessage(), e);
            }
        }
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
        // TODO: 1. extract OBT according to PUS configuration (per APID); 2. apply time correlation
        if(spacePacket.isSecondaryHeaderFlag()) {
            TmPusConfiguration conf = configuration.getPusConfigurationFor(spacePacket.getApid());
            if (conf != null) {
                TmPusHeader pusHeader = TmPusHeader.decodeFrom(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.isPacketSubCounterPresent(), conf.getDestinationLength(), conf.isExplicitPField(), epoch, conf.getTimeDescriptor());
                spacePacket.setAnnotationValue(Constants.ANNOTATION_TM_PUS_HEADER, pusHeader);
                Instant generationTime = pusHeader.getAbsoluteTime();
                if(generationTime != null) {
                    // TODO: Time correlate ... should we do it here?!??
                }
            }
        }
        return null;
    }

    public Quality checkPacketQuality(AbstractTransferFrame abstractTransferFrame, SpacePacket spacePacket) {
        // TODO: if tmPecPresent in PUS configuration is CRC or ISO, check packet PEC: implement in ccsds.encdec (ISO, CRC)
        return Quality.GOOD;
    }
}
