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

import eu.dariolucia.ccsds.encdec.definition.*;
import eu.dariolucia.ccsds.encdec.structure.EncodingException;
import eu.dariolucia.ccsds.encdec.structure.IPacketEncoder;
import eu.dariolucia.ccsds.encdec.structure.PacketDefinitionIndexer;
import eu.dariolucia.ccsds.encdec.structure.impl.DefaultPacketEncoder;
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
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPacketPhase;
import eu.dariolucia.reatmetric.driver.spacecraft.tmtc.TcDataLinkProcessor;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.spacecraft.common.Constants.ACTIVITY_PROPERTY_OVERRIDE_MAP_ID;
import static eu.dariolucia.reatmetric.driver.spacecraft.common.Constants.ACTIVITY_PROPERTY_OVERRIDE_SOURCE_ID;

public class TcPacketHandler {

    private static final Logger LOG = Logger.getLogger(TcPacketHandler.class.getName());

    private static final int MAX_TC_PACKET_SIZE = 65536;

    private final String driverName;
    private final SpacecraftConfiguration configuration;
    private final IServiceCoreContext context;
    private final IServiceBroker serviceBroker;
    private final IPacketEncoder packetEncoder;
    private final Map<Long, PacketDefinition> externalId2packet;
    private final Map<Integer, AtomicInteger> apid2counter = new HashMap<>();
    private final TcDataLinkProcessor tcDataLinkProcessor;
    private final ExecutorService tcExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Activity Handler Thread");
        t.setDaemon(true);
        return t;
    });

    public TcPacketHandler(String driverName, Instant epoch, SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, Definition encodingDecodingDefinitions, TcDataLinkProcessor tcDataLinkProcessor) {
        this.driverName = driverName;
        this.configuration = configuration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        this.packetEncoder = new DefaultPacketEncoder(new PacketDefinitionIndexer(encodingDecodingDefinitions), MAX_TC_PACKET_SIZE, epoch);
        this.tcDataLinkProcessor = tcDataLinkProcessor;
        // Create a map based on the external ID
        this.externalId2packet = new HashMap<>();
        for(PacketDefinition pd : encodingDecodingDefinitions.getPacketDefinitions()) {
            if(pd.getExternalId() != PacketDefinition.EXTERNAL_ID_NOT_SET && pd.getType().equals(configuration.getTcPacketConfiguration().getActivityTcPacketType())) {
                externalId2packet.put(pd.getExternalId(), pd);
            }
        }
    }

    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(configuration.getTcPacketConfiguration().getActivityTcPacketType());
    }

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
            TcPacketInfo packetInfo = new TcPacketInfo(packetInfoStr, ackOverride, sourceId, mapId);
            // Construct the space packet using the information in the encoding definition and the configuration (override by activity properties)
            SpacePacket sp = buildPacket(packetInfo, packetUserDataField);
            Instant encodingTime = Instant.now();
            // Store the TC packet in the raw data archive
            RawData rd = distributeAsRawData(activityInvocation, sp, defToEncode, encodingTime);
            // Build the activity tracker and add it to the Space Packet
            TcTracker tcTracker = buildTcTracker(activityInvocation, sp, packetInfo, rd);
            sp.setAnnotationValue(Constants.ANNOTATION_TC_TRACKER, tcTracker);
            // Notify packet built to service broker: if the packet is time tagged, then the processing will continue in the PUS 11 service implementation
            serviceBroker.informTcPacket(TcPacketPhase.ENCODED, encodingTime, tcTracker);
            // Release packet to lower layer (TC layer), unless the activity is scheduled on-board (PUS 11, activity property)
            String scheduledTime = activityInvocation.getProperties().get(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
            if (scheduledTime != null && !scheduledTime.isBlank()) {
                tcDataLinkProcessor.sendTcPacket(sp, tcTracker);
            }
        } catch(ActivityHandlingException e) {
            LOG.log(Level.SEVERE, "Cannot encode and send TC packet " + defToEncode.getId() + ": " + e.getMessage(), e);
            reportReleaseProgress(context.getProcessingModel(), activityInvocation, ActivityReportState.FATAL);
        }
    }

    private void reportReleaseProgress(IProcessingModel processingModel, IActivityHandler.ActivityInvocation activityInvocation, ActivityReportState status) {
        processingModel.reportActivityProgress(ActivityProgress.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), ActivityOccurrenceReport.RELEASE_REPORT_NAME, Instant.now(), ActivityOccurrenceState.RELEASE, null, status, ActivityOccurrenceState.RELEASE, null));
    }

    private RawData distributeAsRawData(IActivityHandler.ActivityInvocation activityInvocation, SpacePacket sp, PacketDefinition defToEncode, Instant buildTime) {
        RawData rd = new RawData(context.getRawDataBroker().nextRawDataId(), activityInvocation.getGenerationTime(), defToEncode.getId(), defToEncode.getType(),
                activityInvocation.getRoute(), activityInvocation.getSource(), Quality.GOOD, activityInvocation.getActivityOccurrenceId(), sp.getPacket(),
                buildTime, driverName, null);
        rd.setData(sp);
        try {
            context.getRawDataBroker().distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error when distributing encoded TC packet " + defToEncode.getId() + " for activity " + activityInvocation.getPath() + " to raw data broker: " + e.getMessage(), e);
        }
        return rd;
    }

    private SpacePacket buildPacket(TcPacketInfo packetInfo, byte[] packetUserDataField) {
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
        return spb.build();
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
}
