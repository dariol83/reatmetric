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

package eu.dariolucia.reatmetric.driver.spacecraft.sle;

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.incoming.pdus.CltuThrowEventInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.incoming.pdus.CltuTransferDataInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.outgoing.pdus.*;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.pdu.PduFactoryUtil;
import eu.dariolucia.ccsds.sle.utl.si.ProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstance;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstanceBindingStateEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuDiagnosticsStrings;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuParameterEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuServiceInstance;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuUplinkStatusEnum;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.ForwardDataUnitProcessingStatus;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IActivityExecutor;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu.ICltuConnector;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class has a subscription mechanism, which allows subscribers to know the status of a CLTU (ACCEPTED, REJECTED, UPLINKED, FAILED, DISCARDED).
 */
public class CltuServiceInstanceManager extends SleServiceInstanceManager<CltuServiceInstance, CltuServiceInstanceConfiguration> implements IActivityExecutor, ICltuConnector {

    private static final Logger LOG = Logger.getLogger(CltuServiceInstanceManager.class.getName());
    private static final String FIRST_CLTU_ID_KEY = "cltu.first.id";

    private volatile CltuUplinkStatusEnum uplinkStatus = CltuUplinkStatusEnum.UPLINK_STATUS_NOT_AVAILABLE;

    private volatile int bufferCapacity = -1;
    private final AtomicInteger estimatedFreeBuffer = new AtomicInteger();

    private final Map<Long, CltuTracker> cltuId2tracker = new ConcurrentHashMap<>(); // CLTU invocation ID to CLTU tracker
    private final Map<Long, CltuTransferDataInvocation> invokeId2InvocationCorrelationMap = new ConcurrentHashMap<>(); // invokeID to dispatched operation
    private final AtomicLong cltuCounter = new AtomicLong(0); // CLTU invocation ID
    private final Semaphore cltuIdRefreshSemaphore = new Semaphore(0);

    private final Map<Long, IActivityHandler.ActivityInvocation> eventId2tracker = new ConcurrentHashMap<>();
    private final Map<Long, CltuThrowEventInvocation> invokeId2ThrowEventInvocationCorrelationMap = new ConcurrentHashMap<>();
    private final AtomicLong eventInvocationIdCounter = new AtomicLong(0);
    private final Semaphore eventIdRefreshSemaphore = new Semaphore(0);

    private final List<IForwardDataUnitStatusSubscriber> subscribers = new CopyOnWriteArrayList<>();

    public CltuServiceInstanceManager(String driverName, PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IServiceCoreContext context) {
        super(driverName, peerConfiguration, siConfiguration, spacecraftConfiguration, context);
    }

    @Override
    public void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation) {
        // Not needed
    }

    @Override
    public void register(IForwardDataUnitStatusSubscriber listener) {
        this.subscribers.add(listener);
    }

    @Override
    public void deregister(IForwardDataUnitStatusSubscriber listener) {
        this.subscribers.remove(listener);
    }

    @Override
    protected CltuServiceInstance createServiceInstance(PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration) {
        return new CltuServiceInstance(peerConfiguration, siConfiguration);
    }

    @Override
    protected void addToInitialisationMap(Map<String, Object> initialisationMap, Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap) {
        super.addToInitialisationMap(initialisationMap, initialisationDescriptionMap);
        initialisationMap.put(FIRST_CLTU_ID_KEY, (long) 1); // long because the data type is UNSIGNED_INTEGER
        initialisationDescriptionMap.put(FIRST_CLTU_ID_KEY, Pair.of("First CLTU ID", ValueTypeEnum.UNSIGNED_INTEGER));
    }

    @Override
    protected boolean isStartReturn(Object operation) {
        return operation instanceof CltuStartReturn;
    }

    @Override
    protected void handleOperation(Object operation) {
        if (operation instanceof CltuAsyncNotifyInvocation) {
            process((CltuAsyncNotifyInvocation) operation);
        } else if (operation instanceof CltuTransferDataReturn) {
            process((CltuTransferDataReturn) operation);
        } else if (operation instanceof CltuThrowEventReturn) {
            process((CltuThrowEventReturn) operation);
        } else if (operation instanceof SleScheduleStatusReportReturn) {
            process((SleScheduleStatusReportReturn) operation);
        } else if (operation instanceof CltuStatusReportInvocation) {
            process((CltuStatusReportInvocation) operation);
        } else if (operation instanceof CltuGetParameterReturn) {
            process((CltuGetParameterReturn) operation);
        } else if (operation instanceof CltuGetParameterReturnV1toV3) {
            process((CltuGetParameterReturnV1toV3) operation);
        } else if (operation instanceof CltuGetParameterReturnV4) {
            process((CltuGetParameterReturnV4) operation);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": discarding unhandled CLTU operation: " + operation.getClass().getName());
        }
    }

    private void process(CltuGetParameterReturnV4 operation) {
        if (operation.getResult().getPositiveResult() != null) {
            if (operation.getResult().getPositiveResult().getParCltuIdentification() != null) {
                this.cltuCounter.set(operation.getResult().getPositiveResult().getParCltuIdentification().getParameterValue().longValue());
                this.cltuIdRefreshSemaphore.release();
            }
            if (operation.getResult().getPositiveResult().getParEventInvocationIdentification() != null) {
                this.eventInvocationIdCounter.set(operation.getResult().getPositiveResult().getParEventInvocationIdentification().getParameterValue().longValue());
                this.eventIdRefreshSemaphore.release();
            }
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": get parameter returned negative result: " + CltuDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        }
    }

    private void process(CltuGetParameterReturnV1toV3 operation) {
        if (operation.getResult().getPositiveResult() != null) {
            if (operation.getResult().getPositiveResult().getParCltuIdentification() != null) {
                this.cltuCounter.set(operation.getResult().getPositiveResult().getParCltuIdentification().getParameterValue().longValue());
                this.cltuIdRefreshSemaphore.release();
            }
            if (operation.getResult().getPositiveResult().getParEventInvocationIdentification() != null) {
                this.eventInvocationIdCounter.set(operation.getResult().getPositiveResult().getParEventInvocationIdentification().getParameterValue().longValue());
                this.eventIdRefreshSemaphore.release();
            }
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": get parameter returned negative result: " + CltuDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        }
    }

    private void process(CltuGetParameterReturn operation) {
        if (operation.getResult().getPositiveResult() != null) {
            if (operation.getResult().getPositiveResult().getParCltuIdentification() != null) {
                this.cltuCounter.set(operation.getResult().getPositiveResult().getParCltuIdentification().getParameterValue().longValue());
                this.cltuIdRefreshSemaphore.release();
            }
            if (operation.getResult().getPositiveResult().getParEventInvocationIdentification() != null) {
                this.eventInvocationIdCounter.set(operation.getResult().getPositiveResult().getParEventInvocationIdentification().getParameterValue().longValue());
                this.eventIdRefreshSemaphore.release();
            }
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": get parameter returned negative result: " + CltuDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        }
    }

    private void process(CltuStatusReportInvocation operation) {
        if(LOG.isLoggable(Level.FINER)) {
            LOG.finer(serviceInstance.getServiceInstanceIdentifier() + ": status report received: CLTU free buffer=" + operation.getCltuBufferAvailable().intValue() +
                    ", CLTU processed=" + operation.getNumberOfCltusProcessed() +
                    ", CLTU radiated=" + operation.getNumberOfCltusRadiated() +
                    ", CLTU received=" + operation.getNumberOfCltusReceived() +
                    ", uplink status=" + CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()]);
        }
        // The first report after the bind should report the effective buffer capacity
        if (this.bufferCapacity == 0) {
            this.bufferCapacity = operation.getCltuBufferAvailable().intValue();
            this.estimatedFreeBuffer.set(operation.getCltuBufferAvailable().intValue());
        }
        this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getCltuProductionStatus().intValue());
        updateProductionStatus(prodStatus, uplinkStatus);
    }

    private void process(SleScheduleStatusReportReturn operation) {
        if (operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": negative CLTU SCHEDULE STATUS REPORT return: " + CltuDiagnosticsStrings.getScheduleStatusReportDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer(serviceInstance.getServiceInstanceIdentifier() + ": CLTU SCHEDULE STATUS REPORT positive return");
            }
        }
    }

    private void process(CltuThrowEventReturn operation) {
        CltuThrowEventInvocation invocation = this.invokeId2ThrowEventInvocationCorrelationMap.remove(operation.getInvokeId().longValue());
        if (invocation == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": THROW-EVENT return with invokeId " + operation.getInvokeId().longValue() + " not sent by this system");
            return;
        }
        long eventId = invocation.getEventInvocationIdentification().longValue();
        IActivityHandler.ActivityInvocation tracker = this.eventId2tracker.get(eventId);
        if (tracker == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": THROW-EVENT ID " + eventId + " not sent by this system");
            return;
        }
        if (operation.getResult().getPositiveResult() != null) {
            LOG.log(Level.INFO, serviceInstance.getServiceInstanceIdentifier() + ": THROW-EVENT ID " + eventId + " accepted");
            reportThrowEventState(tracker, Instant.now(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION);
            reportThrowEventState(tracker, Instant.now(), ActivityOccurrenceState.EXECUTION, Constants.STAGE_GROUND_STATION_EXECUTION, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION);
        } else {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": negative THROW-EVENT return for THROW-EVENT ID " + eventId + ": " + CltuDiagnosticsStrings.getThrowEventDiagnostic(operation.getResult().getNegativeResult()));
            reportThrowEventState(tracker, Instant.now(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            this.eventId2tracker.remove(eventId);
            refreshExpectedEventId();
        }
    }

    private void refreshExpectedEventId() {
        this.serviceInstance.getParameter(CltuParameterEnum.EXPECTED_EVENT_INVOCATION_IDENTIFICATION);
        try {
            boolean acquired = this.eventIdRefreshSemaphore.tryAcquire(10, TimeUnit.SECONDS);
            if (!acquired) {
                LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": acquisition of expected THROW-EVENT ID failed due to timeout");
            }
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": acquisition of expected THROW-EVENT ID failed due to interruption", e);
        }
    }

    private void process(CltuTransferDataReturn operation) {
        CltuTransferDataInvocation invocation = this.invokeId2InvocationCorrelationMap.remove(operation.getInvokeId().longValue());
        if (invocation == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": CLTU return with invokeId " + operation.getInvokeId().longValue() + " not sent by this system");
            return;
        }
        long cltuId = invocation.getCltuIdentification().longValue();
        CltuTracker tracker = this.cltuId2tracker.get(cltuId);
        if (tracker == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": CLTU ID " + cltuId + " not sent by this system");
            return;
        }
        if (operation.getResult().getPositiveResult() != null) {
            LOG.log(Level.INFO, serviceInstance.getServiceInstanceIdentifier() + ": CLTU ID " + tracker.getExternalId() + " accepted");
            informSubscribers(tracker.getExternalId(), ForwardDataUnitProcessingStatus.ACCEPTED, Constants.STAGE_GROUND_STATION_RECEPTION, Constants.STAGE_GROUND_STATION_UPLINK);
        } else {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": negative CLTU TRANSFER DATA return for CLTU ID " + tracker.getExternalId() + ": " + CltuDiagnosticsStrings.getTransferDataDiagnostic(operation.getResult().getNegativeResult()));
            informSubscribers(tracker.getExternalId(), ForwardDataUnitProcessingStatus.REJECTED, Constants.STAGE_GROUND_STATION_RECEPTION, null);
            this.cltuId2tracker.remove(cltuId);
            increaseEstimatedFreeBuffer(tracker.getCltu().length);
            refreshExpectedCltuId();
        }
    }

    private void refreshExpectedCltuId() {
        this.serviceInstance.getParameter(CltuParameterEnum.EXPECTED_SLDU_IDENTIFICATION);
        try {
            boolean acquired = this.cltuIdRefreshSemaphore.tryAcquire(10, TimeUnit.SECONDS);
            if (!acquired) {
                LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": acquisition of expected CLTU ID failed due to timeout");
            }
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": acquisition of expected CLTU ID failed due to interruption", e);
        }
    }

    private void process(CltuAsyncNotifyInvocation operation) {
        if (operation.getCltuNotification().getCltuRadiated() != null && operation.getCltuLastProcessed().getCltuProcessed() != null) {
            // The CLTU identified by the value of the cltu-last-processed parameter successfully completed radiation
            long cltuId = operation.getCltuLastProcessed().getCltuProcessed().getCltuIdentification().longValue();
            Instant radiationTime = null;
            if (operation.getCltuLastProcessed().getCltuProcessed().getStartRadiationTime() != null) {
                radiationTime = PduFactoryUtil.toInstant(operation.getCltuLastProcessed().getCltuProcessed().getStartRadiationTime());
            }
            if (radiationTime == null) {
                radiationTime = Instant.now();
            }
            cltuRadiated(cltuId, radiationTime);
        } else if (operation.getCltuNotification().getSlduExpired() != null && operation.getCltuLastProcessed().getCltuProcessed() != null) {
            // Radiation of the CLTU identified by the value of the cltu-lastprocessed parameter did not begin by the time specified in the
            // latestradiation-time parameter of the associated CLTU-TRANSFER-DATA invocation. No further CLTUs shall be radiated; buffered CLTUs shall be discarded;
            // and further CLTU-TRANSFER-DATA invocations shall be blocked, i.e., rejected with an 'unable to process' diagnostic.
            long cltuId = operation.getCltuLastProcessed().getCltuProcessed().getCltuIdentification().longValue();
            cltuNotRadiated(cltuId);
            purgeOutstandingCltus();
            refreshExpectedCltuId();
        } else if (operation.getCltuNotification().getProductionInterrupted() != null) {
            // No further CLTUs shall be radiated; buffered CLTUs shall be discarded; and, in state 3 (‘active’), further CLTU-TRANSFER-DATA
            // invocations shall be blocked, i.e., rejected with an ‘unable to process’ diagnostic
            LOG.log(Level.SEVERE, serviceInstance.getServiceInstanceIdentifier() + ": production status is interrupted");
            purgeOutstandingCltus();
        } else if (operation.getCltuNotification().getProductionHalted() != null) {
            // The production process has been stopped by management action. No further CLTUs shall be radiated; buffered CLTUs shall be discarded; and,
            // in state 3 ('active'), further CLTU-TRANSFER-DATA invocations shall be blocked, i.e., rejected with an 'unable to process' diagnostic.
            LOG.log(Level.SEVERE, serviceInstance.getServiceInstanceIdentifier() + ": production status is halted");
            purgeOutstandingCltus();
        } else if (operation.getCltuNotification().getProductionOperational() != null) {
            LOG.log(Level.INFO, serviceInstance.getServiceInstanceIdentifier() + ": production status is operational");
        } else if (operation.getCltuNotification().getActionListCompleted() != null) {
            // The THROW-EVENT identified by the value of the action-list-completed parameter successfully completed
            long eventId = operation.getCltuNotification().getActionListCompleted().longValue();
            throwEventCompleted(eventId, true);
        } else if (operation.getCltuNotification().getActionListNotCompleted() != null) {
            // The THROW-EVENT identified by the value of the action-list-not-completed parameter failed execution
            long eventId = operation.getCltuNotification().getActionListNotCompleted().longValue();
            throwEventCompleted(eventId, false);
        } else if (operation.getCltuNotification().getEventConditionEvFalse() != null) {
            // The THROW-EVENT identified by the value of the event condition parameter failed execution due to failed conditions
            long eventId = operation.getCltuNotification().getEventConditionEvFalse().longValue();
            throwEventCompleted(eventId, false);
        } else if (operation.getCltuNotification().getBufferEmpty() != null) {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": CLTU buffer empty");
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Unknown CLTU ASYNC NOTIFY received");
        }
        this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
        updateProductionStatus(prodStatus, uplinkStatus);
    }

    private void throwEventCompleted(long eventId, boolean success) {
        LOG.log(success ? Level.INFO : Level.SEVERE, serviceInstance.getServiceInstanceIdentifier() + ": THROW-EVENT " + eventId + " " + (success ? "completed" : "failed"));
        IActivityHandler.ActivityInvocation tracker = this.eventId2tracker.remove(eventId);
        if (tracker != null) {
            reportThrowEventState(tracker, Instant.now(), ActivityOccurrenceState.EXECUTION, Constants.STAGE_GROUND_STATION_EXECUTION, success ? ActivityReportState.OK : ActivityReportState.FATAL, success ? ActivityOccurrenceState.VERIFICATION : ActivityOccurrenceState.EXECUTION);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": received notification of completion for THROW-EVENT " + eventId + " not present in the system");
        }
    }

    private void purgeOutstandingCltus() {
        Set<Long> discardedCltus = new HashSet<>(this.cltuId2tracker.keySet());
        for (Long discardedId : discardedCltus) {
            cltuNotRadiated(discardedId);
        }
    }

    private void cltuRadiated(long cltuId, Instant radiationTime) {
        LOG.log(Level.INFO, serviceInstance.getServiceInstanceIdentifier() + ": CLTU " + cltuId + " radiated");
        CltuTracker tracker = this.cltuId2tracker.remove(cltuId);
        if (tracker != null) {
            informSubscribers(tracker.getExternalId(), ForwardDataUnitProcessingStatus.UPLINKED, radiationTime, Constants.STAGE_GROUND_STATION_UPLINK, null);
            increaseEstimatedFreeBuffer(tracker.getCltu().length);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": received notification of radiation for CLTU " + cltuId + " not present in the system");
        }
    }

    private boolean decreaseEstimatedFreeBuffer(int amount) {
        synchronized (this.estimatedFreeBuffer) {
            while (estimatedFreeBuffer.get() - amount < 0) {
                try {
                    estimatedFreeBuffer.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            estimatedFreeBuffer.addAndGet(-amount);
            return true;
        }
    }

    private void increaseEstimatedFreeBuffer(int amount) {
        synchronized (this.estimatedFreeBuffer) {
            this.estimatedFreeBuffer.addAndGet(amount);
            estimatedFreeBuffer.notifyAll();
        }
    }

    private void cltuNotRadiated(long cltuId) {
        LOG.log(Level.INFO, serviceInstance.getServiceInstanceIdentifier() + ": CLTU " + cltuId + " not radiated");
        CltuTracker tracker = this.cltuId2tracker.remove(cltuId);
        if (tracker != null) {
            informSubscribers(tracker.getExternalId(), ForwardDataUnitProcessingStatus.UPLINK_FAILED, Constants.STAGE_GROUND_STATION_UPLINK, null);
            increaseEstimatedFreeBuffer(tracker.getCltu().length);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": received radiation problem for CLTU " + cltuId + " not present in the system");
        }
    }

    private void updateProductionStatus(ProductionStatusEnum prodStatus, CltuUplinkStatusEnum status) {
        if (prodStatus == ProductionStatusEnum.RUNNING && status == CltuUplinkStatusEnum.NOMINAL) {
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer(serviceInstance.getServiceInstanceIdentifier() + ": Production status " + prodStatus + ", uplink status " + status);
            }
            updateAlarmState(AlarmState.NOMINAL);
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Production status " + prodStatus + ", uplink status " + status);
            updateAlarmState(AlarmState.ALARM);
        }
    }

    @Override
    protected void finalizeConnection() {
        if (serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE || serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY) {
            // Reset the buffer capacity
            this.bufferCapacity = 0;
            // Reset event id
            this.eventInvocationIdCounter.set(0);
            // Activate reporting
            if (siConfiguration.getReportingCycle() != 0) {
                serviceInstance.scheduleStatusReport(false, siConfiguration.getReportingCycle());
            }
        }
    }

    @Override
    protected void sendStart() {
        this.cltuCounter.set((Long) getInitialisationMap().get(FIRST_CLTU_ID_KEY));
        this.serviceInstance.start(this.cltuCounter.get());
    }

    @Override
    protected void sendStop() {
        serviceInstance.stop();
    }

    @Override
    public void sendCltu(byte[] encodedCltu, long externalId) {
        if (this.serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of CLTU with external ID " + externalId + " failed: service instance state is " + this.serviceInstance.getCurrentBindingState());
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED, null, null);
            return;
        }
        boolean goAhead = decreaseEstimatedFreeBuffer(encodedCltu.length);
        if (!goAhead) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of CLTU with external ID " + externalId + " failed: remote CLTU buffer availability failed");
            informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASE_FAILED, null, null);
            return;
        }
        long thisCounter = this.cltuCounter.getAndIncrement();
        this.cltuId2tracker.put(thisCounter, new CltuTracker(externalId, encodedCltu));
        informSubscribers(externalId, ForwardDataUnitProcessingStatus.RELEASED, null, Constants.STAGE_GROUND_STATION_RECEPTION);
        LOG.log(Level.INFO, "Sending CLTU with ID " + externalId + ": " + StringUtil.toHexDump(encodedCltu));
        this.serviceInstance.transferData(thisCounter, null, null, 20000000, true, encodedCltu);
    }

    @Override
    public void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        try {
            sendThrowEvent(activityInvocation);
        } catch (Exception e) {
            throw new ActivityHandlingException("Cannot process THROW-EVENT: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(Constants.SLE_CLTU_THROW_EVENT_ACTIVITY_TYPE);
    }

    @Override
    public List<String> getSupportedRoutes() {
        return Collections.singletonList(getServiceInstanceIdentifier());
    }

    @Override
    public void abort(int activityId, IUniqueId activityOccurrenceId) {
        // An activity executed by this executor cannot be aborted
    }

    public void sendThrowEvent(IActivityHandler.ActivityInvocation activityInvocation) {
        int eventIdentifier = ((Number) activityInvocation.getArguments().get(Constants.SLE_CLTU_THROW_EVENT_IDENTIFIER_ARG_NAME)).intValue();
        byte[] eventQualifier = (byte[]) activityInvocation.getArguments().get(Constants.SLE_CLTU_THROW_EVENT_QUALIFIER_ARG_NAME);
        if (this.serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE &&  this.serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.READY) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of THROW-EVENT " + eventIdentifier + "[" + StringUtil.toHexDump(eventQualifier) + "] failed: service instance state is " + this.serviceInstance.getCurrentBindingState());
            reportThrowEventState(activityInvocation, Instant.now(), ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL, ActivityOccurrenceState.RELEASE);
            return;
        }
        long thisCounter = this.eventInvocationIdCounter.getAndIncrement();
        this.eventId2tracker.put(thisCounter, activityInvocation);
        LOG.log(Level.INFO, "Sending THROW-EVENT " + eventIdentifier + "[" + StringUtil.toHexDump(eventQualifier) + "] with ID " + thisCounter);
        reportThrowEventState(activityInvocation, Instant.now(), ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
        reportThrowEventState(activityInvocation, Instant.now(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        this.serviceInstance.throwEvent(thisCounter, eventIdentifier, eventQualifier);
    }

    private void reportThrowEventState(IActivityHandler.ActivityInvocation a, Instant t, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(a.getActivityId(), a.getActivityOccurrenceId(), name, t, state, null, status, nextState, null));
    }

    @Override
    public void onPduSentError(ServiceInstance si, Object operation, String name, byte[] encodedOperation, String error, Exception exception) {
        super.onPduSentError(si, operation, name, encodedOperation, error, exception);
        // handle error on CLTU transfer data transmission
        if (operation instanceof CltuTransferDataInvocation) {
            long cltuId = ((CltuTransferDataInvocation) operation).getCltuIdentification().longValue();
            CltuTracker tracker = cltuId2tracker.remove(cltuId);
            if (tracker != null) {
                increaseEstimatedFreeBuffer(tracker.getCltu().length);
                informSubscribers(tracker.getExternalId(), ForwardDataUnitProcessingStatus.RELEASE_FAILED, null, null);
            }
        }
        // handle error on throw event transmission
        if (operation instanceof CltuThrowEventInvocation) {
            long eventId = ((CltuThrowEventInvocation) operation).getEventInvocationIdentification().longValue();
            IActivityHandler.ActivityInvocation tracker = eventId2tracker.remove(eventId);
            if (tracker != null) {
                reportThrowEventState(tracker, Instant.now(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
            }
        }
    }

    @Override
    public void onPduSent(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
        super.onPduSent(si, operation, name, encodedOperation);
        if(operation instanceof CltuTransferDataInvocation) {
            this.invokeId2InvocationCorrelationMap.put(((CltuTransferDataInvocation) operation).getInvokeId().longValue(), (CltuTransferDataInvocation) operation);
        }
        if(operation instanceof CltuThrowEventInvocation) {
            this.invokeId2ThrowEventInvocationCorrelationMap.put(((CltuThrowEventInvocation) operation).getInvokeId().longValue(), (CltuThrowEventInvocation) operation);
        }
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status, String currentState, String nextState) {
        informSubscribers(externalId, status, Instant.now(), currentState, nextState);
    }

    private void informSubscribers(long externalId, ForwardDataUnitProcessingStatus status, Instant time, String currentState, String nextState) {
        subscribers.forEach(o -> o.informStatusUpdate(externalId, status, time, currentState, nextState));
    }

    private static class CltuTracker {
        private final long externalId;
        private final byte[] cltu;

        public CltuTracker(long externalId, byte[] cltu) {
            this.cltu = cltu;
            this.externalId = externalId;
        }

        public long getExternalId() {
            return externalId;
        }

        public byte[] getCltu() {
            return cltu;
        }
    }
}
