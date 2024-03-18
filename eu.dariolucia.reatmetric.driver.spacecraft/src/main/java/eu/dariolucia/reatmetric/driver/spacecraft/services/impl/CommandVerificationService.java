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

import eu.dariolucia.ccsds.encdec.pus.AckField;
import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.activity.IActivityOccurrenceDataArchive;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.AbstractTcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketInfo;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the ECSS PUS 1 command verification service.
 */
public class CommandVerificationService extends AbstractPacketService<Object> {

    private static final Logger LOG = Logger.getLogger(CommandVerificationService.class.getName());
    public static final long DELAYED_REPORT_VALIDITY_TIME_MILLI = 3600 * 1000L;

    private final Map<Integer, Pair<TcPacketTracker, String>> openCommandVerifications = new ConcurrentHashMap<>(); // ID -> TcTracker and stage last name
    private final Map<Integer, List<QueuedReport>> queuedReportMap = new ConcurrentHashMap<>(); // This content is transient and should not be restored

    @Override
    protected void initialiseModelFrom(IArchive archiveToUse, Instant latestGenerationTime) throws ReatmetricException {
        IRawDataArchive rawDataArchive = archiveToUse.getArchive(IRawDataArchive.class);
        IActivityOccurrenceDataArchive actOccArchive = archiveToUse.getArchive(IActivityOccurrenceDataArchive.class);
        List<RawData> data = rawDataArchive.retrieve(latestGenerationTime, 1, RetrievalDirection.TO_PAST, new RawDataFilter(true, Constants.N_TC_VERIFICATION_MAP, null, Collections.singletonList(Constants.T_TC_VERIFICATION_MAP), Collections.singletonList(String.valueOf(spacecraftConfiguration().getId())), Collections.singletonList(Quality.GOOD)));
        if(!data.isEmpty()) {
            String serializedMap = new String(data.get(0).getContents(), StandardCharsets.US_ASCII);
            String[] entries = serializedMap.split("#", -1);
            for(String entry : entries) {
                int id = Integer.parseInt(entry.substring(0, entry.indexOf('=')));
                String rest = entry.substring(entry.indexOf('=') + 1);
                String lastStage = rest.substring(0, rest.indexOf(';'));
                long actInvId = Long.parseLong(rest.substring(rest.indexOf(';') + 1), rest.indexOf('|'));
                long rawDataId = Long.parseLong(rest.substring(rest.indexOf('|') + 1));
                RawData tc = rawDataArchive.retrieve(new LongUniqueId(rawDataId));
                ActivityOccurrenceData accOccData = actOccArchive.retrieve(new LongUniqueId(actInvId));
                IActivityHandler.ActivityInvocation rebuiltInvocation = new IActivityHandler.ActivityInvocation(new LongUniqueId(actInvId),
                        accOccData.getExternalId(), accOccData.getGenerationTime(), accOccData.getPath(), accOccData.getType(), accOccData.getArguments(), accOccData.getProperties(), accOccData.getRoute(), accOccData.getSource());
                SpacePacket sp = new SpacePacket(tc.getContents(), tc.getQuality() == Quality.GOOD);
                TcPacketInfo packetInfo = (TcPacketInfo) tc.getExtension();
                openCommandVerifications.put(id, Pair.of(new TcPacketTracker(rebuiltInvocation, packetInfo, tc, sp), lastStage));
            }
        } else {
            if(LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Open verification map for spacecraft " + spacecraftConfiguration().getId() + " at time " + latestGenerationTime + " not found");
            }
        }
    }

    @Override
    public synchronized void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
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
        // Verify registered TCs
        switch (tmPusHeader.getServiceSubType()) {
            case 1:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_ACCEPTED, true);
                break;
            case 2:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_ACCEPTED, false);
                break;
            case 3:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_STARTED, true);
                break;
            case 4:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_STARTED, false);
                break;
            case 5:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_PROGRESS, true);
                break;
            case 6:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_PROGRESS, false);
                break;
            case 7:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_COMPLETED, true);
                break;
            case 8:
                commandReport(spacePacket, tmPusHeader, packetRawData.getGenerationTime(), Constants.STAGE_SPACECRAFT_COMPLETED, false);
                break;
            default:
                LOG.log(Level.WARNING, "Command Verification Service (1) subtype " + tmPusHeader.getServiceSubType() + " not handled by this implementation");
        }
    }

    private void commandReport(SpacePacket spacePacket, TmPusHeader tmPusHeader, Instant generationTime, String stageName, boolean success) {
        int id = getTcIdentifierFromReport(spacePacket, tmPusHeader);
        Pair<TcPacketTracker, String> trackerPair = this.openCommandVerifications.get(id);
        if(trackerPair == null) {
            LOG.log(Level.WARNING, "Received Command Verification (" + tmPusHeader.getServiceType() + ", " + tmPusHeader.getServiceSubType() + "): originator telecommand " + String.format("%04X", id) + " not registered, queueing report for later processing");
            // Put the report in a queue for later processing, in case the command is finally registered
            queueReport(id, generationTime, stageName, success);
        } else {
            processReport(id, trackerPair, generationTime, stageName, success);
        }
    }

    private void queueReport(int id, Instant generationTime, String stageName, boolean success) {
        List<QueuedReport> list = queuedReportMap.computeIfAbsent(id, o -> new LinkedList<>());
        list.add(new QueuedReport(id, generationTime, stageName, success, Instant.now()));
    }

    private void processReport(int id, Pair<TcPacketTracker, String> trackerPair, Instant generationTime, String stageName, boolean success) {
        TcPacketTracker tracker = trackerPair.getFirst();
        boolean lastVerification = trackerPair.getSecond().equals(stageName);
        TcPhase phase = getTcPacketPhase(stageName, success, lastVerification);
        if(phase != null) {
            serviceBroker().informTc(phase, generationTime, tracker);
        }
        processingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), stageName, generationTime, ActivityOccurrenceState.EXECUTION, generationTime, success ? ActivityReportState.OK : ActivityReportState.FATAL, lastVerification ? ActivityOccurrenceState.VERIFICATION : ActivityOccurrenceState.EXECUTION, null));
        if(lastVerification || !success) {
            removeFromOpenVerification(id);
        }
    }

    private void removeFromOpenVerification(int id) {
        this.openCommandVerifications.remove(id);
        storeVerificationMap();
    }

    private TcPhase getTcPacketPhase(String stageName, boolean success, boolean lastVerification) {
        if(!success) {
            return TcPhase.FAILED;
        } else {
            switch(stageName) {
                case Constants.STAGE_SPACECRAFT_STARTED: return lastVerification ? TcPhase.COMPLETED : TcPhase.STARTED;
                case Constants.STAGE_SPACECRAFT_COMPLETED: return TcPhase.COMPLETED;
                case Constants.STAGE_SPACECRAFT_ACCEPTED:
                case Constants.STAGE_SPACECRAFT_PROGRESS: return lastVerification ? TcPhase.COMPLETED : null;
            }
        }
        return null;
    }

    private int getTcIdentifierFromReport(SpacePacket spacePacket, TmPusHeader tmPusHeader) {
        return ByteBuffer.wrap(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + tmPusHeader.getEncodedLength(), 4).getInt();
    }

    @Override
    public synchronized void onTcUpdate(TcPhase phase, Instant phaseTime, AbstractTcTracker tracker) {
        // If a TC (whatever it is) is RECEIVED_ONBOARD, and it is NOT scheduled, then announce an AVAILABLE_ONBOARD phase
        if(phase == TcPhase.RECEIVED_ONBOARD && !tracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME)) {
            serviceBroker().informTc(TcPhase.AVAILABLE_ONBOARD, phaseTime, tracker);
            context().getProcessingModel().reportActivityProgress(
                    ActivityProgress.of(tracker.getInvocation().getActivityId(),
                            tracker.getInvocation().getActivityOccurrenceId(), Constants.STAGE_ONBOARD_AVAILABILITY,
                            phaseTime, ActivityOccurrenceState.EXECUTION, phaseTime, ActivityReportState.EXPECTED, ActivityOccurrenceState.EXECUTION, null));
            // And we are done
            return;
        }
        // When a command is successfully AVAILABLE_ONBOARD, then it is ready for immediate execution:
        if(phase == TcPhase.AVAILABLE_ONBOARD) {
            registerTcVerificationStages(tracker);
        }
        // It could be that some PUS 1 reports already arrived before the opening of the verification window (wrong propagation delay set,
        // or simply no propagation delay causing possible interleaving sequences between arrival of TMs and processing of TC phase updates
        // The verification of the ACKs can only be done for space packets
        if(!(tracker instanceof TcPacketTracker)) {
            return;
        }
        TcPacketTracker tcPacketTracker = (TcPacketTracker) tracker;
        // Verify is past acks are pending this command
        verifyPendingAcks(getTcIdentifier(tcPacketTracker.getPacket()), phaseTime);
    }

    private void verifyPendingAcks(int id, Instant phaseTime) {
        List<QueuedReport> reps = queuedReportMap.get(id);
        Pair<TcPacketTracker, String> pair = openCommandVerifications.get(id);
        if(reps != null && pair != null) {
            for(QueuedReport report : reps) {
                // Reports that are too old wrt the phase time, should not be processed: remember that you are in this part
                // of the code because the report arrived before the command verification stages were announced. Comparison with ground
                // times, not with generation times.
                //
                // Hardcoded to 1 hour.
                if(Math.abs(phaseTime.toEpochMilli() - report.getProcessingTime().toEpochMilli()) < DELAYED_REPORT_VALIDITY_TIME_MILLI) {
                    processReport(id, pair, report.getGenerationTime(), report.getStageName(), report.isSuccess());
                }
            }
            queuedReportMap.remove(id);
        }
    }

    public synchronized void registerTcVerificationStages(AbstractTcTracker tracker) {
        // Register now
        AckField ackFields;
        TcPacketTracker tcPacketTracker = null;
        if(!(tracker instanceof TcPacketTracker)) {
            // Not a space packet, no ack flags
            ackFields = new AckField(false, false, false, false);
        } else {
            tcPacketTracker = (TcPacketTracker) tracker;
            if (tcPacketTracker.getInfo().getPusHeader() == null) {
                // No PUS packet, no verification using PUS-1, hardcode
                // TODO: if ReatMetric needs to support this, then we need to promote the ackFields at the level of TcPacketInfo (read from the definitions)
                ackFields = new AckField(false, false, false, false);
            } else {
                ackFields = tcPacketTracker.getInfo().getPusHeader().getAckField();
            }
        }

        String lastStage = Constants.STAGE_SPACECRAFT_COMPLETED;
        if(!ackFields.isCompletionAckSet()) {
            lastStage = Constants.STAGE_SPACECRAFT_PROGRESS;
        }
        if(!ackFields.isProgressAckSet()) {
            lastStage = lastStage.equals(Constants.STAGE_SPACECRAFT_PROGRESS) ? Constants.STAGE_SPACECRAFT_STARTED : lastStage;
        }
        if(!ackFields.isStartAckSet()) {
            lastStage = lastStage.equals(Constants.STAGE_SPACECRAFT_STARTED) ? Constants.STAGE_SPACECRAFT_ACCEPTED : lastStage;
        }
        if(!ackFields.isAcceptanceAckSet()) {
            lastStage = lastStage.equals(Constants.STAGE_SPACECRAFT_ACCEPTED) ? null : lastStage;
        }
        if(tcPacketTracker != null) {
            if (ackFields.isAcceptanceAckSet()) {
                registerCommandStage(tcPacketTracker, Constants.STAGE_SPACECRAFT_ACCEPTED, Constants.STAGE_SPACECRAFT_ACCEPTED.equals(lastStage));
            }
            if (ackFields.isStartAckSet()) {
                registerCommandStage(tcPacketTracker, Constants.STAGE_SPACECRAFT_STARTED, Constants.STAGE_SPACECRAFT_STARTED.equals(lastStage));
            }
            if (ackFields.isProgressAckSet()) {
                registerCommandStage(tcPacketTracker, Constants.STAGE_SPACECRAFT_PROGRESS, Constants.STAGE_SPACECRAFT_PROGRESS.equals(lastStage));
            }
            if (ackFields.isCompletionAckSet()) {
                registerCommandStage(tcPacketTracker, Constants.STAGE_SPACECRAFT_COMPLETED, Constants.STAGE_SPACECRAFT_COMPLETED.equals(lastStage));
            }
        }
        if(lastStage != null) {
            // Store verification map
            storeVerificationMap();
        } else {
            // Assume the command executed
            processingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(),
                    tracker.getInvocation().getActivityOccurrenceId(),
                    Constants.STAGE_SPACECRAFT_COMPLETED, Instant.now(),
                    ActivityOccurrenceState.EXECUTION, null,
                    ActivityReportState.EXPECTED, ActivityOccurrenceState.VERIFICATION, null));
        }
    }

    private void storeVerificationMap() {
        Instant now = Instant.now();
        String serializedMap = serializeOpenVerificationMap();
        RawData rd = new RawData(context().getRawDataBroker().nextRawDataId(), now, Constants.N_TC_VERIFICATION_MAP, Constants.T_TC_VERIFICATION_MAP, "", String.valueOf(spacecraftConfiguration().getId()), Quality.GOOD, null, serializedMap.getBytes(StandardCharsets.US_ASCII), now, driverName(), null);
        try {
            context().getRawDataBroker().distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot store open verification map for spacecraft " + spacecraftConfiguration().getId() + ": " + e.getMessage(), e);
        }
    }

    private String serializeOpenVerificationMap() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Integer, Pair<TcPacketTracker, String>> entry : this.openCommandVerifications.entrySet()) {
            sb.append(entry.getKey()).append('=');
            sb.append(entry.getValue().getSecond()).append(';');
            sb.append(entry.getValue().getFirst().getInvocation().getActivityOccurrenceId().asLong()).append('|');
            sb.append(entry.getValue().getFirst().getRawData().getInternalId().asLong());
            sb.append('#');
        }
        if(sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void registerCommandStage(TcPacketTracker tracker, String stageName, boolean isLastStage) {
        processingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), stageName, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null));
        int id = getTcIdentifier(tracker.getPacket());
        if(!isLastStage) {
            openCommandVerifications.putIfAbsent(id, Pair.of(tracker, stageName));
        } else {
            openCommandVerifications.put(id, Pair.of(tracker, stageName)); // Override
        }
    }

    private int getTcIdentifier(SpacePacket spacePacket) {
        return ByteBuffer.wrap(spacePacket.getPacket(), 0, 4).getInt();
    }

    @Override
    public String getName() {
        return "Command Verification Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, item, pusType, pusSubtype, destination, source) -> (rd.getType().equals(Constants.T_TC_VCA)) ||
            (item instanceof SpacePacket) && (
                (((SpacePacket) item).isTelemetryPacket() && pusType != null && pusType == 1) || // For TM 1,x reports
                        !((SpacePacket) item).isTelemetryPacket()
            ); // All TCs
    }

    @Override
    public int getServiceType() {
        return 1;
    }

    @Override
    public boolean isDirectHandler(AbstractTcTracker trackedTc) {
        return false;
    }

    @Override
    public void dispose() {
        openCommandVerifications.clear();
    }

    @Override
    protected Object loadConfiguration(String serviceConfigurationPath) {
        return null;
    }

    private static class QueuedReport {
        private final int id;
        private final Instant generationTime;
        private final String stageName;
        private final boolean success;
        private final Instant processingTime;

        public QueuedReport(int id, Instant generationTime, String stageName, boolean success, Instant processingTime) {
            this.id = id;
            this.generationTime = generationTime;
            this.stageName = stageName;
            this.success = success;
            this.processingTime = processingTime;
        }

        public int getId() {
            return id;
        }

        public Instant getGenerationTime() {
            return generationTime;
        }

        public String getStageName() {
            return stageName;
        }

        public boolean isSuccess() {
            return success;
        }

        public Instant getProcessingTime() {
            return processingTime;
        }
    }
}
