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
import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.ccsds.sle.utl.si.*;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

abstract public class SleServiceInstanceManager<T extends ServiceInstance, K extends ServiceInstanceConfiguration> implements ITransportConnector, IServiceInstanceListener {

    public static final String SLE_VERSION_KEY = "sle.version";

    private final String name;
    private final String description;

    private final Timer bitrateTimer;

    protected final T serviceInstance;
    protected final K siConfiguration;
    protected final PeerConfiguration peerConfiguration;

    protected final SpacecraftConfiguration spacecraftConfiguration;
    protected final IRawDataBroker broker;

    private volatile long lastTxRate = 0;
    private volatile long lastRxRate = 0;
    private volatile String lastMessage = "";
    private volatile AlarmState lastAlarmState = AlarmState.UNKNOWN;

    private final Map<String, Object> initialisationMap = new HashMap<>();
    private final Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap = new HashMap<>();

    private final Semaphore bindSemaphore = new Semaphore(0);
    private final Semaphore unbindSemaphore = new Semaphore(0);
    private final Semaphore startSemaphore = new Semaphore(0);
    private final Semaphore stopSemaphore = new Semaphore(0);

    private final List<ITransportSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private volatile TransportConnectionStatus connectionStatus = TransportConnectionStatus.NOT_INIT;
    private volatile boolean initialised = false;

    protected SleServiceInstanceManager(PeerConfiguration peerConfiguration, K siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
        this.name = siConfiguration.getServiceInstanceIdentifier();
        this.description = siConfiguration.getType().name() + " " + siConfiguration.getServiceInstanceIdentifier();
        this.peerConfiguration = peerConfiguration;
        this.siConfiguration = siConfiguration;
        this.broker = broker;
        this.spacecraftConfiguration = spacecraftConfiguration;
        this.bitrateTimer = new Timer(name + " Bitrate Timer", true);
        this.bitrateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(connectionStatus == TransportConnectionStatus.OPEN) {
                    computeBitrate();
                } else {
                    updateRates(0, 0);
                }
            }
        }, 2000, 2000);

        this.serviceInstance = createServiceInstance(peerConfiguration, siConfiguration);
        this.serviceInstance.register(this);

        this.initialisationMap.put(SLE_VERSION_KEY, siConfiguration.getServiceVersionNumber());
        this.initialisationDescriptionMap.put(SLE_VERSION_KEY, Pair.of("SLE Version", ValueTypeEnum.UNSIGNED_INTEGER));
        addToInitialisationMap(initialisationMap, initialisationDescriptionMap);
    }

    protected void addToInitialisationMap(Map<String, Object> initialisationMap, Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap) {
        // Nothing, subclasses can override
    }

    protected abstract T createServiceInstance(PeerConfiguration peerConfiguration, K siConfiguration);

    private void computeBitrate() {
        if(this.serviceInstance != null) {
            RateSample sample = this.serviceInstance.getCurrentRate();
            updateRates(Math.round(sample.getByteSample().getOutRate() * 8), Math.round(sample.getByteSample().getInRate() * 8));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TransportConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public boolean isInitialised() {
        return initialised;
    }

    @Override
    public void initialise(Map<String, Object> properties) {
        initialisationMap.clear();
        initialisationMap.putAll(properties);
        updateInitialisation(true);
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
            sendStart();

            boolean startReturned = startSemaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!startReturned) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("START operation timeout");
            }
            if (serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.ACTIVE) {
                serviceInstance.peerAbort(PeerAbortReasonEnum.OTHER_REASON);
                throw new TransportException("START not completed successfully");
            }

            finalizeConnection();

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
            sendStop();

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
    public void register(ITransportSubscriber listener) {
        this.subscribers.add(listener);
    }

    @Override
    public void deregister(ITransportSubscriber listener) {
        this.subscribers.remove(listener);
    }

    @Override
    public void dispose() {
        this.subscribers.clear();
        this.bitrateTimer.cancel();
        if(serviceInstance.getCurrentBindingState() != ServiceInstanceBindingStateEnum.UNBOUND) {
            serviceInstance.peerAbort(PeerAbortReasonEnum.OPERATIONAL_REQUIREMENTS);
        }
        this.initialised = false;
    }

    private void notifySubscribers() {
        this.subscribers.forEach((s) -> {
            try {
                s.status(this, new TransportStatus(name, lastMessage, connectionStatus, lastTxRate, lastRxRate, lastAlarmState));
            } catch(Exception e) {
                // TODO: log
                e.printStackTrace();
            }
        });
    }

    protected void updateConnectionStatus(TransportConnectionStatus status) {
        boolean toNotify = !Objects.equals(status, this.connectionStatus);
        this.connectionStatus = status;
        if(toNotify) {
            notifySubscribers();
        }
    }

    protected void updateMessage(String message) {
        boolean toNotify = !Objects.equals(message, this.lastMessage);
        this.lastMessage = message;
        if(toNotify) {
            notifySubscribers();
        }
    }

    protected void updateAlarmState(AlarmState state) {
        boolean toNotify = !Objects.equals(state, this.lastAlarmState);
        this.lastAlarmState = state;
        if(toNotify) {
            notifySubscribers();
        }
    }

    protected void updateRates(long txRate, long rxRate) {
        boolean toNotify = txRate != lastTxRate || rxRate != lastRxRate;
        this.lastRxRate = rxRate;
        this.lastTxRate = txRate;
        if(toNotify) {
            notifySubscribers();
        }
    }

    protected void updateInitialisation(boolean b) {
        boolean toNotify = initialised != b;
        initialised = b;
        if(toNotify) {
            notifySubscribers();
        }
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
        } else if(isStartReturn(operation)) {
            startSemaphore.release();
        } else if(operation instanceof SleAcknowledgement) {
            stopSemaphore.release();
        } else {
            handleOperation(operation);
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

    protected abstract boolean isStartReturn(Object operation);

    protected abstract void handleOperation(Object operation);

    protected abstract void finalizeConnection();

    protected abstract void sendStart();

    protected abstract void sendStop();
}
