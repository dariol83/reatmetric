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
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.TcPacketPhase;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CLTU can contain more than one packet, and also a CLTU can contain a part of a packet. This is the reason why this class
 * handles a list of {@link TcTracker} objects per CLTU invocation.
 * <p>
 * This class has also a subscription mechanism, which allows subscribers to know the status of a CLTU (ACCEPTED, REJECTED, UPLINKED, FAILED, DISCARDED).
 */
public class CltuServiceInstanceManager extends SleServiceInstanceManager<CltuServiceInstance, CltuServiceInstanceConfiguration> {

    private static final Logger LOG = Logger.getLogger(CltuServiceInstanceManager.class.getName());
    private static final String FIRST_CLTU_ID_KEY = "cltu.first.id";

    private volatile CltuUplinkStatusEnum uplinkStatus = CltuUplinkStatusEnum.UPLINK_STATUS_NOT_AVAILABLE;

    private volatile int bufferCapacity = -1;
    private final AtomicInteger estimatedFreeBuffer = new AtomicInteger();

    private final Map<Long, CltuTracker> cltuId2tracker = new ConcurrentHashMap<>();
    private final AtomicLong cltuCounter = new AtomicLong(0);
    private final IServiceBroker serviceBroker;

    private final Semaphore cltuIdRefreshSemaphore = new Semaphore(0);

    private final List<BiConsumer<Long, CltuProcessingStatus>> subscribers = new CopyOnWriteArrayList<>();

    public CltuServiceInstanceManager(String driverName, PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IServiceCoreContext context, IServiceBroker serviceBroker) {
        super(driverName, peerConfiguration, siConfiguration, spacecraftConfiguration, context);
        this.serviceBroker = serviceBroker;
    }

    public void register(BiConsumer<Long, CltuProcessingStatus> listener) {
        this.subscribers.add(listener);
    }

    public void deregister(BiConsumer<Long, CltuProcessingStatus> listener) {
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
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": get parameter returned negative result: " + CltuDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        }
    }

    private void process(CltuStatusReportInvocation operation) {
        LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": status report received: CLTU free buffer=" + operation.getCltuBufferAvailable().intValue() +
                ", CLTU processed=" + operation.getNumberOfCltusProcessed() +
                ", CLTU radiated=" + operation.getNumberOfCltusRadiated() +
                ", CLTU received=" + operation.getNumberOfCltusReceived() +
                ", uplink status=" + CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()]);
        // The first report after the bind should report the effective buffer capacity
        if (this.bufferCapacity == 0) {
            this.bufferCapacity = operation.getCltuBufferAvailable().intValue();
            this.estimatedFreeBuffer.set(operation.getCltuBufferAvailable().intValue());
        }
        this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getCltuProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(SleScheduleStatusReportReturn operation) {
        if (operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": negative CLTU SCHEDULE STATUS REPORT return: " + CltuDiagnosticsStrings.getScheduleStatusReportDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": CLTU SCHEDULE STATUS REPORT positive return");
        }
    }

    private void process(CltuTransferDataReturn operation) {
        long cltuId = operation.getCltuIdentification().longValue();
        CltuTracker tracker = this.cltuId2tracker.get(cltuId);
        if (tracker == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": CLTU ID " + cltuId + " not send by this system");
            return;
        }
        if (operation.getResult().getPositiveResult() != null) {
            subscribers.forEach(o -> o.accept(tracker.getExternalId(), CltuProcessingStatus.ACCEPTED));
            reportActivityState(tracker.getTcTracker(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
            reportActivityState(tracker.getTcTracker(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        } else {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": negative CLTU TRANSFER DATA return: " + CltuDiagnosticsStrings.getTransferDataDiagnostic(operation.getResult().getNegativeResult()));
            subscribers.forEach(o -> o.accept(tracker.getExternalId(), CltuProcessingStatus.REJECTED));
            reportActivityState(tracker.getTcTracker(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FAIL, ActivityOccurrenceState.TRANSMISSION);
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
        if (operation.getUplinkStatus() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": uplink status changed: " + CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()]);
            this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
            updateMessage("Uplink status: " + this.uplinkStatus);
        } else if (operation.getProductionStatus() != null) {
            ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
            updateProductionStatus(prodStatus);
            updateMessage("Production status: " + prodStatus.name());
        } else if (operation.getCltuNotification().getCltuRadiated() != null && operation.getCltuLastProcessed().getCltuProcessed() != null) {
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
            // TODO introduce THROW-EVENT activity type
        } else if (operation.getCltuNotification().getActionListNotCompleted() != null) {
            // TODO introduce THROW-EVENT activity type
        } else if (operation.getCltuNotification().getEventConditionEvFalse() != null) {
            // TODO introduce THROW-EVENT activity type
        } else if (operation.getCltuNotification().getBufferEmpty() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": CLTU buffer empty");
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Unknown CLTU ASYNC NOTIFY received");
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
            subscribers.forEach(o -> o.accept(tracker.getExternalId(), CltuProcessingStatus.UPLINKED));
            increaseEstimatedFreeBuffer(tracker.getCltu().length);
            informServiceBroker(TcPacketPhase.UPLINKED, radiationTime, tracker.getTcTracker());
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
            subscribers.forEach(o -> o.accept(tracker.getExternalId(), CltuProcessingStatus.FAILED));
            increaseEstimatedFreeBuffer(tracker.getCltu().length);
            informServiceBroker(TcPacketPhase.FAILED, Instant.now(), tracker.getTcTracker());
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": received radiation problem for CLTU " + cltuId + " not present in the system");
        }
    }

    private void updateProductionStatus(ProductionStatusEnum prodStatus) {
        if (prodStatus == ProductionStatusEnum.RUNNING) {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Production status " + prodStatus);
            updateAlarmState(AlarmState.NOMINAL);
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Production status " + prodStatus);
            updateAlarmState(AlarmState.ALARM);
        }
    }

    private void informServiceBroker(TcPacketPhase phase, Instant time, List<TcTracker> trackers) {
        for (TcTracker tracker : trackers) {
            serviceBroker.informTcPacket(phase, time, tracker);
        }
    }

    @Override
    protected void finalizeConnection() {
        if (serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE || serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY) {
            // Reset the buffer capacity
            this.bufferCapacity = 0;
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

    public void sendCltu(byte[] encodedCltu, long externalId, List<TcTracker> trackers) {
        if (this.serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of CLTU with external ID " + externalId + " failed: service instance state is " + this.serviceInstance.getCurrentBindingState());
            reportTcFailed(trackers);
            reportActivityState(trackers, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FAIL, ActivityOccurrenceState.RELEASE);
            return;
        }
        boolean goAhead = decreaseEstimatedFreeBuffer(encodedCltu.length);
        if (!goAhead) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of CLTU with external ID " + externalId + " failed: remote CLTU buffer availability failed");
            reportTcFailed(trackers);
            reportActivityState(trackers, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FAIL, ActivityOccurrenceState.RELEASE);
            return;
        }
        long thisCounter = this.cltuCounter.getAndIncrement();
        this.cltuId2tracker.put(thisCounter, new CltuTracker(externalId, trackers, encodedCltu));
        reportTcReleased(trackers);
        reportActivityState(trackers, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
        reportActivityState(trackers, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        this.serviceInstance.transferData(thisCounter, null, null, 20000000, true, encodedCltu);
    }

    private void reportTcReleased(List<TcTracker> trackers) {
        informServiceBroker(TcPacketPhase.FAILED, Instant.now(), trackers);
    }

    private void reportTcFailed(List<TcTracker> trackers) {
        informServiceBroker(TcPacketPhase.FAILED, Instant.now(), trackers);
    }

    private void reportActivityState(List<TcTracker> trackers, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        Instant t = Instant.now();
        for (TcTracker tracker : trackers) {
            context.getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, t, state, null, status, nextState, null));
        }
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
                reportTcFailed(tracker.getTcTracker());
                reportActivityState(tracker.getTcTracker(), ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FAIL, ActivityOccurrenceState.TRANSMISSION);
            }
        }
    }

    private static class CltuTracker {
        private final long externalId;
        private final List<TcTracker> tcTracker;
        private final byte[] cltu;

        public CltuTracker(long externalId, List<TcTracker> tcTracker, byte[] cltu) {
            this.tcTracker = List.copyOf(tcTracker);
            this.cltu = cltu;
            this.externalId = externalId;
        }

        public long getExternalId() {
            return externalId;
        }

        public List<TcTracker> getTcTracker() {
            return tcTracker;
        }

        public byte[] getCltu() {
            return cltu;
        }
    }

    public enum CltuProcessingStatus {
        ACCEPTED, REJECTED, UPLINKED, FAILED
    }
}
