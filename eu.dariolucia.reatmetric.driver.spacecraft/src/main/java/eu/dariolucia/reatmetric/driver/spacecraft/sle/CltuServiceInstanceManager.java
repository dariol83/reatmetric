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
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.*;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.structures.AntennaId;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.LockStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstanceBindingStateEnum;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuDiagnosticsStrings;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuServiceInstance;
import eu.dariolucia.ccsds.sle.utl.si.cltu.CltuUplinkStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafDiagnosticsStrings;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstance;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CltuServiceInstanceManager extends SleServiceInstanceManager<CltuServiceInstance, CltuServiceInstanceConfiguration> {

    private static final Logger LOG = Logger.getLogger(CltuServiceInstanceManager.class.getName());

    private volatile CltuUplinkStatusEnum uplinkStatus = CltuUplinkStatusEnum.UPLINK_STATUS_NOT_AVAILABLE;

    private volatile int lastReportedFreeBuffer;
    private volatile int estimatedFreeBuffer;

    public CltuServiceInstanceManager(String driverName, PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        super(driverName, peerConfiguration, siConfiguration, spacecraftConfiguration, broker);
    }

    @Override
    protected CltuServiceInstance createServiceInstance(PeerConfiguration peerConfiguration, CltuServiceInstanceConfiguration siConfiguration) {
        return new CltuServiceInstance(peerConfiguration, siConfiguration);
    }

    // TODO initialisation map with required properties (e.g. cltuId)

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
        // TODO
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
        // TODO: from the initialisation properties
        serviceInstance.start(0);
    }

    @Override
    protected void sendStop() {
        serviceInstance.stop();
    }
}
