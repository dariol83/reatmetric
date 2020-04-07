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

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.ITransportSubscriber;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TelecommandTransportConnectorImpl implements ITransportConnector {

    private final String name;
    private final String type;
    private final String[] routes;
    private final List<ITransportSubscriber> subscriber = new CopyOnWriteArrayList<>();
    private final ExecutorService notifier = Executors.newSingleThreadExecutor();
    private volatile boolean connected;
    private volatile boolean initialised;
    private volatile String message;
    private volatile Thread generator;
    private final Map<String, Object> properties = new HashMap<>();

    public TelecommandTransportConnectorImpl(String name, String type, String[] routes) {
        this.name = name;
        this.type = type;
        this.routes = routes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Connector " + name + " for type " + type + ": " + Arrays.toString(routes);
    }

    @Override
    public TransportConnectionStatus getConnectionStatus() {
        return deriveStatus();
    }

    @Override
    public void prepare() {
        // Nothing to be done
    }

    public String getType() {
        return type;
    }

    public List<String> getRoutes() {
        return Arrays.asList(routes);
    }

    public boolean isReady() {
        return connected && initialised;
    }

    @Override
    public boolean isInitialised() {
        return initialised;
    }

    @Override
    public void initialise(Map<String, Object> properties) {
        this.initialised = true;
        this.message = "Initialised";
        this.properties.clear();
        this.properties.putAll(properties);
        notifyState();
    }

    @Override
    public void connect() {
        connected = true;
        message = "Connected";
        notifyState();
    }

    @Override
    public void disconnect() {
        connected = false;
        message = "Disconnected";
        notifyState();
    }

    @Override
    public void abort() {
        connected = false;
        message = "Aborted";
        notifyState();
    }

    @Override
    public void dispose() {
        connected = false;
        initialised = false;
        message = "Disposed";
        notifyState();
    }

    private void notifyState() {
        for(ITransportSubscriber s : subscriber) {
            notifyState(s);
        }
    }

    @Override
    public Map<String, Object> getCurrentProperties() {
        return Collections.unmodifiableMap(this.properties);
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
        notifier.execute(() -> listener.status(this, status));
    }

    private TransportStatus buildTransportStatus() {
        return new TransportStatus(name, message, deriveStatus(), 0, 0, AlarmState.WARNING);
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
