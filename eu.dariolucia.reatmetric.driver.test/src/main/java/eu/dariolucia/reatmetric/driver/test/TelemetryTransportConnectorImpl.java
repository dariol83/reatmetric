/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TelemetryTransportConnectorImpl implements ITransportConnector {

    private final String name;
    private final String description;
    private final List<ITransportSubscriber> subscriber = new CopyOnWriteArrayList<>();
    private final ProcessingDefinition definitions;
    private final IProcessingModel model;
    private final ExecutorService notifier = Executors.newSingleThreadExecutor();
    private volatile boolean connected;
    private volatile boolean initialised;
    private volatile String message;

    public TelemetryTransportConnectorImpl(String name, String description, ProcessingDefinition definitions, IProcessingModel model) {
        this.name = name;
        this.description = description;
        this.definitions = definitions;
        this.model = model;
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
    public boolean isInitialised() {
        return initialised;
    }

    @Override
    public void initialise(Map<String, Object> properties) {
        initialised = true;
        notifyState();
    }

    @Override
    public void connect() {
        connected = true;
        notifyState();
        startTmGeneration();
    }

    @Override
    public void disconnect() {
        connected = false;
        notifyState();
    }

    @Override
    public void abort() {
        connected = false;
        notifyState();
    }

    @Override
    public void dispose() {
        connected = false;
        initialised = false;
        notifyState();
    }

    private void notifyState() {
        for(ITransportSubscriber s : subscriber) {
            notifyState(s);
        }
    }

    @Override
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() {
        return Collections.emptyMap();
    }

    @Override
    public void register(ITransportSubscriber listener) {
        if(!subscriber.contains(listener)) {
            subscriber.add(listener);
            notifyState(listener);
        }
    }

    private void notifyState(ITransportSubscriber listener) {
        TransportStatus status = buildTransportStatus();
        notifier.execute(() -> listener.status(status));
    }

    private TransportStatus buildTransportStatus() {
        return new TransportStatus(name, message, deriveStatus(), 0, 0);
    }

    private TransportConnectionStatus deriveStatus() {
        if(connected) {
            return TransportConnectionStatus.OPEN;
        } else {
            if(initialised) {
                return TransportConnectionStatus.IDLE;
            } else {
                return TransportConnectionStatus.NOT_INIT;
            }
        }
    }

    @Override
    public void deregister(ITransportSubscriber listener) {
        subscriber.remove(listener);
    }
}
