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

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.services.OnboardOperationsSchedulingServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the ECSS PUS 11 command scheduling service (limited to 11,4 and 11,3 commands).
 * TODO: support for 11,1, 11,2 and status report (minimal capability set) to be implemented
 * TODO: restore from archived state
 */
public class OnboardOperationsSchedulingService extends AbstractPacketService<OnboardOperationsSchedulingServiceConfiguration> {

    private static final Logger LOG = Logger.getLogger(OnboardOperationsSchedulingService.class.getName());

    public static final int VERIFICATION_AHEAD_MILLIS = 1000;

    private final Timer scheduler = new Timer();
    // This map contains the time-tagged TCs
    private final Map<IUniqueId, LinkedTcTracker> linkedActivityOccurrence2tcTracker = new HashMap<>();

    @Override
    public synchronized void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // If a TC is RECEIVED_ONBOARD and it is NOT scheduled, then announce an AVAILABLE_ONBOARD phase
        if(phase == TcPhase.RECEIVED_ONBOARD && !tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME)) {
            serviceBroker().informTcPacket(TcPhase.AVAILABLE_ONBOARD, phaseTime, tcTracker);
            reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.EXECUTION, Constants.STAGE_ONBOARD_AVAILABILITY, ActivityReportState.EXPECTED, ActivityOccurrenceState.EXECUTION, phaseTime);
        }
        // We are interested in TCs having scheduled information in the ENCODED phase
        if(phase == TcPhase.ENCODED && tcTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME)) {
            String scheduledTime = tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
            if (scheduledTime != null && !scheduledTime.isBlank()) {
                // This is a time-tagged command, so a 11,4 must be built and provided to the lower layers
                Instant targetTime = Instant.parse(scheduledTime);
                // If you will get any notification about this activity at the various stages, then also inform accordingly the original TcTracker
                LinkedTcTracker linkedTracker = new LinkedTcTracker(tcTracker, targetTime);
                linkedActivityOccurrence2tcTracker.put(tcTracker.getInvocation().getActivityOccurrenceId(), linkedTracker);
                // Dispatch a new activity
                try {
                    dispatch(targetTime, linkedTracker);
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Error while dispatching 11,4 command for activity " + tcTracker.getInvocation().getPath() + " (" + tcTracker.getInvocation().getActivityOccurrenceId() + "): " + e.getMessage(), e);
                    linkedTracker.terminate(TcPhase.FAILED, phaseTime, false);
                }
                // Stop here
                return;
            }
        }
        // If it is a 11,4, check for the tracked activity and announce phases accordingly
        if(tcTracker.getInfo().getPusHeader().getServiceType() == 11 && tcTracker.getInfo().getPusHeader().getServiceSubType() == 4) {
            // Get the original tracker
            // It could be more than one activity linked to the PUS 11,4 TC, so use a concatenation of IDs, separated by |. The approach is identical though (use a for loop).
            // But in this implementation we support a single TC per PUS 11,4, so the above is not needed.
            String occIdStr = tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID);
            LinkedTcTracker linkedTcTracker = linkedActivityOccurrence2tcTracker.get(new LongUniqueId(Long.parseLong(occIdStr)));
            // Update the tracking information
            linkedTcTracker.informTcTransition(phase, phaseTime);
        }
        // If it is a 11,3 in status COMPLETED, the schedule is reset
        if(tcTracker.getInfo().getPusHeader().getServiceType() == 11 && tcTracker.getInfo().getPusHeader().getServiceSubType() == 3 && phase == TcPhase.COMPLETED) {
            // Get the original tracker
            Set<IUniqueId> keys = linkedActivityOccurrence2tcTracker.keySet();
            for(IUniqueId id : keys) {
                LinkedTcTracker linkedTcTracker = linkedActivityOccurrence2tcTracker.remove(id);
                linkedTcTracker.terminate(TcPhase.FAILED, phaseTime, false);
            }
        }
        // In any case, if the tcTracker is present in the linkedActivityOccurrence2tcTracker and results as STARTED or COMPLETED or FAILED, it should be removed silently
        if(phase == TcPhase.STARTED || phase == TcPhase.COMPLETED || phase == TcPhase.FAILED) {
            IUniqueId occId = tcTracker.getInvocation().getActivityOccurrenceId();
            LinkedTcTracker track = linkedActivityOccurrence2tcTracker.remove(occId);
            if(track != null) {
                track.terminate(phase, phaseTime, true);
            }
        }
    }

    private void dispatch(Instant targetTime, LinkedTcTracker originalCommand) throws ReatmetricException, RemoteException {
        SystemEntityPath activity = SystemEntityPath.fromString(configuration().getActivityPath());
        ActivityDescriptor descriptor = context().getServiceFactory().getActivityOccurrenceDataMonitorService().getDescriptor(activity);
        // Derive the various arguments you need: sub-schedule-id (opt.), if array or not, first time ABSOLUTE_TIME of array is execution time, last field OCTET_STRING of array is the command
        String subscheduleName = configuration().getSubscheduleIdName();
        String numCommandsName = configuration().getNumCommandsName();
        PlainActivityArgument subschedule = null;
        if(configuration().isArrayUsed()) {
            // Assume 1, 2 or 3 parameters: sub-schedule ID if subscheduleName is not null; num-elements if numCommandsName is not null; array definition (mandatory)
            PlainActivityArgument numElements = null;
            ArrayActivityArgument listCommands = null;
            for (AbstractActivityArgumentDescriptor arg : descriptor.getArgumentDescriptors()) {
                if (arg instanceof ActivityPlainArgumentDescriptor && arg.getName().equals(subscheduleName)) {
                    ValueTypeEnum type = ((ActivityPlainArgumentDescriptor) arg).getRawDataType();
                    String subScheduleIdStr = originalCommand.tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_ID);
                    Object value = ValueUtil.parse(type, subScheduleIdStr);
                    subschedule = new PlainActivityArgument(subscheduleName, value, null, false);
                    // Set the subschedule ID inside the linked tc tracker
                    originalCommand.setSubscheduleId((Number) value);
                } else if (arg instanceof ActivityPlainArgumentDescriptor && arg.getName().equals(numCommandsName)) {
                    ValueTypeEnum type = ((ActivityPlainArgumentDescriptor) arg).getRawDataType();
                    Object value = ValueUtil.parse(type, "1");
                    numElements = new PlainActivityArgument(numCommandsName, value, null, false);
                } else if (arg instanceof ActivityArrayArgumentDescriptor) {
                    // Must be the array
                    PlainActivityArgument execTimeArg = null;
                    PlainActivityArgument commandArg = null;
                    for(AbstractActivityArgumentDescriptor innerArg : ((ActivityArrayArgumentDescriptor) arg).getElements()) {
                        // Assume plain argument
                        ActivityPlainArgumentDescriptor innerArgDesc = (ActivityPlainArgumentDescriptor) innerArg;
                        if(innerArgDesc.getRawDataType() == ValueTypeEnum.ABSOLUTE_TIME && execTimeArg == null) {
                            execTimeArg = new PlainActivityArgument(innerArg.getName(), targetTime, null, false);
                        } else if(innerArgDesc.getRawDataType() == ValueTypeEnum.OCTET_STRING && commandArg == null) {
                            commandArg = new PlainActivityArgument(innerArg.getName(), originalCommand.tcTracker.getPacket().getPacket(), null, false);
                        }
                    }
                    ArrayActivityArgumentRecord record = new ArrayActivityArgumentRecord(Arrays.asList(execTimeArg, commandArg));
                    listCommands = new ArrayActivityArgument(arg.getName(), Collections.singletonList(record));
                }
            }
            List<AbstractActivityArgument> arguments = new LinkedList<>();
            if(subschedule != null) {
                arguments.add(subschedule);
            }
            if(numElements != null) {
                arguments.add(numElements);
            }
            if(listCommands != null) {
                arguments.add(listCommands);
            }
            processingModel().startActivity(new ActivityRequest(descriptor.getExternalId(), descriptor.getPath(), arguments,
                    Map.of(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID, String.valueOf(originalCommand.tcTracker.getInvocation().getActivityOccurrenceId().asLong())),
                    originalCommand.tcTracker.getInvocation().getRoute(),
                    originalCommand.tcTracker.getInvocation().getSource()));
        } else {
            // Assume 3 parameters: sub-schedule ID if name is not null; execution time (mandatory); command (mandatory)
            PlainActivityArgument execTimeArg = null;
            PlainActivityArgument commandArg = null;
            PlainActivityArgument numElements = null;
            for (AbstractActivityArgumentDescriptor targ : descriptor.getArgumentDescriptors()) {
                if(targ instanceof ActivityPlainArgumentDescriptor) {
                    ActivityPlainArgumentDescriptor arg = (ActivityPlainArgumentDescriptor) targ;
                    if (arg.getName().equals(subscheduleName)) {
                        ValueTypeEnum type = arg.getRawDataType();
                        String subScheduleIdStr = originalCommand.tcTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_ID);
                        Object value = ValueUtil.parse(type, subScheduleIdStr);
                        subschedule = new PlainActivityArgument(subscheduleName, value, null, false);
                        // Set the subschedule ID inside the linked tc tracker
                        originalCommand.setSubscheduleId((Number) value);
                    } else if (arg.getName().equals(numCommandsName)) {
                        ValueTypeEnum type = arg.getRawDataType();
                        Object value = ValueUtil.parse(type, "1");
                        numElements = new PlainActivityArgument(numCommandsName, value, null, false);
                    } else if (arg.getRawDataType() == ValueTypeEnum.ABSOLUTE_TIME && execTimeArg == null) {
                        execTimeArg = new PlainActivityArgument(arg.getName(), targetTime, null, false);
                    } else if (arg.getRawDataType() == ValueTypeEnum.OCTET_STRING && commandArg == null) {
                        commandArg = new PlainActivityArgument(arg.getName(), originalCommand.tcTracker.getPacket().getPacket(), null, false);
                    }
                }
            }
            List<AbstractActivityArgument> arguments = new LinkedList<>();
            if(subschedule != null) {
                arguments.add(subschedule);
            }
            if(numElements != null) {
                arguments.add(numElements);
            }
            if(execTimeArg != null) {
                arguments.add(execTimeArg);
            }
            if(commandArg != null) {
                arguments.add(commandArg);
            }
            processingModel().startActivity(new ActivityRequest(descriptor.getExternalId(), descriptor.getPath(), arguments,
                    Map.of(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID, String.valueOf(originalCommand.tcTracker.getInvocation().getActivityOccurrenceId().asLong())),
                    originalCommand.tcTracker.getInvocation().getRoute(),
                    originalCommand.tcTracker.getInvocation().getSource()));
        }
    }

    private void reportActivityState(TcTracker tracker, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState, Instant executionTime) {
        context().getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, executionTime, status, nextState, null));
    }

    @Override
    public String getName() {
        return "Onboard Operations Scheduling Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, sp, pusType, pusSubtype, destination, source) -> !sp.isTelemetryPacket() || (pusType != null && pusType == 11);
    }

    @Override
    public int getServiceType() {
        return 11;
    }

    @Override
    public boolean isDirectHandler(TcTracker trackedTc) {
        return trackedTc.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
    }

    public void dispose() {
        scheduler.purge();
        scheduler.cancel();
        new HashSet<>(linkedActivityOccurrence2tcTracker.values()).forEach(o -> o.terminate(TcPhase.FAILED, Instant.now(), true)); // Avoid concurrent modification exception
        linkedActivityOccurrence2tcTracker.clear();
    }

    @Override
    protected OnboardOperationsSchedulingServiceConfiguration loadConfiguration(String serviceConfigurationPath) throws IOException {
        return OnboardOperationsSchedulingServiceConfiguration.load(new FileInputStream(serviceConfigurationPath));
    }

    private class LinkedTcTracker {

        private final TcTracker tcTracker;
        private volatile TimerTask scheduledOpening;
        private volatile Instant currentExecutionTime;
        private volatile Number subscheduleId;
        private volatile String lastAnnouncedStage = ActivityOccurrenceReport.RELEASE_REPORT_NAME;
        private volatile ActivityOccurrenceState lastAnnouncedState = ActivityOccurrenceState.RELEASE;

        public LinkedTcTracker(TcTracker tcTracker, Instant executionTime) {
            this.tcTracker = tcTracker;
            this.currentExecutionTime = executionTime;
        }

        public synchronized void informTcTransition(TcPhase phase, Instant phaseTime) {
            switch (phase) {
                case RELEASED: {
                    serviceBroker().informTcPacket(phase, phaseTime, tcTracker);
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, currentExecutionTime);
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION, null);
                    lastAnnouncedStage = Constants.STAGE_GROUND_STATION_UPLINK;
                    lastAnnouncedState = ActivityOccurrenceState.TRANSMISSION;
                }
                break;
                case UPLINKED: {
                    serviceBroker().informTcPacket(phase, phaseTime, tcTracker);
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.OK, ActivityOccurrenceState.SCHEDULING, null);
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.PENDING, ActivityOccurrenceState.SCHEDULING, null);
                    lastAnnouncedStage = Constants.STAGE_ONBOARD_RECEPTION;
                    lastAnnouncedState = ActivityOccurrenceState.TRANSMISSION;
                }
                break;
                case AVAILABLE_ONBOARD: {
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.PENDING, ActivityOccurrenceState.SCHEDULING, null);
                    lastAnnouncedStage = Constants.STAGE_SPACECRAFT_SCHEDULED;
                    lastAnnouncedState = ActivityOccurrenceState.SCHEDULING;
                }
                break;
                case COMPLETED: {
                    registerScheduledTc(this, currentExecutionTime);
                    serviceBroker().informTcPacket(TcPhase.SCHEDULED, phaseTime, tcTracker);
                    reportActivityState(tcTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.OK, ActivityOccurrenceState.SCHEDULING, currentExecutionTime);
                }
                break;
                case FAILED: {
                    terminate(phase, phaseTime, false);
                }
                break;
            }
        }

        private synchronized void registerScheduledTc(LinkedTcTracker tcTracker, Instant scheduledTime) {
            TimerTask scheduledTc = scheduledOpening;
            if(scheduledTc != null) {
                boolean okCancelled = scheduledTc.cancel();
                if(!okCancelled) {
                    return;
                }
            }
            scheduledOpening = new TimerTask() {
                @Override
                public void run() {
                    tcTracker.informOnboardAvailability();
                }
            };
            scheduler.schedule(scheduledOpening, new Date(scheduledTime.toEpochMilli() - VERIFICATION_AHEAD_MILLIS));
            scheduler.purge();
        }

        public void informOnboardAvailability() {
            serviceBroker().informTcPacket(TcPhase.AVAILABLE_ONBOARD, currentExecutionTime, tcTracker);
            reportActivityState(tcTracker, currentExecutionTime, ActivityOccurrenceState.EXECUTION, Constants.STAGE_ONBOARD_AVAILABILITY, ActivityReportState.EXPECTED, ActivityOccurrenceState.EXECUTION, currentExecutionTime);
            lastAnnouncedStage = Constants.STAGE_SPACECRAFT_SCHEDULED;
            lastAnnouncedState = ActivityOccurrenceState.SCHEDULING;
            // Remove tracker from map
            linkedActivityOccurrence2tcTracker.remove(tcTracker.getInvocation().getActivityOccurrenceId());
            scheduler.purge();
        }

        public void terminate(TcPhase phase, Instant phaseTime, boolean silently) {
            if(!silently) {
                serviceBroker().informTcPacket(phase, phaseTime, tcTracker);
                reportActivityState(tcTracker, phaseTime, lastAnnouncedState, lastAnnouncedStage, ActivityReportState.FATAL, lastAnnouncedState, null);
            }
            // Remove from map2
            if(scheduledOpening != null) {
                scheduledOpening.cancel();
            }
            linkedActivityOccurrence2tcTracker.remove(tcTracker.getInvocation().getActivityOccurrenceId());
            scheduler.purge();
        }

        public void setSubscheduleId(Number value) {
            subscheduleId = value;
        }
    }
}
