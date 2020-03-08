/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.sle;

import eu.dariolucia.ccsds.sle.utl.config.PeerConfiguration;
import eu.dariolucia.ccsds.sle.utl.config.ServiceInstanceConfiguration;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

abstract public class SleServiceInstanceManager implements ITransportConnector {

    public static final String SLE_VERSION_KEY = "sle.version";

    protected final String name;
    protected final String description;

    protected final Timer bitrateTimer;

    protected final ServiceInstanceConfiguration siConfiguration;
    protected final PeerConfiguration peerConfiguration;
    protected final SpacecraftConfiguration spacecraftConfiguration;
    protected final IRawDataBroker broker;

    protected volatile long lastTxRate = 0;
    protected volatile long lastRxRate = 0;
    protected volatile String lastMessage = "";

    protected final List<ITransportSubscriber> subscribers = new CopyOnWriteArrayList<>();
    protected volatile TransportConnectionStatus connectionStatus = TransportConnectionStatus.NOT_INIT;
    protected volatile boolean initialised = false;

    protected SleServiceInstanceManager(PeerConfiguration peerConfiguration, ServiceInstanceConfiguration siConfiguration, SpacecraftConfiguration spacecraftConfiguration, IRawDataBroker broker) {
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
    }

    protected abstract void computeBitrate();

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
        this.initialised = false;
    }

    private void notifySubscribers() {
        this.subscribers.forEach((s) -> {
            try {
                s.status(this, new TransportStatus(name, lastMessage, connectionStatus, lastTxRate, lastRxRate, AlarmState.NOMINAL)); // TODO: alarm state with production status!
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
}
