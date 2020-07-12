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

import eu.dariolucia.ccsds.encdec.definition.Definition;
import eu.dariolucia.ccsds.encdec.definition.PacketDefinition;
import eu.dariolucia.ccsds.encdec.pus.PusChecksumUtil;
import eu.dariolucia.ccsds.encdec.pus.TcPusHeader;
import eu.dariolucia.ccsds.encdec.structure.*;
import eu.dariolucia.ccsds.encdec.structure.resolvers.DefaultValueFallbackResolver;
import eu.dariolucia.ccsds.encdec.structure.resolvers.PathLocationBasedResolver;
import eu.dariolucia.ccsds.tmtc.transport.builder.SpacePacketBuilder;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.value.Array;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IActivityExecutor;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketInfo;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket.ITcPacketConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TcPacketConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TcDataLinkProcessor;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.spacecraft.common.Constants.ACTIVITY_PROPERTY_OVERRIDE_MAP_ID;
import static eu.dariolucia.reatmetric.driver.spacecraft.common.Constants.ACTIVITY_PROPERTY_OVERRIDE_SOURCE_ID;

public class TcPacketProcessor implements IActivityExecutor, ITcPacketInjector {

    private static final Logger LOG = Logger.getLogger(TcPacketProcessor.class.getName());

    private final String driverName;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final IPacketEncoder packetEncoder;
    private final IPacketDecoder packetDecoder;
    private final Map<Long, PacketDefinition> externalId2packet;
    private final Map<Integer, AtomicInteger> apid2counter = new HashMap<>();
    private final TcDataLinkProcessor tcDataLinkProcessor;
    private final Map<String, ITcPacketConnector> tcPacketConnectors;
    private final ExecutorService tcExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Activity Handler Thread");
        t.setDaemon(true);
        return t;
    });

    public TcPacketProcessor(String driverName, Instant epoch, SpacecraftConfiguration configuration, IServiceCoreContext context,
                             IServiceBroker serviceBroker, Definition encodingDecodingDefinitions, TcDataLinkProcessor tcDataLinkProcessor,
                             List<ITcPacketConnector> tcPacketConnectors) {
        this.driverName = driverName;
        this.configuration = configuration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        this.packetEncoder = serviceBroker.locate(IPacketEncoder.class);
        this.packetDecoder = serviceBroker.locate(IPacketDecoder.class);;
        this.tcDataLinkProcessor = tcDataLinkProcessor;

        // Create a map based on the external ID
        this.externalId2packet = new HashMap<>();
        for(PacketDefinition pd : encodingDecodingDefinitions.getPacketDefinitions()) {
            if(pd.getExternalId() != PacketDefinition.EXTERNAL_ID_NOT_SET && pd.getType().equals(configuration.getTcPacketConfiguration().getActivityTcPacketType())) {
                externalId2packet.put(pd.getExternalId(), pd);
            }
        }
        // Create the TC Packet connector map
        this.tcPacketConnectors = new TreeMap<>();
        for(ITcPacketConnector m : tcPacketConnectors) {
            for(String route : m.getSupportedRoutes()) {
                this.tcPacketConnectors.put(route, m);
            }
        }
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(configuration.getTcPacketConfiguration().getActivityTcPacketType());
    }

    @Override
    public List<String> getSupportedRoutes() {
        return tcDataLinkProcessor.getSupportedRoutes();
    }

    @Override
    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        //
        if(context.getProcessingModel() == null) {
            throw new ActivityHandlingException("Invocation for activity occurrence " + activityInvocation.getActivityOccurrenceId() + " of " + activityInvocation.getPath()
                    + " cannot be processed: processing model not available");
        }
        // Get the encoding definition
        PacketDefinition defToEncode = externalId2packet.get((long) activityInvocation.getActivityId());
        if(defToEncode == null) {
            throw new ActivityHandlingException("Invocation for activity occurrence " + activityInvocation.getActivityOccurrenceId() + " of " + activityInvocation.getPath()
            + " cannot be processed: TC packet definition not found for external ID " + activityInvocation.getActivityId());
        }
        tcExecutor.execute(() -> encodeAndDispatchTc(activityInvocation, defToEncode));
    }

    private void encodeAndDispatchTc(IActivityHandler.ActivityInvocation activityInvocation, PacketDefinition defToEncode) {
        LOG.log(Level.INFO, "Invocation of activity " + activityInvocation.getPath() + " (" + activityInvocation.getActivityOccurrenceId() + ") in progress");
        reportReleaseProgress(context.getProcessingModel(), activityInvocation, ActivityReportState.PENDING);
        try {
            // Build the map and encode the activity as space packet contents
            Map<String, Object> convertedArgumentMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : activityInvocation.getArguments().entrySet()) {
                if (entry.getValue() instanceof Array) {
                    convertArrayRecords(defToEncode, entry.getKey(), (Array) entry.getValue(), convertedArgumentMap);
                } else {
                    convertedArgumentMap.put(defToEncode.getId() + "." + entry.getKey(), entry.getValue());
                }
            }
            // Encode the body of the packet (no PUS header)
            byte[] packetUserDataField;
            try {
                packetUserDataField = packetEncoder.encode(defToEncode.getId(), new DefaultValueFallbackResolver(new PathLocationBasedResolver(convertedArgumentMap)));
            } catch (EncodingException e) {
                throw new ActivityHandlingException("Cannot encode activity occurrence " + activityInvocation.getActivityOccurrenceId()
                        + " of external ID " + activityInvocation.getActivityId() + ": " + e.getMessage(), e);
            }
            // Retrieve the packet header information
            String packetInfoStr = defToEncode.getExtension();
            // PUS acks overridden?
            String ackOverride = activityInvocation.getProperties().get(Constants.ACTIVITY_PROPERTY_OVERRIDE_ACK);
            // Source ID overridden?
            String sourceIdOverride = activityInvocation.getProperties().get(ACTIVITY_PROPERTY_OVERRIDE_SOURCE_ID);
            Integer sourceId = null;
            if (sourceIdOverride != null) {
                try {
                    sourceId = Integer.parseInt(sourceIdOverride);
                } catch (NumberFormatException e) {
                    throw new ActivityHandlingException("Property " + ACTIVITY_PROPERTY_OVERRIDE_SOURCE_ID + " has wrong format", e);
                }
            }
            // Map ID overridden?
            String mapIdOverride = activityInvocation.getProperties().get(ACTIVITY_PROPERTY_OVERRIDE_MAP_ID);
            Integer mapId = null;
            if (mapIdOverride != null) {
                try {
                    mapId = Integer.parseInt(mapIdOverride);
                } catch (NumberFormatException e) {
                    throw new ActivityHandlingException("Property " + ACTIVITY_PROPERTY_OVERRIDE_MAP_ID + " has wrong format", e);
                }
            }
            // Finally build the packet info for the header
            TcPacketInfo packetInfo = new TcPacketInfo(packetInfoStr, ackOverride, sourceId, mapId, configuration.getTcPacketConfiguration().getSourceIdDefaultValue(), configuration.getTcPacketConfiguration().getTcPecPresent());
            // Construct the space packet using the information in the encoding definition and the configuration (override by activity properties)
            SpacePacket sp = buildPacket(packetInfo, packetUserDataField);
            // Send it off, no need to remember the TcTracker here
            injectTcPacket(activityInvocation, defToEncode, packetInfo, sp);
        } catch(ActivityHandlingException e) {
            LOG.log(Level.SEVERE, "Cannot encode and send TC packet " + defToEncode.getId() + ": " + e.getMessage(), e);
            reportReleaseProgress(context.getProcessingModel(), activityInvocation, ActivityReportState.FATAL);
        } catch(Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error when processing TC packet " + defToEncode.getId() + ": " + e.getMessage(), e);
            reportReleaseProgress(context.getProcessingModel(), activityInvocation, ActivityReportState.FATAL);
        }
    }

    /**
     * This method is used to inject a complete space packet (mapped to an activity occurrence) into the lower processing layers.
     *
     * @param activityInvocation the activity invocation
     * @param packetDefinition the packet definition
     * @param packetInfo the TC packet information
     * @param sp the fully encoded space packet
     * @throws ActivityHandlingException in case of troubles handling the activity
     */
    @Override
    public TcTracker injectTcPacket(IActivityHandler.ActivityInvocation activityInvocation, PacketDefinition packetDefinition, TcPacketInfo packetInfo, SpacePacket sp) throws ActivityHandlingException {
        Instant encodingTime = Instant.now();
        // Store the TC packet in the raw data archive
        RawData rd = distributeAsRawData(activityInvocation, sp, packetDefinition, encodingTime, packetInfo);
        // Build the activity tracker and add it to the Space Packet
        TcTracker tcTracker = buildTcTracker(activityInvocation, sp, packetInfo, rd);
        sp.setAnnotationValue(Constants.ANNOTATION_TC_TRACKER, tcTracker);
        // Notify packet built to service broker: if the packet is time tagged, then the processing will continue in the PUS 11 service implementation
        serviceBroker.informTcPacket(TcPhase.ENCODED, encodingTime, tcTracker);
        // Release packet to lower layer (TC layer), unless the activity is directly handled by a service (e.g. PUS 11, activity property)
        if (!serviceBroker.isDirectlyHandled(tcTracker)) {
            // If there is an external connector for the route, go for it
            ITcPacketConnector externalConnector = this.tcPacketConnectors.get(activityInvocation.getRoute());
            if(externalConnector != null) {
                externalConnector.sendTcPacket(sp, tcTracker);
            } else {
                // Fall back to the TC Data Link processor
                tcDataLinkProcessor.sendTcPacket(sp, tcTracker);
            }
        }
        return tcTracker;
    }

    private void reportReleaseProgress(IProcessingModel processingModel, IActivityHandler.ActivityInvocation activityInvocation, ActivityReportState status) {
        processingModel.reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), ActivityOccurrenceReport.RELEASE_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, status, ActivityOccurrenceState.RELEASE, null));
    }

    private RawData distributeAsRawData(IActivityHandler.ActivityInvocation activityInvocation, SpacePacket sp, PacketDefinition defToEncode, Instant buildTime, TcPacketInfo packetInfo) {
        RawData rd = new RawData(context.getRawDataBroker().nextRawDataId(), activityInvocation.getGenerationTime(), defToEncode.getId(), Constants.T_TC_PACKET,
                activityInvocation.getRoute(), activityInvocation.getSource(), Quality.GOOD, activityInvocation.getActivityOccurrenceId(), sp.getPacket(),
                buildTime, driverName, packetInfo);
        rd.setData(sp);
        try {
            context.getRawDataBroker().distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error when distributing encoded TC packet " + defToEncode.getId() + " for activity " + activityInvocation.getPath() + " to raw data broker: " + e.getMessage(), e);
        }
        return rd;
    }

    private SpacePacket buildPacket(TcPacketInfo packetInfo, byte[] packetUserDataField) {
        // The packet shall also have a PECF as per ECSS.
        SpacePacketBuilder spb = SpacePacketBuilder.create()
                .setApid(packetInfo.getApid())
                .setTelecommandPacket()
                .setSequenceFlag(SpacePacket.SequenceFlagType.UNSEGMENTED)
                .setSecondaryHeaderFlag(packetInfo.getPusHeader() != null);
        // The counter
        int counter = apid2counter.computeIfAbsent(packetInfo.getApid(), o -> new AtomicInteger(0)).accumulateAndGet(1, (a,b) -> {
            int newVal = a + b;
            if(newVal <= 0x3FFF) {
                return newVal;
            } else {
                return 0;
            }
        });
        spb.setPacketSequenceCount(counter);
        if(packetInfo.getPusHeader() != null) {
            spb.addData(packetInfo.getPusHeader().encode(configuration.getTcPacketConfiguration().getSourceIdLength(), configuration.getTcPacketConfiguration().getSpareLength()));
        }
        spb.addData(packetUserDataField);
        switch (packetInfo.getChecksumType()) {
            case CRC:
            case ISO:
                spb.addData(new byte[2]);
                break;
        }
        SpacePacket sp = spb.build();
        switch (packetInfo.getChecksumType()) {
            case CRC: {
                short crc = PusChecksumUtil.crcChecksum(sp.getPacket(), 0, sp.getLength());
                sp.getPacket()[sp.getLength() - 2] = (byte) ((crc >> 8) & 0x00FF);
                sp.getPacket()[sp.getLength() - 1] = (byte) (crc & 0x00FF);
            }
            break;
            case ISO: {
                short iso = PusChecksumUtil.isoChecksum(sp.getPacket(), 0, sp.getLength());
                sp.getPacket()[sp.getLength() - 2] = (byte) ((iso >> 8) & 0x00FF);
                sp.getPacket()[sp.getLength() - 1] = (byte) (iso & 0x00FF);
            }
            break;
        }
        sp.setAnnotationValue(Constants.ANNOTATION_TC_PUS_HEADER, packetInfo.getPusHeader());
        return sp;
    }

    private TcTracker buildTcTracker(IActivityHandler.ActivityInvocation activityInvocation, SpacePacket sp, TcPacketInfo packetInfo, RawData rd) {
        return new TcTracker(activityInvocation, sp, packetInfo, rd);
    }

    private void convertArrayRecords(PacketDefinition defToEncode, String key, Array value, Map<String, Object> convertedArgumentMap) {
        String parentPath = defToEncode.getId() + "." + key;
        appendArrayRecords(parentPath, value.getRecords(), convertedArgumentMap);
    }

    private void appendArrayRecords(String parentPath, List<Array.Record> records, Map<String, Object> convertedArgumentMap) {
        // Map the records now
        int i = 0;
        for(Array.Record record : records) {
            String arrayPath = parentPath + "#" + i;
            appendRecords(arrayPath, record, convertedArgumentMap);
        }
    }

    private void appendRecords(String parentPath, Array.Record record, Map<String, Object> convertedArgumentMap) {
        for(Pair<String, Object> recordElement : record.getElements()) {
            if(recordElement.getSecond() instanceof Array) {
                String arrayPath = parentPath + "." + recordElement.getFirst();
                int i = 0;
                for(Array.Record innerRecord : ((Array) recordElement.getSecond()).getRecords()) {
                    appendRecords(arrayPath + "#" + i, innerRecord, convertedArgumentMap);
                }
            } else {
                convertedArgumentMap.put(parentPath + "." + recordElement.getFirst(), recordElement.getSecond());
            }
        }
    }

    public void dispose() {
        this.tcExecutor.shutdownNow();
    }

    public LinkedHashMap<String, String> renderTcPacket(RawData rawData) {
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        SpacePacket sp = (SpacePacket) rawData.getData();
        if(sp == null) {
            sp = new SpacePacket(rawData.getContents(), rawData.getQuality().equals(Quality.GOOD));
        }
        // Recompute the PUS header (we need the encoded length to perform correct extraction)
        TcPusHeader pusHeader = null;
        if (sp.isSecondaryHeaderFlag()) {
            TcPacketConfiguration conf = configuration.getTcPacketConfiguration();
            if (conf != null) {
                pusHeader = TcPusHeader.decodeFrom(sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH, conf.getSourceIdLength());
            }
        }
        // Packet parameters
        DecodingResult result = null;
        try {
            if(!sp.isIdle()) {
                result = packetDecoder.decode(rawData.getName(), sp.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + (pusHeader != null ? pusHeader.getEncodedLength() : 0), rawData.getContents().length - SpacePacket.SP_PRIMARY_HEADER_LENGTH - (pusHeader != null ? pusHeader.getEncodedLength() : 0), null);
            }
        } catch (DecodingException e) {
            LOG.log(Level.SEVERE, "Cannot decode TM packet " + rawData.getName() + " from route " + rawData.getRoute() + ": " + e.getMessage(), e);
        }
        toReturn.put("TC Space Packet", null);
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
            toReturn.put("Source ID", String.valueOf(pusHeader.getSourceId()));
            toReturn.put("Ack Field", String.valueOf(pusHeader.getAckField().toString()));
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
}
