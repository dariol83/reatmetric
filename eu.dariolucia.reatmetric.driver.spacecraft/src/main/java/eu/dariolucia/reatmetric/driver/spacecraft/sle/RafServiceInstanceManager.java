/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.sle;

import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.bind.types.SleBindReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.bind.types.SleUnbindReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleAcknowledgement;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.common.pdus.SleStopInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafStartReturn;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafSyncNotifyInvocation;
import eu.dariolucia.ccsds.sle.generated.ccsds.sle.transfer.service.raf.outgoing.pdus.RafTransferDataInvocation;
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.raf.RafServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.ccsds.sle.utl.si.raf.RafServiceInstance;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// TODO: once finished, move as much as you can in the parent class, use templates to simply the code
public class RafServiceInstanceManager extends SleServiceInstanceManager implements IServiceInstanceListener {

    private final RafServiceInstance serviceInstance;
    private final Map<String, Object> initialisationMap = new HashMap<>();
    private final Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap = new HashMap<>();

    private final Semaphore bindSemaphore = new Semaphore(0);
    private final Semaphore unbindSemaphore = new Semaphore(0);
    private final Semaphore startSemaphore = new Semaphore(0);
    private final Semaphore stopSemaphore = new Semaphore(0);

    public RafServiceInstanceManager(PeerConfiguration peerConfiguration, ServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        super(peerConfiguration, siConfiguration, spacecraftConfiguration, broker);
        this.serviceInstance = new RafServiceInstance(peerConfiguration, (RafServiceInstanceConfiguration) siConfiguration);
        this.serviceInstance.register(this);
        buildInitialisationMap();
    }

    private void buildInitialisationMap() {
        this.initialisationMap.put(SLE_VERSION_KEY, siConfiguration.getServiceVersionNumber());
        this.initialisationDescriptionMap.put(SLE_VERSION_KEY, Pair.of("SLE Version", ValueTypeEnum.UNSIGNED_INTEGER));
    }

    @Override
    protected void computeBitrate() {
        if(this.serviceInstance != null) {
            RateSample sample = this.serviceInstance.getCurrentRate();
            updateRates(Math.round(sample.getByteSample().getOutRate() * 8), Math.round(sample.getByteSample().getInRate() * 8));
        }
    }

    @Override
    public void initialise(Map<String, Object> properties) throws TransportException {
        initialisationMap.clear();
        initialisationMap.putAll(properties);
        updateInitialisation(true);
    }

    @Override
    public void connect() throws TransportException {
        bindSemaphore.drainPermits();
        startSemaphore.drainPermits();
        try {
            serviceInstance.bind((Integer) initialisationMap.get(SLE_VERSION_KEY));
            boolean bindReturned = bindSemaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!bindReturned) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("BIND operation timeout");
            }
            if (serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.READY) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("BIND not completed successfully");
            }
            // We can start now
            serviceInstance.start(null, null, ((RafServiceInstanceConfiguration) siConfiguration).getRequestedFrameQuality());
            boolean startReturned = startSemaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!startReturned) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("START operation timeout");
            }
            if (serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("START not completed successfully");
            }
            // Up and running
        } catch (InterruptedException e) {
            throw new TransportException(e);
        }
    }

    @Override
    public void disconnect() throws TransportException {
        unbindSemaphore.drainPermits();
        stopSemaphore.drainPermits();
        try {
            serviceInstance.stop();
            boolean stopReturned = stopSemaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!stopReturned) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("STOP operation timeout");
            }
            if (serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.READY) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("STOP not completed successfully");
            }
            // We can unbind now
            serviceInstance.unbind(UnbindReasonEnum.SUSPEND);
            boolean unbindReturned = unbindSemaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!unbindReturned) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("UNBIND operation timeout");
            }
            if (serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.UNBOUND) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("UNBIND not completed successfully");
            }
            //
        } catch (InterruptedException e) {
            throw new TransportException(e);
        }
    }

    @Override
    public void abort() {
        serviceInstance.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
    }

    @Override
    public void dispose() {
        super.dispose();
        if(serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.UNBOUND) {
            serviceInstance.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
        }
    }

    @Override
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() {
        return initialisationDescriptionMap;
    }

    @Override
    public Map<String, Object> getCurrentProperties() {
        return Collections.unmodifiableMap(initialisationMap);
    }

    @Override
    public void onStateUpdated(ServiceInstance si, ServiceInstanceState state) {
        switch(state.getState()) {
            case BIND_PENDING:
            case START_PENDING:
                updateConnectionStatus(TransportConnectionStatus.CONNECTING);
                break;
            case STOP_PENDING:
            case UNBIND_PENDING:
                updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
                break;
            case ACTIVE:
                updateConnectionStatus(TransportConnectionStatus.OPEN);
                break;
            case UNBOUND:
                updateConnectionStatus(initialised ? TransportConnectionStatus.IDLE : TransportConnectionStatus.NOT_INIT);
                break;
        }
    }

    @Override
    public void onPduReceived(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
        if(operation instanceof SleBindReturn) {
            bindSemaphore.release();
        } else if(operation instanceof SleUnbindReturn) {
            unbindSemaphore.release();
        } else if(operation instanceof RafStartReturn) {
            startSemaphore.release();
        } else if(operation instanceof SleAcknowledgement) {
            stopSemaphore.release();
        } else if(operation instanceof RafTransferDataInvocation) {
            // TODO: implement
        } else if(operation instanceof RafSyncNotifyInvocation) {
            // TODO: implement
        }
    }

    @Override
    public void onPduSent(ServiceInstance si, Object operation, String name, byte[] encodedOperation) {
        // Nothing
    }

    @Override
    public void onPduSentError(ServiceInstance si, Object operation, String name, byte[] encodedOperation, String error, Exception exception) {
        // TODO: log SEVERE
    }

    @Override
    public void onPduDecodingError(ServiceInstance serviceInstance, byte[] encodedOperation) {
        // TODO: log SEVERE
    }

    @Override
    public void onPduHandlingError(ServiceInstance serviceInstance, Object operation, byte[] encodedOperation) {
        // TODO: log SEVERE
    }
}
