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

import eu.dariolucia.ccsds.encdec.pus.PusChecksumUtil;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingException;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.encdec.structure.IPacketDecoder;
import eu.dariolucia.ccsds.encdec.structure.ParameterValue;
import eu.dariolucia.ccsds.encdec.value.BitString;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IDebugInfoProvider;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPacketConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TmPusConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TmFrameDescriptor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TmPacketProcessor implements IRawDataSubscriber, IDebugInfoProvider {

    private static final Logger LOG = Logger.getLogger(TmPacketProcessor.class.getName());

    private static final ITimeCorrelation IDENTITY_TIME_CORRELATION = new ITimeCorrelation() {
        @Override
        public Instant toUtc(Instant obt) {
            return obt;
        }

        @Override
        public Instant toObt(Instant utc) {
            return utc;
        }
    };

    private final String spacecraft;
    private final Instant epoch;
    private final IProcessingModel processingModel;
    private final IPacketDecoder packetDecoder;
    private final IRawDataBroker broker;
    private final TmPacketConfiguration configuration;
    private final ITimeCorrelation timeCorrelation;
    private final IServiceBroker serviceBroker;
    private final boolean[] processedVCs;

    private final Timer performanceSampler = new Timer("TM Packet Processor - Sampler", true);
    private final AtomicReference<List<DebugInformation>> lastStats = new AtomicReference<>(Arrays.asList(
            DebugInformation.of("TM Packet Processor", "Input packets", 0, null, "packets/second"),
            DebugInformation.of("TM Packet Processor", "Output parameters", 0, null, "parameters/second")
    ));
    private Instant lastSampleGenerationTime;
    private long packetInput = 0;
    private long parameterOutput = 0;


    public TmPacketProcessor(SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker) {
        this.spacecraft = String.valueOf(configuration.getId());
        this.epoch = configuration.getEpoch() == null ? null : Instant.ofEpochMilli(configuration.getEpoch().getTime());
        this.processingModel = context.getProcessingModel();
        this.packetDecoder = serviceBroker.locate(IPacketDecoder.class);
        this.broker = context.getRawDataBroker();
        this.configuration = configuration.getTmPacketConfiguration();
        ITimeCorrelation timeCorrelationService = serviceBroker.locate(eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation.class);
        if(timeCorrelationService != null) {
            this.timeCorrelation = timeCorrelationService;
        } else {
            this.timeCorrelation = IDENTITY_TIME_CORRELATION;
        }
        this.serviceBroker = serviceBroker;
        this.processedVCs = new boolean[64];
        for(int i = 0; i < this.processedVCs.length; ++i) {
            if(this.configuration.getProcessVcs() != null && this.configuration.getProcessVcs().contains(i)) {
                this.processedVCs[i] = true;
            }
        }
        // Create performance samples
        performanceSampler.schedule(new TimerTask() {
            @Override
            public void run() {
                sample();
            }
        }, 1000, 2000);
    }

    private void sample() {
        synchronized (performanceSampler) {
            Instant genTime = Instant.now();
            if (lastSampleGenerationTime == null) {
                lastSampleGenerationTime = genTime;
                packetInput = 0;
                parameterOutput = 0;
            } else {
                long packetInputCurr = packetInput;
                packetInput = 0;
                long paramOutputCurr = parameterOutput;
                parameterOutput = 0;
                int millis = (int) (genTime.toEpochMilli() - lastSampleGenerationTime.toEpochMilli());
                lastSampleGenerationTime = genTime;
                double pktPerSecond = (packetInputCurr / (millis/1000.0));
                double paramsPerSecond = (paramOutputCurr / (millis/1000.0));
                List<DebugInformation> toSet = Arrays.asList(
                        DebugInformation.of("TM Packet Processor", "Input packets", (int) pktPerSecond, null, "packets/second"),
                        DebugInformation.of("TM Packet Processor", "Output parameters", (int) paramsPerSecond, null, "parameters/second")
                );
                lastStats.set(toSet);
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
        // Add performance indicator
        synchronized (performanceSampler) {
            packetInput += messages.size();
        }
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
                DecodingResult result = null;
                int offset = 0;
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
        // Add performance indicator
        synchronized (performanceSampler) {
            parameterOutput += decodedParameters.size();
        }
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
                    return timeCorrelation.toUtc(generationTime);
                }
            }
        }
        // In case the packet time cannot be derived, then use the frame generation time, which is the best approximation possible
        return (Instant) abstractTransferFrame.getAnnotationValue(Constants.ANNOTATION_GEN_TIME);
    }

    public Quality checkPacketQuality(AbstractTransferFrame abstractTransferFrame, SpacePacket spacePacket) {
        // if tmPecPresent in PUS configuration is CRC or ISO, check packet PEC
        short apid = spacePacket.getApid();
        TmPusConfiguration confForApid = configuration.getPusConfigurationFor(apid);
        if(confForApid == null) {
            return Quality.GOOD;
        } else {
            switch (confForApid.getTmPecPresent()) {
                case CRC: {
                    if(PusChecksumUtil.crcChecksum(spacePacket.getPacket(), 0, spacePacket.getLength()) == 0) {
                        return Quality.GOOD;
                    } else {
                        return Quality.BAD;
                    }
                }
                case ISO: {
                    if(PusChecksumUtil.verifyIsoChecksum(spacePacket.getPacket(), 0, spacePacket.getLength())) {
                        return Quality.GOOD;
                    } else {
                        return Quality.BAD;
                    }
                }
                default:
                    return Quality.GOOD;
            }
        }
    }

    public void dispose() {
        this.broker.unsubscribe(this);
        this.performanceSampler.cancel();
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
        // Packet parameters
        DecodingResult result = null;
        try {
            if(!sp.isIdle()) {
                result = packetDecoder.decode(rawData.getName(), rawData.getContents(), 0, rawData.getContents().length, null);
            }
        } catch (DecodingException e) {
            LOG.log(Level.SEVERE, "Cannot decode TM packet " + rawData.getName() + " from route " + rawData.getRoute() + ": " + e.getMessage(), e);
        }
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
        if(result != null) {
            Map<String, Object> paramMap = result.getDecodedItemsAsMap();
            if(!paramMap.isEmpty()) {
                toReturn.put("Raw Parameters", null);
                for (Map.Entry<String, Object> params : paramMap.entrySet()){
                    toReturn.put(params.getKey(), ValueUtil.toString(params.getValue()));
                }
            }
        }
        return toReturn;
    }

    public LinkedHashMap<String, String> renderBadPacket(RawData rawData) {
        return null;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return lastStats.get();
    }
}
