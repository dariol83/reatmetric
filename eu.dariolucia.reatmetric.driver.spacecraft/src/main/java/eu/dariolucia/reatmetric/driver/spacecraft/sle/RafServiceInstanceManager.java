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

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.*;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.structures.AntennaId;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.LockStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstanceBindingStateEnum;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafDiagnosticsStrings;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstance;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RafServiceInstanceManager extends SleServiceInstanceManager<RafServiceInstance, RafServiceInstanceConfiguration> {

    private static final Logger LOG = Logger.getLogger(RafServiceInstanceManager.class.getName());

    public RafServiceInstanceManager(String driverName, PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IServiceCoreContext context) {
        super(driverName, peerConfiguration, siConfiguration, spacecraftConfiguration, context);
    }

    @Override
    protected RafServiceInstance createServiceInstance(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration siConfiguration) {
        return new RafServiceInstance(peerConfiguration, siConfiguration);
    }

    @Override
    protected boolean isStartReturn(Object operation) {
        return operation instanceof RafStartReturn;
    }

    @Override
    protected void handleOperation(Object operation) {
        if(operation instanceof RafSyncNotifyInvocation) {
            process((RafSyncNotifyInvocation) operation);
        } else if(operation instanceof RafTransferDataInvocation) {
            process((RafTransferDataInvocation) operation);
        } else if(operation instanceof SleScheduleStatusReportReturn) {
            process((SleScheduleStatusReportReturn) operation);
        } else if(operation instanceof RafStatusReportInvocation) {
            process((RafStatusReportInvocation) operation);
        } else if(operation instanceof RafStatusReportInvocationV1toV2) {
            process((RafStatusReportInvocationV1toV2) operation);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": Discarding unhandled RAF operation: " + operation.getClass().getName());
        }
    }

    private void process(RafStatusReportInvocationV1toV2 operation) {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Status report received: carrier=" + LockStatusEnum.fromCode(operation.getCarrierLockStatus().intValue()) +
                    ", subcarrier=" + LockStatusEnum.fromCode(operation.getSubcarrierLockStatus().intValue()) +
                    ", bitlock=" + LockStatusEnum.fromCode(operation.getSymbolSyncLockStatus().intValue()) +
                    ", frame lock=" + LockStatusEnum.fromCode(operation.getFrameSyncLockStatus().intValue()));
        }
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(RafStatusReportInvocation operation) {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Status report received: carrier=" + LockStatusEnum.fromCode(operation.getCarrierLockStatus().intValue()) +
                    ", subcarrier=" + LockStatusEnum.fromCode(operation.getSubcarrierLockStatus().intValue()) +
                    ", bitlock=" + LockStatusEnum.fromCode(operation.getSymbolSyncLockStatus().intValue()) +
                    ", frame lock=" + LockStatusEnum.fromCode(operation.getFrameSyncLockStatus().intValue()));
        }
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(SleScheduleStatusReportReturn operation) {
        if(operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Negative RAF SCHEDULE STATUS REPORT return: " + RafDiagnosticsStrings.getScheduleStatusReportDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer(serviceInstance.getServiceInstanceIdentifier() + ": RAF SCHEDULE STATUS REPORT positive return");
            }
        }
    }

    private void process(RafTransferDataInvocation operation) {
        // Get the frame
        byte[] frameContents = operation.getData().value;
        Quality quality =  Quality.values()[operation.getDeliveredFrameQuality().intValue()];
        Instant receptionTime = parseTime(operation.getEarthReceiveTime());
        Instant genTimeInstant = receptionTime.minusNanos(spacecraftConfiguration.getPropagationDelay() * 1000);
        String antennaId = toString(operation.getAntennaId());
        // Build frame to distribute
        if(spacecraftConfiguration.getTmDataLinkConfigurations().getType() == TransferFrameType.TM) {
            distributeTmFrame(frameContents, quality, genTimeInstant, receptionTime, antennaId);
        } else if(spacecraftConfiguration.getTmDataLinkConfigurations().getType() == TransferFrameType.AOS) {
            distributeAosFrame(frameContents, quality, genTimeInstant, receptionTime, antennaId);
        }
        // Ignore, it would fail at configuration time
    }

    private String toString(AntennaId antennaId) {
        if(antennaId.getGlobalForm() != null) {
            return Arrays.toString(antennaId.getGlobalForm().value);
        } else {
            return new String(antennaId.getLocalForm().value);
        }
    }

    private void process(RafSyncNotifyInvocation operation) {
        if(operation.getNotification().getEndOfData() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": End of data received");
        } else if(operation.getNotification().getExcessiveDataBacklog() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Data discarded due to excessive backlog");
        } else if(operation.getNotification().getLossFrameSync() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Frame synchronisation lost");
        } else if(operation.getNotification().getProductionStatusChange() != null) {
            ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getNotification().getProductionStatusChange().intValue());
            updateProductionStatus(prodStatus);
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Unknown RAF SYNC NOTIFY received");
        }
    }

    private void updateProductionStatus(ProductionStatusEnum prodStatus) {
        if (prodStatus == ProductionStatusEnum.RUNNING) {
            if(LOG.isLoggable(Level.FINER)) {
                LOG.finer(serviceInstance.getServiceInstanceIdentifier() + ": Production status " + prodStatus);
            }
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
        serviceInstance.start(null, null, siConfiguration.getRequestedFrameQuality());
    }

    @Override
    protected void sendStop() {
        serviceInstance.stop();
    }
}
