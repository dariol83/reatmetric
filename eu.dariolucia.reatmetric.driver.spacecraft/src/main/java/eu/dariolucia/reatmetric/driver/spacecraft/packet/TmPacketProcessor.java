/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.packet;

import eu.dariolucia.ccsds.encdec.definition.EncodedParameter;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.ccsds.encdec.time.IGenerationTimeProcessor;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.PusConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmPacketProcessor implements IRawDataSubscriber, IGenerationTimeProcessor {

    private static final Logger LOG = Logger.getLogger(TmPacketProcessor.class.getName());

    private final IProcessingModel processingModel;
    private final IPacketDecoder packetDecoder;
    private final IRawDataBroker broker;
    private final PusConfiguration configuration;

    public TmPacketProcessor(IProcessingModel processingModel, IPacketDecoder packetDecoder, IRawDataBroker broker, PusConfiguration configuration) {
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
                DecodingResult result = packetDecoder.decode(rd.getName(), rd.getContents(), this);
                forwardParameterResult(rd, result.getDecodedParameters());
                notifyExtensionServices(rd, result); // TODO: notify the result and the packet to the registered PUS services
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
        return ParameterSample.of(pv.getId(), pv.getGenerationTime(), packet.getReceptionTime(), packet.getInternalId(), pv.getValue(), packet.getRoute(), null); // TODO use configured offset, change ccsds encdec to return the external id
    }

    @Override
    public Instant computeGenerationTime(EncodedParameter ei, Object value, Instant derivedGenerationTime, Duration derivedOffset, Integer fixedOffsetMs) {
        // TODO
        return null;
    }
}
