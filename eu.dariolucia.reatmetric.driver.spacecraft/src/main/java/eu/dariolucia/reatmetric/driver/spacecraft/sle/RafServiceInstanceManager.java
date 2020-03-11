/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.sle;

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleScheduleStatusReportReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.*;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.structures.AntennaId;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.structures.RafGetParameter;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.structures.RafGetParameterV1toV4;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.pdu.PduFactoryUtil;
import eu.dariolucia.ccsds.sle.utl.si.LockStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ProductionStatusEnum;
import eu.dariolucia.ccsds.sle.utl.si.ServiceInstanceBindingStateEnum;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafDiagnosticsStrings;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstance;
import eu.dariolucia.ccsds.tmtc.datalink.pdu.TmTransferFrame;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.TransferFrameType;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RafServiceInstanceManager extends SleServiceInstanceManager<RafServiceInstance, RafServiceInstanceConfiguration> {

    private static final Logger LOG = Logger.getLogger(RafServiceInstanceManager.class.getName());

    public RafServiceInstanceManager(PeerConfiguration peerConfiguration, RafServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        super(peerConfiguration, siConfiguration, spacecraftConfiguration, broker);
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
        } else if(operation instanceof RafGetParameterReturn) {
            process((RafGetParameterReturn) operation);
        } else if(operation instanceof RafGetParameterReturnV1toV4) {
            process((RafGetParameterReturnV1toV4) operation);
        } else {
            LOG.log(Level.WARNING, serviceInstance.getServiceInstanceIdentifier() + ": Discarding unsupported RAF operation: " + operation.getClass().getName());
        }
    }

    private void process(RafGetParameterReturnV1toV4 operation) {
        if(operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Negative RAF GET PARAMETER return: " + RafDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": RAF GET PARAMETER: " + toString(operation.getResult().getPositiveResult()));
        }
    }

    private String toString(RafGetParameterV1toV4 positiveResult) {
        if(positiveResult.getParBufferSize() != null) {
            return "buffer size=" + positiveResult.getParBufferSize().getParameterValue().intValue();
        } else if(positiveResult.getParDeliveryMode() != null) {
            return "delivery mode=" + positiveResult.getParDeliveryMode().getParameterValue().intValue(); // TODO string value for delivery mode
        } else if(positiveResult.getParLatencyLimit() != null) {
            if(positiveResult.getParLatencyLimit().getParameterValue().getOnline() != null) {
                return "latency limit=" + positiveResult.getParLatencyLimit().getParameterValue().getOnline().intValue();
            } else {
                return "latency limit=OFFLINE";
            }
        } else if(positiveResult.getParReportingCycle() != null) {
            if(positiveResult.getParReportingCycle().getParameterValue().getPeriodicReportingOn() != null) {
                return "reporting cycle=" + positiveResult.getParReportingCycle().getParameterValue().getPeriodicReportingOn().intValue();
            } else {
                return "reporting cycle=OFF";
            }
        } else if(positiveResult.getParReqFrameQuality() != null) {
            return "req. frame quality=" + positiveResult.getParReqFrameQuality().getParameterValue().intValue(); // TODO string value for requested frame quality
        } else if(positiveResult.getParReturnTimeout() != null) {
            return "return timeout=" + positiveResult.getParReturnTimeout().getParameterValue().intValue();
        } else {
            return "unknown";
        }
    }

    private void process(RafGetParameterReturn operation) {
        if(operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Negative RAF GET PARAMETER return: " + RafDiagnosticsStrings.getGetParameterDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": RAF GET PARAMETER: " + toString(operation.getResult().getPositiveResult()));
        }
    }

    private String toString(RafGetParameter positiveResult) {
        if(positiveResult.getParBufferSize() != null) {
            return "buffer size=" + positiveResult.getParBufferSize().getParameterValue().intValue();
        } else if(positiveResult.getParDeliveryMode() != null) {
            return "delivery mode=" + positiveResult.getParDeliveryMode().getParameterValue().intValue(); // TODO string value for delivery mode
        } else if(positiveResult.getParLatencyLimit() != null) {
            if(positiveResult.getParLatencyLimit().getParameterValue().getOnline() != null) {
                return "latency limit=" + positiveResult.getParLatencyLimit().getParameterValue().getOnline().intValue();
            } else {
                return "latency limit=OFFLINE";
            }
        } else if(positiveResult.getParReportingCycle() != null) {
            if(positiveResult.getParReportingCycle().getParameterValue().getPeriodicReportingOn() != null) {
                return "reporting cycle=" + positiveResult.getParReportingCycle().getParameterValue().getPeriodicReportingOn().intValue();
            } else {
                return "reporting cycle=OFF";
            }
        } else if(positiveResult.getParReqFrameQuality() != null) {
            return "req. frame quality=" + positiveResult.getParReqFrameQuality().getParameterValue().intValue(); // TODO string value for requested frame quality
        } else if(positiveResult.getParReturnTimeout() != null) {
            return "return timeout=" + positiveResult.getParReturnTimeout().getParameterValue().intValue();
        } else if(positiveResult.getParMinReportingCycle() != null) {
            return "min. reporting cycle=" + positiveResult.getParMinReportingCycle().getParameterValue().intValue();
        } else if(positiveResult.getParPermittedFrameQuality() != null) {
            return "perm. frame quality=" + positiveResult.getParPermittedFrameQuality().getParameterValue().getRequestedFrameQuality();
        } else {
            return "unknown";
        }
    }

    private void process(RafStatusReportInvocationV1toV2 operation) {
        LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Status report received: carrier=" + LockStatusEnum.fromCode(operation.getCarrierLockStatus().intValue()) +
                ", subcarrier=" + LockStatusEnum.fromCode(operation.getSubcarrierLockStatus().intValue()) +
                ", bitlock=" + LockStatusEnum.fromCode(operation.getSymbolSyncLockStatus().intValue()) +
                ", frame lock=" + LockStatusEnum.fromCode(operation.getFrameSyncLockStatus().intValue()));
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(RafStatusReportInvocation operation) {
        LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": Status report received: carrier=" + LockStatusEnum.fromCode(operation.getCarrierLockStatus().intValue()) +
                ", subcarrier=" + LockStatusEnum.fromCode(operation.getSubcarrierLockStatus().intValue()) +
                ", bitlock=" + LockStatusEnum.fromCode(operation.getSymbolSyncLockStatus().intValue()) +
                ", frame lock=" + LockStatusEnum.fromCode(operation.getFrameSyncLockStatus().intValue()));
        ProductionStatusEnum prodStatus = ProductionStatusEnum.fromCode(operation.getProductionStatus().intValue());
        updateProductionStatus(prodStatus);
    }

    private void process(SleScheduleStatusReportReturn operation) {
        if(operation.getResult().getNegativeResult() != null) {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Negative RAF SCHEDULE STATUS REPORT return: " + RafDiagnosticsStrings.getScheduleStatusReportDiagnostic(operation.getResult().getNegativeResult()));
        } else {
            LOG.info(serviceInstance.getServiceInstanceIdentifier() + ": RAF SCHEDULE STATUS REPORT positive return");
        }
    }

    private void process(RafTransferDataInvocation operation) {
        // Build frame to distribute
        if(spacecraftConfiguration.getTmDataLinkConfigurations().getType() == TransferFrameType.TM) {
            distributeTmFrame(operation);
        } else if(spacecraftConfiguration.getTmDataLinkConfigurations().getType() == TransferFrameType.AOS) {
            distributeAosFrame(operation);
        }
        // Ignore, it would fail at configuration time
    }

    private void distributeAosFrame(RafTransferDataInvocation operation) {
        // TODO
    }

    private void distributeTmFrame(RafTransferDataInvocation operation) {
        // Get the frame
        byte[] frameContents = operation.getData().value;
        Quality quality =  Quality.values()[operation.getDeliveredFrameQuality().intValue()];
        Instant genTimeInstant;
        if(operation.getEarthReceiveTime().getCcsdsFormat() != null) {
            long[] genTime = PduFactoryUtil.buildTimeMillis(operation.getEarthReceiveTime().getCcsdsFormat().value);
            genTimeInstant = Instant.ofEpochMilli(genTime[0]).plusNanos(genTime[1] * 1000);
        } else {
            long[] genTime = PduFactoryUtil.buildTimeMillisPico(operation.getEarthReceiveTime().getCcsdsPicoFormat().value);
            genTimeInstant = Instant.ofEpochMilli(genTime[0]).plusNanos(genTime[1] / 1000);
        }
        String antennaId = toString(operation.getAntennaId());
        // Check the quality: if GOOD, it is a frame, if BAD it is a CADU
        if(operation.getDeliveredFrameQuality().intValue() == 0) { // GOOD
            TmTransferFrame frame = new TmTransferFrame(frameContents, spacecraftConfiguration.getTmDataLinkConfigurations().isFecfPresent());
            RawData rd = new RawData(broker.nextRawDataId(), genTimeInstant, Constants.N_TM_TRANSFER_FRAME, Constants.T_TM_FRAME, antennaId, String.valueOf(frame.getSpacecraftId()), quality, null, frameContents, Instant.now(), null);
            rd.setData(frame);
            distribute(rd);
        } else {
            LOG.warning(serviceInstance.getServiceInstanceIdentifier() + ": Bad frame received");
            RawData rd = new RawData(broker.nextRawDataId(), genTimeInstant, Constants.N_TM_TRANSFER_FRAME, Constants.T_BAD_TM, antennaId, "", quality, null, frameContents, Instant.now(), null);
            distribute(rd);
        }
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
        serviceInstance.start(null, null, siConfiguration.getRequestedFrameQuality());
    }

    @Override
    protected void sendStop() {
        serviceInstance.stop();
    }
}
