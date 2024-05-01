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

import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.AbstractTcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcPacketTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.services.OnboardOperationsSchedulingServiceConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServicePacketFilter;
import eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPhase;

import java.io.*;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the ECSS PUS 11 command scheduling service (limited to 11,4 and 11,3 commands).
 */
public class OnboardOperationsSchedulingService extends AbstractPacketService<OnboardOperationsSchedulingServiceConfiguration> {

    private static final Logger LOG = Logger.getLogger(OnboardOperationsSchedulingService.class.getName());

    public static final int VERIFICATION_AHEAD_MILLIS = 1000;

    private final Timer scheduler = new Timer();
    // This map contains the time-tagged TCs
    private final Map<IUniqueId, LinkedTcTracker> linkedActivityOccurrence2tcTracker = new HashMap<>();

    @Override
    public void postInitialisation() {
        // Reminder: support for 11,1, 11,2 and status report (minimal capability set) to be implemented,
        // not available in this version of the spacecraft driver
        // subscribeToRawDataBroker();
    }

    @Override
    protected void initialiseModelFrom(IArchive archiveToUse, Instant time) throws ReatmetricException {
        IRawDataArchive archive = archiveToUse.getArchive(IRawDataArchive.class);
        List<RawData> data = archive.retrieve(time, 1, RetrievalDirection.TO_PAST, new RawDataFilter(true, Constants.N_SCHEDULE_MODEL_STATE, null, Collections.singletonList(Constants.T_SCHEDULE_MODEL_STATE), Collections.singletonList(String.valueOf(spacecraftConfiguration().getId())), Collections.singletonList(Quality.GOOD)));
        if(!data.isEmpty()) {
            try {
                Map<IUniqueId, LinkedTcTracker> theMap = (Map<IUniqueId, LinkedTcTracker>) new ObjectInputStream(new ByteArrayInputStream(data.get(0).getContents())).readObject();
                for(Map.Entry<IUniqueId, LinkedTcTracker> entry : theMap.entrySet()) {
                    entry.getValue().setService(this);
                    this.linkedActivityOccurrence2tcTracker.put(entry.getKey(), entry.getValue());
                    // If the linked task is completed - 11,4 fully executed - then start with the timer again
                    if(entry.getValue().isCompleted()) {
                        entry.getValue().registerScheduledTc();
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new ReatmetricException(e);
            }
        } else {
            if(LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Onboard schedule model for spacecraft " + spacecraftConfiguration().getId() + " at time " + time + " not found");
            }
        }
    }

    @Override
    public synchronized void onTcUpdate(TcPhase phase, Instant phaseTime, AbstractTcTracker tracker) {
        // Here we process only TC packets
        if(!(tracker instanceof TcPacketTracker)) {
            return;
        }
        TcPacketTracker tcPacketTracker = (TcPacketTracker) tracker;
        // We are interested in TCs having scheduled information in the ENCODED phase
        if(phase == TcPhase.ENCODED && tcPacketTracker.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME)) {
            String scheduledTime = tcPacketTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
            if (scheduledTime != null && !scheduledTime.isBlank()) {
                // This is a time-tagged command, so a 11,4 must be built and provided to the lower layers
                // If the time is wrongly typed, here you have an exception. In this case, the activity remains pending forever if you do not report it here.
                Instant targetTime = null;
                try {
                    targetTime = Instant.parse(scheduledTime);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error while dispatching 11,4 command for activity " + tcPacketTracker.getInvocation().getPath() + " (" + tcPacketTracker.getInvocation().getActivityOccurrenceId() + "): " + e.getMessage(), e);
                    serviceBroker().informTc(TcPhase.FAILED, phaseTime, tcPacketTracker);
                    reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE, null);
                    return;
                }
                // If you will get any notification about this activity at the various stages, then also inform accordingly the original TcTracker
                LinkedTcTracker linkedTracker = new LinkedTcTracker(tcPacketTracker, targetTime);
                linkedTracker.setService(this);
                linkedActivityOccurrence2tcTracker.put(tcPacketTracker.getInvocation().getActivityOccurrenceId(), linkedTracker);
                // Dispatch a new activity
                try {
                    dispatch(targetTime, linkedTracker);
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Error while dispatching 11,4 command for activity " + tcPacketTracker.getInvocation().getPath() + " (" + tcPacketTracker.getInvocation().getActivityOccurrenceId() + "): " + e.getMessage(), e);
                    linkedTracker.terminate(TcPhase.FAILED, phaseTime, false);
                }
                // Save state
                storeState();
                // Stop here
                return;
            }
        }
        // If it is a 11,4, check for the tracked activity and announce phases accordingly
        if(tcPacketTracker.getInfo().getPusHeader().getServiceType() == 11 && tcPacketTracker.getInfo().getPusHeader().getServiceSubType() == 4) {
            // Get the original tracker
            // It could be more than one activity linked to the PUS 11,4 TC, so use a concatenation of IDs, separated by |. The approach is identical though (use a for loop).
            // But in this implementation we support a single TC per PUS 11,4, so the above is not needed.
            String occIdStr = tcPacketTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID);
            LinkedTcTracker linkedTcTracker = linkedActivityOccurrence2tcTracker.get(new LongUniqueId(Long.parseLong(occIdStr)));
            // Update the tracking information
            linkedTcTracker.informTcTransition(phase, phaseTime);
            // Save state
            storeState();
        }
        // If it is a 11,3 in status COMPLETED, the schedule is reset
        if(tcPacketTracker.getInfo().getPusHeader().getServiceType() == 11 && tcPacketTracker.getInfo().getPusHeader().getServiceSubType() == 3 && phase == TcPhase.COMPLETED) {
            // Get the original tracker
            Set<IUniqueId> keys = linkedActivityOccurrence2tcTracker.keySet();
            for(IUniqueId id : keys) {
                LinkedTcTracker linkedTcTracker = linkedActivityOccurrence2tcTracker.remove(id);
                linkedTcTracker.terminate(TcPhase.FAILED, phaseTime, false);
            }
            // Save state
            storeState();
        }
        // In any case, if the tcTracker is present in the linkedActivityOccurrence2tcTracker and results as STARTED or COMPLETED or FAILED, it should be removed silently
        if(phase == TcPhase.STARTED || phase == TcPhase.COMPLETED || phase == TcPhase.FAILED) {
            IUniqueId occId = tcPacketTracker.getInvocation().getActivityOccurrenceId();
            LinkedTcTracker track = linkedActivityOccurrence2tcTracker.remove(occId);
            if(track != null) {
                track.terminate(phase, phaseTime, true);
                // Save state
                storeState();
            }
        }
    }

    /**
     * Store the state of the model, so that it is possible to restore it from the archive.
     */
    private synchronized void storeState() {
        try {
            // Serialize map
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this.linkedActivityOccurrence2tcTracker);
            oos.flush();
            RawData rd = new RawData(context().getRawDataBroker().nextRawDataId(), Instant.now(), Constants.N_SCHEDULE_MODEL_STATE, Constants.T_SCHEDULE_MODEL_STATE, "", String.valueOf(spacecraftConfiguration().getId()), Quality.GOOD, null, bos.toByteArray(), Instant.now(), driverName(), null);
            context().getRawDataBroker().distribute(Collections.singletonList(rd));
        } catch (ReatmetricException | IOException e) {
            LOG.log(Level.SEVERE, "Cannot store on-board schedule model for spacecraft " + spacecraftConfiguration().getId() + ": " + e.getMessage(), e);
        }
    }

    private void dispatch(Instant targetTime, LinkedTcTracker originalCommand) throws ReatmetricException, RemoteException {
        SystemEntityPath activity = SystemEntityPath.fromString(configuration().getActivityPath());
        ActivityDescriptor descriptor = context().getServiceFactory().getActivityOccurrenceDataMonitorService().getDescriptor(activity);
        // Derive the various arguments you need: sub-schedule-id (opt.), if array or not, first time ABSOLUTE_TIME of array is execution time, last field OCTET_STRING of array is the command
        String subscheduleName = configuration().getSubscheduleIdName();
        String numCommandsName = configuration().getNumCommandsName();
        PlainActivityArgument subschedule = null;
        // time-correlate the targetTime to OBT
        ITimeCorrelation timeCorrelationService = serviceBroker().locate(eu.dariolucia.reatmetric.driver.spacecraft.services.ITimeCorrelation.class);
        if(timeCorrelationService != null) {
            targetTime = timeCorrelationService.toObt(targetTime);
        }
        if(configuration().isArrayUsed()) {
            // Assume 1, 2 or 3 parameters: sub-schedule ID if subscheduleName is not null; num-elements if numCommandsName is not null; array definition (mandatory)
            PlainActivityArgument numElements = null;
            ArrayActivityArgument listCommands = null;
            for (AbstractActivityArgumentDescriptor arg : descriptor.getArgumentDescriptors()) {
                if (arg instanceof ActivityPlainArgumentDescriptor && arg.getName().equals(subscheduleName)) {
                    ValueTypeEnum type = ((ActivityPlainArgumentDescriptor) arg).getRawDataType();
                    String subScheduleIdStr = originalCommand.tcPacketTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_ID);
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
                            commandArg = new PlainActivityArgument(innerArg.getName(), originalCommand.tcPacketTracker.getPacket().getPacket(), null, false);
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
                    Map.of(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID, String.valueOf(originalCommand.tcPacketTracker.getInvocation().getActivityOccurrenceId().asLong())),
                    originalCommand.tcPacketTracker.getInvocation().getRoute(),
                    originalCommand.tcPacketTracker.getInvocation().getSource()));
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
                        String subScheduleIdStr = originalCommand.tcPacketTracker.getInvocation().getProperties().get(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_ID);
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
                        commandArg = new PlainActivityArgument(arg.getName(), originalCommand.tcPacketTracker.getPacket().getPacket(), null, false);
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
                    Map.of(Constants.ACTIVITY_PROPERTY_SUBSCHEDULE_TRACKING_ID, String.valueOf(originalCommand.tcPacketTracker.getInvocation().getActivityOccurrenceId().asLong())),
                    originalCommand.tcPacketTracker.getInvocation().getRoute(),
                    originalCommand.tcPacketTracker.getInvocation().getSource()));
        }
    }

    private void reportActivityState(TcPacketTracker tracker, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState, Instant executionTime) {
        context().getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, executionTime, status, nextState, null));
    }

    @Override
    public String getName() {
        return "Onboard Operations Scheduling Service";
    }

    @Override
    public IServicePacketFilter getSubscriptionFilter() {
        return (rd, sp, pusType, pusSubtype, destination, source) -> sp instanceof SpacePacket && (!((SpacePacket) sp).isTelemetryPacket() || (pusType != null && pusType == 11));
    }

    @Override
    public int getServiceType() {
        return 11;
    }

    @Override
    public boolean isDirectHandler(AbstractTcTracker trackedTc) {
        return trackedTc instanceof TcPacketTracker &&
                trackedTc.getInvocation().getProperties().containsKey(Constants.ACTIVITY_PROPERTY_SCHEDULED_TIME);
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

    /*
     * I know, the design of this service is ugly and should be refactored, but as this is a demonstrator, I will keep it as it is know.
     * After all, the class is still reasonable short in terms of lines of code.
     */
    private static class LinkedTcTracker implements Serializable {

        private final TcPacketTracker tcPacketTracker;
        private volatile transient TimerTask scheduledOpening;
        private volatile Instant currentExecutionTime;
        private volatile Number subscheduleId;
        private volatile String lastAnnouncedStage = ActivityOccurrenceReport.RELEASE_REPORT_NAME;
        private volatile ActivityOccurrenceState lastAnnouncedState = ActivityOccurrenceState.RELEASE;
        private volatile boolean completed = false;
        private volatile transient OnboardOperationsSchedulingService service;

        public LinkedTcTracker(TcPacketTracker tcPacketTracker, Instant executionTime) {
            this.tcPacketTracker = tcPacketTracker;
            this.currentExecutionTime = executionTime;
        }

        public void setService(OnboardOperationsSchedulingService service) {
            this.service = service;
        }

        public synchronized void informTcTransition(TcPhase phase, Instant phaseTime) {
            switch (phase) {
                case RELEASED: {
                    service.serviceBroker().informTc(phase, phaseTime, tcPacketTracker);
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION, currentExecutionTime);
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION, null);
                    lastAnnouncedStage = Constants.STAGE_GROUND_STATION_UPLINK;
                    lastAnnouncedState = ActivityOccurrenceState.TRANSMISSION;
                }
                break;
                case UPLINKED: {
                    service.serviceBroker().informTc(phase, phaseTime, tcPacketTracker);
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.OK, ActivityOccurrenceState.SCHEDULING, null);
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.PENDING, ActivityOccurrenceState.SCHEDULING, null);
                    lastAnnouncedStage = Constants.STAGE_ONBOARD_RECEPTION;
                    lastAnnouncedState = ActivityOccurrenceState.TRANSMISSION;
                }
                break;
                case AVAILABLE_ONBOARD: {
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.PENDING, ActivityOccurrenceState.SCHEDULING, null);
                    lastAnnouncedStage = Constants.STAGE_SPACECRAFT_SCHEDULED;
                    lastAnnouncedState = ActivityOccurrenceState.SCHEDULING;
                }
                break;
                case COMPLETED: {
                    completed = true;
                    registerScheduledTc();
                    service.serviceBroker().informTc(TcPhase.SCHEDULED, phaseTime, tcPacketTracker);
                    service.reportActivityState(tcPacketTracker, phaseTime, ActivityOccurrenceState.SCHEDULING, Constants.STAGE_SPACECRAFT_SCHEDULED, ActivityReportState.OK, ActivityOccurrenceState.SCHEDULING, currentExecutionTime);
                }
                break;
                case FAILED: {
                    terminate(phase, phaseTime, false);
                }
                break;
            }
        }

        public synchronized void registerScheduledTc() {
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
                    informOnboardAvailability();
                }
            };
            Date toActivate = new Date(currentExecutionTime.toEpochMilli() - VERIFICATION_AHEAD_MILLIS);
            service.scheduler.schedule(scheduledOpening, toActivate);
            service.scheduler.purge();
        }

        public synchronized void informOnboardAvailability() {
            service.serviceBroker().informTc(TcPhase.AVAILABLE_ONBOARD, currentExecutionTime, tcPacketTracker);
            service.reportActivityState(tcPacketTracker, currentExecutionTime, ActivityOccurrenceState.EXECUTION, Constants.STAGE_ONBOARD_AVAILABILITY, ActivityReportState.EXPECTED, ActivityOccurrenceState.EXECUTION, currentExecutionTime);
            lastAnnouncedStage = Constants.STAGE_SPACECRAFT_SCHEDULED;
            lastAnnouncedState = ActivityOccurrenceState.SCHEDULING;
            // Remove tracker from map
            service.linkedActivityOccurrence2tcTracker.remove(tcPacketTracker.getInvocation().getActivityOccurrenceId());
            service.scheduler.purge();
            // Save state to be done here, as this method is run by a different thread
            service.storeState();
        }

        public void terminate(TcPhase phase, Instant phaseTime, boolean silently) {
            if(!silently) {
                service.serviceBroker().informTc(phase, phaseTime, tcPacketTracker);
                service.reportActivityState(tcPacketTracker, phaseTime, lastAnnouncedState, lastAnnouncedStage, ActivityReportState.FATAL, lastAnnouncedState, null);
            }
            // Remove from map2
            if(scheduledOpening != null) {
                scheduledOpening.cancel();
            }
            service.linkedActivityOccurrence2tcTracker.remove(tcPacketTracker.getInvocation().getActivityOccurrenceId());
            service.scheduler.purge();
            // State stored by the caller
        }

        public void setSubscheduleId(Number value) {
            subscheduleId = value;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
