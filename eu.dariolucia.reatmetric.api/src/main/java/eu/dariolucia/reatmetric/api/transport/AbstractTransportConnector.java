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

package eu.dariolucia.reatmetric.api.transport;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This abstract class provides a basic skeleton for the implementation of {@link ITransportConnector} classes.
 */
abstract public class AbstractTransportConnector implements ITransportConnector {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(AbstractTransportConnector.class.getName());
    /** Log template for connection alarm state changes. */
    public static final String LOGTEMPLATE_CONNECTION_ALARM_STATE_CHANGED = "Connection %s alarm state changed to %s";

    /** The name of this transport connector. */
    private volatile String name;
    /** The description of this transport connector. */
    private volatile String description;

    /** Timer for computing bitrate. */
    private final Timer bitrateTimer;

    /** The last transmission rate in bits per second. */
    private volatile long lastTxRate = 0;
    /** The last reception rate in bits per second. */
    private volatile long lastRxRate = 0;
    /** The last alarm state of the connection. */
    private volatile AlarmState lastAlarmState = AlarmState.UNKNOWN;

    /** Map of initialization properties. */
    private final Map<String, Object> initialisationMap = new HashMap<>();
    /** Map of initialization property descriptions. */
    private final Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap = new HashMap<>();

    /** List of registered subscribers. */
    private final List<ITransportSubscriber> subscribers = new CopyOnWriteArrayList<>();
    /** The current connection status. */
    private volatile TransportConnectionStatus connectionStatus = TransportConnectionStatus.NOT_INIT;
    /** Whether the connector has been prepared. */
    private volatile boolean prepared = false;
    /** Whether the connector has been initialized. */
    private volatile boolean initialised = false;
    /** Whether the connector is currently busy. */
    private volatile boolean busy = false;
    /** Whether auto-reconnect is enabled. */
    private volatile boolean autoReconnect = false;

    /** The last transport status. */
    private volatile TransportStatus lastTransportStatus = null;

    /**
     * Constructs a new AbstractTransportConnector with the specified name and description.
     *
     * @param name the name of the transport connector
     * @param description the description of the transport connector
     */
    protected AbstractTransportConnector(String name, String description) {
        this();
        setCharacteristics(name, description);
    }

    /**
     * Default constructor.
     * Initializes the bitrate timer to compute transmission and reception rates.
     */
    private AbstractTransportConnector() {
        this.bitrateTimer = new Timer(name + " Bitrate Timer", true);
        this.bitrateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(connectionStatus == TransportConnectionStatus.OPEN) {
                    Pair<Long, Long> rate = computeBitrate();
                    if(rate != null) {
                        updateRates(rate.getFirst(), rate.getSecond());
                    } else {
                        updateRates(0, 0);
                    }
                } else {
                    updateRates(0, 0);
                }
            }
        }, 2000, 2000);
    }

    /**
     * Sets the characteristics of this transport connector.
     *
     * @param name the name of the transport connector
     * @param description the description of the transport connector
     */
    protected void setCharacteristics(String name, String description) {
        this.name = name;
        this.description = description;
        this.lastTransportStatus = new TransportStatus(name, connectionStatus, lastTxRate, lastRxRate, lastAlarmState, autoReconnect);;
    }

    /**
     * Computes the current bitrate (transmission and reception) in bits per second.
     * This method must be implemented by subclasses.
     *
     * @return a pair containing TX rate and RX rate in bits per second, or null if not available
     */
    protected abstract Pair<Long, Long> computeBitrate();

    @Override
    /**
     * Prepares this transport connector.
     * This method must be called before the connector can be initialized.
     *
     * @throws IllegalStateException if name or description is null
     */
    public void prepare() {
        if(prepared) {
            LOG.log(Level.WARNING, "Transport connector " + getName() + " already prepared");
            return;
        }
        if(name == null || description == null) {
            throw new IllegalStateException("Name and description cannot be null");
        }
        addToInitialisationMap(initialisationMap, initialisationDescriptionMap);
        prepared = true;
    }

    /**
     * Adds initialisation properties to the provided maps.
     * Subclasses can override this method to add their own initialization properties.
     *
     * @param initialisationMap the map to add initialization values to
     * @param initialisationDescriptionMap the map to add initialization property descriptions to
     */
    protected void addToInitialisationMap(Map<String, Object> initialisationMap, Map<String, Pair<String, ValueTypeEnum>> initialisationDescriptionMap) {
        // Nothing, subclasses can override
    }

    /**
     * Returns the initialization map.
     *
     * @return the map of initialization properties
     */
    protected Map<String, Object> getInitialisationMap() {
        return initialisationMap;
    }

    @Override
    /**
     * Returns the name of this transport connector.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    /**
     * Returns the description of this transport connector.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    /**
     * Returns the current connection status.
     *
     * @return the connection status
     */
    public TransportConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    /**
     * Returns the last transport status.
     *
     * @return the last transport status, may be null
     */
    public TransportStatus getLastTransportStatus() {
        return lastTransportStatus;
    }

    @Override
    /**
     * Returns whether this transport connector has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    public boolean isInitialised() {
        return initialised;
    }

    @Override
    /**
     * Sets whether auto-reconnect is enabled.
     *
     * @param autoReconnect true to enable auto-reconnect, false to disable
     */
    public void setReconnect(boolean autoReconnect) {
        boolean toNotify = !Objects.equals(autoReconnect, this.autoReconnect);
        this.autoReconnect = autoReconnect;
        if(toNotify) {
            notifySubscribers();
        }
    }

    @Override
    /**
     * Returns whether auto-reconnect is enabled.
     *
     * @return true if auto-reconnect is enabled, false otherwise
     */
    public boolean isReconnect() {
        return this.autoReconnect;
    }

    @Override
    /**
     * Initializes this transport connector with the specified properties.
     *
     * @param properties the initialization properties
     */
    public void initialise(Map<String, Object> properties) {
        checkPrepared();
        initialisationMap.clear();
        initialisationMap.putAll(properties);
        updateInitialisation(true);
    }

    /**
     * Checks if this connector has been prepared.
     *
     * @throws IllegalStateException if the connector has not been prepared
     */
    private void checkPrepared() {
        if(!prepared) {
            throw new IllegalStateException(getName() + " not prepared");
        }
    }

    @Override
    /**
     * Returns the map of supported properties.
     *
     * @return an unmodifiable map of supported properties with their descriptions
     */
    public Map<String, Pair<String, ValueTypeEnum>> getSupportedProperties() {
        checkPrepared();
        return Map.copyOf(initialisationDescriptionMap);
    }

    @Override
    /**
     * Returns the map of current properties.
     *
     * @return an unmodifiable map of current property values
     */
    public Map<String, Object> getCurrentProperties() {
        checkPrepared();
        return Map.copyOf(initialisationMap);
    }

    @Override
    /**
     * Connects this transport connector.
     *
     * @throws TransportException if the connection fails
     */
    public void connect() throws TransportException {
        checkPrepared();
        if(busy) {
            return;
        }
        busy = true;
        try {
            LOG.log(Level.INFO, "Transport connector " + name + " opening connection");
            doConnect();
        } finally {
            busy = false;
        }
    }

    /**
     * Performs the actual connection. Must be implemented by subclasses.
     *
     * @throws TransportException if the connection fails
     */
    protected abstract void doConnect() throws TransportException;

    @Override
    /**
     * Disconnects this transport connector.
     * This method clears the auto-reconnect flag before disconnecting.
     *
     * @throws TransportException if the disconnection fails
     */
    public final void disconnect() throws TransportException {
        checkPrepared();
        if(busy) {
            return;
        }
        busy = true;
        try {
            setReconnect(false); // Explicit invocation should stop reconnection
            LOG.log(Level.INFO, "Transport connector " + name + " disconnecting");
            doDisconnect();
        } finally {
            busy = false;
        }
    }

    /**
     * This method has the same effects as disconnect(), without clearing the autoreconnect flag.
     * It must be invoked by implementations wishing to perform a clean-up of the connection status,
     * without affecting the autoreconnect flag.
     *
     * @throws TransportException if the disconnection fails
     */
    protected final void internalDisconnect() throws TransportException {
        checkPrepared();
        if(busy) {
            return;
        }
        busy = true;
        try {
            doDisconnect();
        } finally {
            busy = false;
        }
    }

    /**
     * Performs the actual disconnection. Must be implemented by subclasses.
     *
     * @throws TransportException if the disconnection fails
     */
    protected abstract void doDisconnect() throws TransportException;

    @Override
    /**
     * Registers a subscriber to receive transport status updates.
     *
     * @param listener the subscriber to register
     */
    public void register(ITransportSubscriber listener) {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("New subscriber received: " + listener);
        }
        this.subscribers.add(listener);
    }

    @Override
    /**
     * Deregisters a subscriber.
     *
     * @param listener the subscriber to deregister
     */
    public void deregister(ITransportSubscriber listener) {
        if(LOG.isLoggable(Level.FINE)) {
            LOG.fine("Subscriber to remove: " + listener);
        }
        this.subscribers.remove(listener);
    }

    @Override
    /**
     * Disposes of this transport connector, releasing all resources.
     * After disposal, the connector cannot be used again.
     */
    public void dispose() {
        checkPrepared();
        this.subscribers.clear();
        this.bitrateTimer.cancel();
        doDispose();
        this.initialised = false;
    }

    /**
     * Performs cleanup operations before disposal. Must be implemented by subclasses.
     */
    protected abstract void doDispose();

    /**
     * Notifies all registered subscribers with the current transport status.
     * If a subscriber throws an exception, it is automatically deregistered.
     */
    private void notifySubscribers() {
        TransportStatus status = new TransportStatus(name, connectionStatus, lastTxRate, lastRxRate, lastAlarmState, autoReconnect);
        this.lastTransportStatus = status;
        this.subscribers.forEach((s) -> {
            try {
                if(LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Sending update to subscriber " + s + ": " + status);
                }
                s.status(status);
            } catch(Exception e) {
                LOG.log(Level.WARNING, getName() + ": cannot notify subscriber " + s + ": " + e.getMessage(), e);
                // Protect the class, unregister
                deregister(s);
            }
        });
    }

    /**
     * Updates the connection status and notifies subscribers if the status has changed.
     *
     * @param status the new connection status
     */
    protected void updateConnectionStatus(TransportConnectionStatus status) {
        boolean toNotify = !Objects.equals(status, this.connectionStatus);
        this.connectionStatus = status;
        if(toNotify) {
            notifySubscribers();
        }
    }

    /**
     * Updates the alarm state and notifies subscribers if the state has changed.
     * Also logs the state change at the appropriate level.
     *
     * @param state the new alarm state
     */
    protected void updateAlarmState(AlarmState state) {
        boolean toNotify = !Objects.equals(state, this.lastAlarmState);
        this.lastAlarmState = state;
        if(toNotify) {
            switch (state) {
                case ALARM:
                case ERROR:
                    if(LOG.isLoggable(Level.SEVERE)) {
                        LOG.log(Level.SEVERE, String.format(LOGTEMPLATE_CONNECTION_ALARM_STATE_CHANGED, getName(), state), new Object[] { getName() });
                    }
                    break;
                case WARNING:
                case VIOLATED:
                case UNKNOWN:
                    if(LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, String.format(LOGTEMPLATE_CONNECTION_ALARM_STATE_CHANGED, getName(), state), new Object[] { getName() });
                    }
                    break;
                default:
                    if(LOG.isLoggable(Level.INFO)) {
                        LOG.log(Level.INFO, String.format(LOGTEMPLATE_CONNECTION_ALARM_STATE_CHANGED, getName(), state), new Object[] { getName() });
                    }
                    break;
            }
            notifySubscribers();
        }
    }

    /**
     * Updates the transmission and reception rates and notifies subscribers if they have changed.
     *
     * @param txRate the new transmission rate in bits per second
     * @param rxRate the new reception rate in bits per second
     */
    protected void updateRates(long txRate, long rxRate) {
        boolean toNotify = txRate != lastTxRate || rxRate != lastRxRate;
        this.lastRxRate = rxRate;
        this.lastTxRate = txRate;
        if(toNotify) {
            notifySubscribers();
        }
    }

    /**
     * Updates the initialization state and notifies subscribers if it has changed.
     *
     * @param b the new initialization state
     */
    protected void updateInitialisation(boolean b) {
        boolean toNotify = initialised != b;
        initialised = b;
        if(toNotify) {
            notifySubscribers();
        }
    }
}
