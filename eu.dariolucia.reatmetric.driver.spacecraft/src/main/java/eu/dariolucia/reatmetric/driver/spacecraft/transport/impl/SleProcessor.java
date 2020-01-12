/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.transport.impl;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.TcTransferFrame;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.driver.spacecraft.message.IMessageProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.transport.ITransportProcessor;
import eu.dariolucia.reatmetric.driver.spacecraft.transport.ITransportProcessorListener;
import eu.dariolucia.reatmetric.driver.spacecraft.util.SleepUtil;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.common.types.Time;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafSyncNotifyInvocation;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.raf.structures.LockStatusReport;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.raf.structures.RafProductionStatus;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfSyncNotifyInvocation;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.rcf.outgoing.pdus.RcfTransferDataInvocation;
import eu.dariolucia.sle.generated.ccsds.sle.transfer.service.rcf.structures.RcfProductionStatus;
import eu.dariolucia.sle.testtool.impl.config.api.SleApiConfiguration;
import eu.dariolucia.sle.testtool.impl.config.si.ServiceInstanceConfiguration;
import eu.dariolucia.sle.testtool.impl.config.si.cltu.CltuServiceInstanceConfiguration;
import eu.dariolucia.sle.testtool.impl.config.si.loader.SiConfigLoader;
import eu.dariolucia.sle.testtool.impl.config.si.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.sle.testtool.impl.config.si.rcf.RcfServiceInstanceConfiguration;
import eu.dariolucia.sle.testtool.impl.pdu.PduFactoryUtil;
import eu.dariolucia.sle.testtool.impl.si.*;
import eu.dariolucia.sle.testtool.impl.si.cltu.CltuServiceInstance;
import eu.dariolucia.sle.testtool.impl.si.raf.RafRequestedFrameQualityEnum;
import eu.dariolucia.sle.testtool.impl.si.raf.RafServiceInstance;
import eu.dariolucia.sle.testtool.impl.si.rcf.RcfServiceInstance;

import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SleProcessor implements ITransportProcessor {

    public static final String SLE_CONFIGURATION_KEY = "sle.configuration";
    public static final String SLE_SICF_FOLDER_KEY = "sle.sicf.folder";
    public static final String SLE_VERSION_KEY = "sle.version";

    public static final String SOURCE_ID = "SLE Processor";

    private final List<ServiceInstance> returnServiceInstances = new CopyOnWriteArrayList<>();

    private volatile CltuServiceInstance forwardServiceInstance = null;

    private final int sleVersion;

    private final SleApiConfiguration configuration;

    private final List<ServiceInstanceConfiguration> serviceInstanceConfigurationList;

    private volatile IMessageProcessor logger;

    private volatile IServiceInstanceListener operationCollector;

    private volatile boolean running = false;

    public SleProcessor() {
        this.sleVersion = readVersion();
        this.configuration = readConfiguration();
        this.serviceInstanceConfigurationList = readServiceInstances();
    }

    private List<ServiceInstanceConfiguration> readServiceInstances() {
        List<ServiceInstanceConfiguration> conf = new LinkedList<>();
        File directory = new File(System.getProperty(SLE_SICF_FOLDER_KEY));
        for(File f : directory.listFiles()) {
            try {
                conf.addAll(SiConfigLoader.load(new FileInputStream(f)));
            } catch (Exception e) {
                throw new RuntimeException("Error in service instance loading of file " + f.getAbsolutePath(), e);
            }
        }
        return conf;
    }

    private SleApiConfiguration readConfiguration() {
        try {
            return SleApiConfiguration.load(new FileInputStream(System.getProperty(SLE_CONFIGURATION_KEY)));
        } catch (Exception e) {
            throw new RuntimeException("Error in configuration of the SLE processor", e);
        }
    }

    private int readVersion() {
        return Integer.parseInt(System.getProperty(SLE_VERSION_KEY, "2"));
    }

    public void setLogger(IMessageProcessor logger) {
        this.logger = logger;
    }

    private IServiceInstanceListener buildOperationCollector(final ITransportProcessorListener tmFrameSink) {
        return new IServiceInstanceListener() {

            @Override
            public void onStateUpdated(ServiceInstance serviceInstance, ServiceInstanceState serviceInstanceState) {
                logger.raiseMessage("SLE Service Instance " + serviceInstance.getServiceInstanceIdentifier() + " is " + serviceInstanceState, SOURCE_ID, Severity.INFO);
            }

            @Override
            public void onPduReceived(ServiceInstance serviceInstance, Object o, String s, byte[] bytes) {
                processReceivedPdu(serviceInstance, o, tmFrameSink);
            }

            @Override
            public void onPduSent(ServiceInstance serviceInstance, Object o, String s, byte[] bytes) {
                // Ignore
            }

            @Override
            public void onPduSentError(ServiceInstance serviceInstance, Object o, String s, byte[] bytes, String s1, Exception e) {
                logger.raiseMessage("Error while sending PDU " + s + " by SLE Service Instance " + serviceInstance.getServiceInstanceIdentifier() + ": " + s1, SOURCE_ID, Severity.ALARM);
            }

            @Override
            public void onPduDecodingError(ServiceInstance serviceInstance, byte[] bytes) {
                logger.raiseMessage("Error while decoding PDU by SLE Service Instance " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.ALARM);
            }

            @Override
            public void onPduHandlingError(ServiceInstance serviceInstance, Object o, byte[] bytes) {
                logger.raiseMessage("Error while handling PDU by SLE Service Instance " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.ALARM);
            }
        };
    }

    private void processReceivedPdu(ServiceInstance serviceInstance, Object pdu, ITransportProcessorListener sink) {
        if(serviceInstance instanceof RafServiceInstance) {
            if(pdu instanceof RafTransferDataInvocation) {
                RafTransferDataInvocation frame = (RafTransferDataInvocation) pdu;
                if(((RafTransferDataInvocation) pdu).getDeliveredFrameQuality().intValue() == 0) {
                    byte[] theFrame = frame.getData().value;
                    Instant ert = toInstant(frame.getEarthReceiveTime());
                    byte[] privateAnnotations = frame.getPrivateAnnotation().getNotNull() != null ? frame.getPrivateAnnotation().getNotNull().value : null;
                    sink.goodTmFrameReceived(theFrame, ert, privateAnnotations);
                } else {
                    logger.raiseMessage("Bad or undetermined frame quality received: " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                    sink.badTmFrameReceived(frame.getData().value);
                }
            } else if(pdu instanceof RafSyncNotifyInvocation) {
                RafSyncNotifyInvocation notify = (RafSyncNotifyInvocation) pdu;
                if(notify.getNotification().getEndOfData() != null) {
                    logger.raiseMessage("RAF end-of-data notified: " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getExcessiveDataBacklog() != null) {
                    logger.raiseMessage("RAF excessive-backlog notified: " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getLossFrameSync() != null) {
                    logger.raiseMessage("RAF loss-frame-sync notified (" + toString(notify.getNotification().getLossFrameSync()) + ": " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getProductionStatusChange() != null) {
                    logger.raiseMessage("RAF production-status notified (" + toString(notify.getNotification().getProductionStatusChange()) + ": " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                }
            }
            // Ignore the rest
        } else if(serviceInstance instanceof RcfServiceInstance) {
            if(pdu instanceof RcfTransferDataInvocation) {
                RcfTransferDataInvocation frame = (RcfTransferDataInvocation) pdu;
                byte[] theFrame = frame.getData().value;
                Instant ert = toInstant(frame.getEarthReceiveTime());
                byte[] privateAnnotations = frame.getPrivateAnnotation().getNotNull() != null ? frame.getPrivateAnnotation().getNotNull().value : null;
                sink.goodTmFrameReceived(theFrame, ert, privateAnnotations);
            } else if(pdu instanceof RcfSyncNotifyInvocation) {
                RcfSyncNotifyInvocation notify = (RcfSyncNotifyInvocation) pdu;
                if(notify.getNotification().getEndOfData() != null) {
                    logger.raiseMessage("RCF end-of-data notified: " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getExcessiveDataBacklog() != null) {
                    logger.raiseMessage("RCF excessive-backlog notified: " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getLossFrameSync() != null) {
                    logger.raiseMessage("RCF loss-frame-sync notified (" + toString(notify.getNotification().getLossFrameSync()) + "): " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                } else if(notify.getNotification().getProductionStatusChange() != null) {
                    logger.raiseMessage("RCF production-status notified (" + toString(notify.getNotification().getProductionStatusChange()) + "): " + serviceInstance.getServiceInstanceIdentifier(), SOURCE_ID, Severity.WARN);
                }
            }
            // Ignore the rest
        } else if(serviceInstance instanceof CltuServiceInstance) {
            // TODO: correlate responses to requests (CLTU TRANSFER DATA, ASYNC NOTIFY of radiation) for proper command ack
        }
    }

    private String toString(eu.dariolucia.sle.generated.ccsds.sle.transfer.service.rcf.structures.LockStatusReport lossFrameSync) {
        return "TODO"; // TODO
    }

    private String toString(RcfProductionStatus productionStatusChange) {
        switch(productionStatusChange.value.intValue()) {
            case 0: return "running";
            case 1: return "interrupted";
            case 2: return "halted";
            default: return "unknown";
        }
    }

    private String toString(RafProductionStatus productionStatusChange) {
        switch(productionStatusChange.value.intValue()) {
            case 0: return "running";
            case 1: return "interrupted";
            case 2: return "halted";
            default: return "unknown";
        }
    }

    private String toString(LockStatusReport lossFrameSync) {
        return "TODO"; // TODO
    }

    private Instant toInstant(Time earthReceiveTime) {
        if(earthReceiveTime.getCcsdsFormat() != null) {
            long[] res = PduFactoryUtil.buildTimeMillis(earthReceiveTime.getCcsdsFormat().value);
            return Instant.ofEpochMilli(res[0]).plusNanos(res[1]);
        } else if(earthReceiveTime.getCcsdsPicoFormat() != null) {
            long[] res = PduFactoryUtil.buildTimeMillisPico(earthReceiveTime.getCcsdsFormat().value);
            return Instant.ofEpochMilli(res[0]).plusNanos(res[1]);
        } else {
            return Instant.now();
        }
    }

    @Override
    public void openLinks() {
        if(logger == null) {
            throw new IllegalStateException("Deployment error, logger not set");
        }
        if(operationCollector == null) {
            throw new IllegalStateException("Deployment error, listener not set");
        }
        if (running) {
            logger.raiseMessage("Cannot open SLE links: already open", SOURCE_ID, Severity.WARN);
            return;
        }
        running = true;
        createServiceInstances();
        connectServiceInstances();
    }

    private void connectServiceInstances() {
        // Bind what you can
        for (ServiceInstance si : returnServiceInstances) {
            bindAndStart(si);
        }
        if (forwardServiceInstance != null) {
            bindAndStart(forwardServiceInstance);
        }
    }

    private void bindAndStart(ServiceInstance si) {
        try {
            si.bind(sleVersion);
            SleepUtil.conditionalSleep(5000, 1000, () -> si.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY);
            if(si.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY) {
                if (si instanceof RafServiceInstance) {
                    ((RafServiceInstance) si).start(null, null, RafRequestedFrameQualityEnum.ALL_FRAMES);
                } else if (si instanceof RcfServiceInstance) {
                    RcfServiceInstanceConfiguration rcfCfg = (RcfServiceInstanceConfiguration) si.getServiceInstanceConfiguration();
                    ((RcfServiceInstance) si).start(null, null, rcfCfg.getPermittedGvcid().get(0));
                } else if(si instanceof CltuServiceInstance) {
                    ((CltuServiceInstance) si).start(0L);
                }
                // No schedule status report requested
            } else {
                logger.raiseMessage("Start-up of SLE Service Instance " + si.getServiceInstanceIdentifier() + "failed: BIND failed or timed out", SOURCE_ID, Severity.ALARM);
            }
        } catch (Exception e) {
            logger.raiseMessage("Start-up of SLE Service Instance " + si.getServiceInstanceIdentifier() + "failed: " + e.getMessage(), SOURCE_ID, Severity.ALARM);
        }
    }

    private void createServiceInstances() {
        // Iterate over the configured services
        for (ServiceInstanceConfiguration sic : serviceInstanceConfigurationList) {
            try {
                if (sic instanceof RafServiceInstanceConfiguration) {
                    RafServiceInstance rafSi = new RafServiceInstance(this.configuration, (RafServiceInstanceConfiguration) sic);
                    returnServiceInstances.add(rafSi);
                    rafSi.register(operationCollector);
                } else if (sic instanceof RcfServiceInstanceConfiguration) {
                    RcfServiceInstance rcfSi = new RcfServiceInstance(this.configuration, (RcfServiceInstanceConfiguration) sic);
                    returnServiceInstances.add(rcfSi);
                    rcfSi.register(operationCollector);
                } else if (sic instanceof CltuServiceInstanceConfiguration) {
                    if (forwardServiceInstance == null) {
                        forwardServiceInstance = new CltuServiceInstance(this.configuration, (CltuServiceInstanceConfiguration) sic);
                        forwardServiceInstance.register(operationCollector);
                    } else {
                        logger.raiseMessage("Loading of CLTU Service Instance " + sic.getServiceInstanceIdentifier() + " skipped due to already loaded CLTU Service Instance", SOURCE_ID, Severity.WARN);
                    }
                }
            } catch (Exception e) {
                logger.raiseMessage("Loading of SLE Service Instance " + sic.getServiceInstanceIdentifier() + " skipped due to type not supported (" + sic.getType() + ")", SOURCE_ID, Severity.WARN);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void closeLinks() {
        if (!running) {
            logger.raiseMessage("Cannot close SLE links: already closed", SOURCE_ID, Severity.WARN);
            return;
        }
        running = false;
        disconnectServiceInstances();
        cleanupServiceInstances();
    }

    @Override
    public void sendTcFrame(long tcId, TcTransferFrame frame) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void setListener(ITransportProcessorListener listener) {
        if(this.operationCollector != null) {
            throw new IllegalStateException("SLE Processor already registered with a listener");
        }
        this.operationCollector = buildOperationCollector(listener);
    }

    private void cleanupServiceInstances() {
        this.forwardServiceInstance = null;
        this.returnServiceInstances.clear();
    }

    private void disconnectServiceInstances() {
        // Stop/Unbind what you can
        for (ServiceInstance si : returnServiceInstances) {
            stopAndUnbind(si);
        }
        if (forwardServiceInstance != null) {
            stopAndUnbind(forwardServiceInstance);
        }
    }

    private void stopAndUnbind(ServiceInstance si) {
        try {
            if(si.getCurrentBindingState() == ServiceInstanceBindingStateEnum.ACTIVE) {
                if (si instanceof RafServiceInstance) {
                    ((RafServiceInstance) si).stop();
                } else if (si instanceof RcfServiceInstance) {
                    ((RcfServiceInstance) si).stop();
                } else if(si instanceof CltuServiceInstance) {
                    ((CltuServiceInstance) si).stop();
                }
            }
            SleepUtil.conditionalSleep(5000, 1000, () -> si.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY);
            if(si.getCurrentBindingState() == ServiceInstanceBindingStateEnum.READY) {
                si.unbind(UnbindReasonEnum.SUSPEND);
            } else {
                logger.raiseMessage("Unbind of SLE Service Instance " + si.getServiceInstanceIdentifier() + "failed: STOP failed or timed out", SOURCE_ID, Severity.WARN);
                // Go for peer abort
                si.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
            }
        } catch (Exception e) {
            logger.raiseMessage("Shutdown of SLE Service Instance " + si.getServiceInstanceIdentifier() + "failed: " + e.getMessage(), SOURCE_ID, Severity.ALARM);
        }
    }
}
