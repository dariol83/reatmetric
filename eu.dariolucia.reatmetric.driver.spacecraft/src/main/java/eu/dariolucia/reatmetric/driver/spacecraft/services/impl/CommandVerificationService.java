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
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.nio.ByteBuffer;
import java.time.Instant;
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

    private final Map<Integer, Pair<TcTracker, String>> openCommandVerifications = new ConcurrentHashMap<>(); // ID -> TcTracker and stage last name
    private final Map<Integer, List<QueuedReport>> queuedReportMap = new ConcurrentHashMap<>();

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
        Pair<TcTracker, String> trackerPair = this.openCommandVerifications.get(id);
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
        list.add(new QueuedReport(id, generationTime, stageName, success));
    }

    private void processReport(int id, Pair<TcTracker, String> trackerPair, Instant generationTime, String stageName, boolean success) {
        TcTracker tracker = trackerPair.getFirst();
        boolean lastVerification = trackerPair.getSecond().equals(stageName);
        TcPhase phase = getTcPacketPhase(stageName, success, lastVerification);
        if(phase != null) {
            serviceBroker().informTcPacket(phase, generationTime, tracker);
        }
        processingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), stageName, generationTime, ActivityOccurrenceState.EXECUTION, generationTime, success ? ActivityReportState.OK : ActivityReportState.FATAL, lastVerification ? ActivityOccurrenceState.VERIFICATION : ActivityOccurrenceState.EXECUTION, null));
        if(lastVerification || !success) {
            this.openCommandVerifications.remove(id);
        }
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
    public synchronized void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // When a command is successfully AVAILABLE_ONBOARD, then it is ready for immediate execution:
        if(phase == TcPhase.AVAILABLE_ONBOARD) {
            registerTcVerificationStages(tcTracker);
        }
        // Verify is past acks are pending this command
        verifyPendingAcks(getTcIdentifier(tcTracker.getPacket()), phaseTime);
    }

    private void verifyPendingAcks(int id, Instant phaseTime) {
        List<QueuedReport> reps = queuedReportMap.get(id);
        Pair<TcTracker, String> pair = openCommandVerifications.get(id);
        if(reps != null && pair != null) {
            for(QueuedReport report : reps) {
                // TODO: reports that are too old wrt the phase time, should not be processed: remember that you are in this part
                //  of the code because the report arrived before the command verification stages were announced. Comparison with ground
                //  times - to be recorded -, not with generation times.
                processReport(id, pair, report.getGenerationTime(), report.getStageName(), report.isSuccess());
            }
            queuedReportMap.remove(id);
        }
    }

    public synchronized void registerTcVerificationStages(TcTracker tcTracker) {
        // Register now
        AckField ackFields = tcTracker.getInfo().getPusHeader().getAckField();
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
        if(ackFields.isAcceptanceAckSet()) {
            registerCommandStage(tcTracker, Constants.STAGE_SPACECRAFT_ACCEPTED, Constants.STAGE_SPACECRAFT_ACCEPTED.equals(lastStage));
        }
        if(ackFields.isStartAckSet()) {
            registerCommandStage(tcTracker, Constants.STAGE_SPACECRAFT_STARTED, Constants.STAGE_SPACECRAFT_STARTED.equals(lastStage));
        }
        if(ackFields.isProgressAckSet()) {
            registerCommandStage(tcTracker, Constants.STAGE_SPACECRAFT_PROGRESS, Constants.STAGE_SPACECRAFT_PROGRESS.equals(lastStage));
        }
        if(ackFields.isCompletionAckSet()) {
            registerCommandStage(tcTracker, Constants.STAGE_SPACECRAFT_COMPLETED, Constants.STAGE_SPACECRAFT_COMPLETED.equals(lastStage));
        }
        if(lastStage == null) {
            // Assume the command executed
            processingModel().reportActivityProgress(ActivityProgress.of(tcTracker.getInvocation().getActivityId(), tcTracker.getInvocation().getActivityOccurrenceId(), Constants.STAGE_SPACECRAFT_COMPLETED, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.EXPECTED, ActivityOccurrenceState.VERIFICATION, null));
        }
    }

    private void registerCommandStage(TcTracker tracker, String stageName, boolean isLastStage) {
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
        return (rd, sp, pusType, pusSubtype, destination, source) ->
             (sp.isTelemetryPacket() && pusType != null && pusType == 1) || // For TM 1,x reports
                    !sp.isTelemetryPacket(); // All TCs
    }

    @Override
    public int getServiceType() {
        return 1;
    }

    @Override
    public boolean isDirectHandler(TcTracker trackedTc) {
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

        public QueuedReport(int id, Instant generationTime, String stageName, boolean success) {
            this.id = id;
            this.generationTime = generationTime;
            this.stageName = stageName;
            this.success = success;
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
    }
}
