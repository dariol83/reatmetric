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
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPacketPhase;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandVerificationService implements IServicePacketSubscriber {

    private static final Logger LOG = Logger.getLogger(CommandVerificationService.class.getName());

    private final SpacecraftConfiguration configuration;
    private final IServiceBroker serviceBroker;
    private final IProcessingModel processingModel;

    private final Timer scheduler = new Timer();

    private final Map<Integer, Pair<TcTracker, String>> openCommandVerifications = new ConcurrentHashMap<>(); // ID -> TcTracker and stage last name
    private final Map<Integer, TimerTask> scheduledOpeningCommandVerifications = new ConcurrentHashMap<>();

    public CommandVerificationService(SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker) {
        this.configuration = configuration;
        this.serviceBroker = serviceBroker;
        this.processingModel = context.getProcessingModel();
        subscribeToBroker();
    }

    private void subscribeToBroker() {
        // Subscribe to service broker to intercept event 1 packets (PUS type == 1)
        serviceBroker.register(this, this::packetFilter);
    }

    private boolean packetFilter(RawData rawData, SpacePacket spacePacket, Integer type, Integer subtype, Integer destination, Integer source) {
        return (spacePacket.isTelemetryPacket() && type != null && type == 1) || !spacePacket.isTelemetryPacket();
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
        processingModel.raiseEvent(eo);
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
            LOG.log(Level.WARNING, "Received Command Verification (" + tmPusHeader.getServiceType() + ", " + tmPusHeader.getServiceSubType() + "): originator telecommand " + String.format("%04X", id) + " not registered");
        } else {
            TcTracker tracker = trackerPair.getFirst();
            boolean lastVerification = trackerPair.getSecond().equals(stageName);
            TcPacketPhase phase = getTcPacketPhase(stageName, success, lastVerification);
            if(phase != null) {
                serviceBroker.informTcPacket(phase, generationTime, tracker);
            }
            processingModel.reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), stageName, generationTime, ActivityOccurrenceState.EXECUTION, generationTime, success ? ActivityReportState.OK : ActivityReportState.FATAL, lastVerification ? ActivityOccurrenceState.VERIFICATION : ActivityOccurrenceState.EXECUTION, null));
            if(lastVerification || !success) {
                this.openCommandVerifications.remove(id);
            }
        }
    }

    private TcPacketPhase getTcPacketPhase(String stageName, boolean success, boolean lastVerification) {
        if(!success) {
            return TcPacketPhase.FAILED;
        } else {
            switch(stageName) {
                case Constants.STAGE_SPACECRAFT_STARTED: return lastVerification ? TcPacketPhase.COMPLETED : TcPacketPhase.STARTED;
                case Constants.STAGE_SPACECRAFT_COMPLETED: return TcPacketPhase.COMPLETED;
                case Constants.STAGE_SPACECRAFT_ACCEPTED:
                case Constants.STAGE_SPACECRAFT_PROGRESS: return lastVerification ? TcPacketPhase.COMPLETED : null;
            }
        }
        return null;
    }

    private int getTcIdentifierFromReport(SpacePacket spacePacket, TmPusHeader tmPusHeader) {
        return ByteBuffer.wrap(spacePacket.getPacket(), SpacePacket.SP_PRIMARY_HEADER_LENGTH + tmPusHeader.getEncodedLength(), 4).getInt();
    }

    @Override
    public synchronized void onTcPacket(TcPacketPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // When a command is successfully UPLINKED, check if:
        if(phase == TcPacketPhase.RECEIVED_ONBOARD) {
            // It is a TC for immediate execution?
            String scheduledTime = tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
            if(scheduledTime == null || scheduledTime.isBlank()) {
                // In this case announce all the expected stages from the ack field now
                registerImmediateTc(tcTracker);
            } else {
                // It is a scheduled TC: in this case announce the stages at the provided time
                Instant time = Instant.parse(scheduledTime);
                registerScheduledTc(tcTracker, time);
            }
        } else if(phase == TcPacketPhase.FAILED) {
            // Check if there is a command that was scheduled for registration
            int id = getTcIdentifier(tcTracker.getPacket());
            TimerTask tt = this.scheduledOpeningCommandVerifications.remove(id);
            if(tt != null) {
                tt.cancel();
            }
        }
    }

    private void registerScheduledTc(TcTracker tcTracker, Instant scheduledTime) {
        Instant now = Instant.now();
        if(now.toEpochMilli() - scheduledTime.toEpochMilli() < 0) {
            // Schedule now
            registerImmediateTc(tcTracker);
        } else {
            TimerTask scheduleLater = new TimerTask() {
                @Override
                public void run() {
                    registerImmediateTc(tcTracker);
                }
            };
            scheduledOpeningCommandVerifications.putIfAbsent(getTcIdentifier(tcTracker.getPacket()), scheduleLater);
            scheduler.schedule(scheduleLater, new Date(scheduledTime.toEpochMilli()));
        }
    }

    private synchronized void registerImmediateTc(TcTracker tcTracker) {
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
            processingModel.reportActivityProgress(ActivityProgress.of(tcTracker.getInvocation().getActivityId(), tcTracker.getInvocation().getActivityOccurrenceId(), Constants.STAGE_SPACECRAFT_COMPLETED, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.EXPECTED, ActivityOccurrenceState.VERIFICATION, null));
        }
    }

    private void registerCommandStage(TcTracker tracker, String stageName, boolean isLastStage) {
        processingModel.reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), stageName, Instant.now(), ActivityOccurrenceState.EXECUTION, null, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null));
        int id = getTcIdentifier(tracker.getPacket());
        openCommandVerifications.putIfAbsent(id, Pair.of(tracker, stageName));
    }

    private int getTcIdentifier(SpacePacket spacePacket) {
        return ByteBuffer.wrap(spacePacket.getPacket(), 0, 4).getInt();
    }

    public void dispose() {
        scheduler.cancel();
        scheduler.purge();
        serviceBroker.deregister(this);
        openCommandVerifications.clear();
        scheduledOpeningCommandVerifications.clear();
    }
}
