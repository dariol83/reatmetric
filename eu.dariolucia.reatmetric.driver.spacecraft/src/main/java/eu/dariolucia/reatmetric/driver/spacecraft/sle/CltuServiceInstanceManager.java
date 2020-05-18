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

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuAsyncNotifyInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStartReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuStatusReportInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.cltu.outgoing.pdus.CltuTransferDataReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.ProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstance;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstanceBindingStateEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuDiagnosticsStrings;
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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CltuServiceInstanceManager extends SleServiceInstanceManager<CltuServiceInstance, CltuServiceInstanceConfiguration> {

    private static final Logger LOG = Logger.getLogger(CltuServiceInstanceManager.class.getName());
    private static final String FIRST_CLTU_ID_KEY = "cltu.first.id";

    private volatile CltuUplinkStatusEnum uplinkStatus = CltuUplinkStatusEnum.UPLINK_STATUS_NOT_AVAILABLE;

    private volatile int lastReportedFreeBuffer;
    private volatile int estimatedFreeBuffer;

    private final Map<Long, TcTracker> cltuId2tracker = new ConcurrentHashMap<>();
    private final AtomicLong cltuCounter = new AtomicLong(0);

    public CltuServiceInstanceManager(String driverName, PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IServiceCoreContext context) {
        super(driverName, peerConfiguration, siConfiguration, spacecraftConfiguration, context);
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
        if(operation instanceof CltuAsyncNotifyInvocation) {
            process((CltuAsyncNotifyInvocation) operation);
        } else if(operation instanceof CltuTransferDataReturn) {
            process((CltuTransferDataReturn) operation);
        } else if(operation instanceof SleScheduleStatusReportReturn) {
            process((SleScheduleStatusReportReturn) operation);
        } else if(operation instanceof CltuStatusReportInvocation) {
            process((CltuStatusReportInvocation) operation);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": Discarding unhandled CLTU operation: " + operation.getClass().getName());
        }
    }

    private void process(CltuStatusReportInvocation operation) {
        LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Status report received: CLTU free buffer=" + operation.getCltuBufferAvailable().intValue() +
                ", CLTU processed=" + operation.getNumberOfCltusProcessed() +
                ", CLTU radiated=" + operation.getNumberOfCltusRadiated() +
                ", CLTU received=" + operation.getNumberOfCltusReceived() +
                ", uplink status=" + CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()]);
        this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getCltuProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(SleScheduleStatusReportReturn operation) {
        if(operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Negative CLTU SCHEDULE STATUS REPORT return: " + CltuDiagnosticsStrings.getScheduleStatusReportDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": CLTU SCHEDULE STATUS REPORT positive return");
        }
    }

    private void process(CltuTransferDataReturn operation) {
        long cltuId = operation.getCltuIdentification().longValue();
        TcTracker tracker = this.cltuId2tracker.get(cltuId);
        if(tracker == null) {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": CLTU ID " + cltuId + " not send by this system");
            return;
        }
        this.lastReportedFreeBuffer = operation.getCltuBufferAvailable().intValue();
        if(operation.getResult().getPositiveResult() != null) {
            reportActivityState(tracker, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
            reportActivityState(tracker, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_UPLINK, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        } else {
            reportActivityState(tracker, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.FAIL, ActivityOccurrenceState.TRANSMISSION);
            this.cltuId2tracker.remove(cltuId);
        }
    }

    private void process(CltuAsyncNotifyInvocation operation) {
        if(operation.getUplinkStatus() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": uplink status changed: " + CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()]);
            this.uplinkStatus = CltuUplinkStatusEnum.values()[operation.getUplinkStatus().intValue()];
            updateMessage("Uplink status: " + this.uplinkStatus);
        } else if(operation.getProductionStatus() != null) {
            ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
            updateProductionStatus(prodStatus);
            updateMessage("Production status: " + prodStatus.name());
            // TODO other cases
            // TODO report radiation
        } else if(operation.getCltuNotification().getBufferEmpty() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": CLTU buffer empty");
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Unknown CLTU ASYNC NOTIFY received");
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

    @Override
    protected void finalizeConnection() {
        if(serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE || serviceInstance.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY) {
            if(siConfiguration.getReportingCycle() != 0) {
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

    public void sendCltu(byte[] encodedCltu, TcTracker tracker) {
        if(this.serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE) {
            LOG.severe(serviceInstance.getServiceInstanceIdentifier() + ": transmission of activity " + tracker.getInvocation().getActivityId() + ", " + tracker.getInvocation().getActivityOccurrenceId() + " failed: service instance not active");
            reportActivityState(tracker, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FAIL, ActivityOccurrenceState.RELEASE);
            reportTcFailed(tracker);
            return;
        }
        long thisCounter = this.cltuCounter.getAndIncrement();
        this.cltuId2tracker.put(thisCounter, tracker);
        reportActivityState(tracker, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK, ActivityOccurrenceState.TRANSMISSION);
        reportTcReleased(tracker);
        reportActivityState(tracker, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_GROUND_STATION_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
        this.serviceInstance.transferData(thisCounter, null, null, 20000000, true, encodedCltu);
    }

    private void reportTcReleased(TcTracker tracker) {
        // TODO service broker
    }

    private void reportTcFailed(TcTracker tracker) {
        // TODO service broker
    }

    private void reportActivityState(TcTracker tracker, ActivityOccurrenceState state, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(tracker.getInvocation().getActivityId(), tracker.getInvocation().getActivityOccurrenceId(), name, Instant.now(), state, null, status, nextState, null));
    }

    @Override
    public void onPduSentError(ServiceInstance si, Object operation, String name, byte[] encodedOperation, String error, Exception exception) {
        super.onPduSentError(si, operation, name, encodedOperation, error, exception);
        // TODO: handle error on CLTU transfer data transmission
    }
}
