/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.connectors;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.api.value.StringUtil;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.common.Constants;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This connector established a TCP/IP connection to a host on a given port, and uses this connection to:
 * <ul>
 *     <li>Receive data</li>
 *     <li>Send data</li>
 * </ul>
 */
public abstract class AbstractFullDuplexTcpConnector extends AbstractTransportConnector {
    private static final Logger LOG = Logger.getLogger(AbstractFullDuplexTcpConnector.class.getName());

    private volatile Instant lastSamplingTime;
    private final AtomicLong rxBytes = new AtomicLong(0); // received bytes
    private final AtomicLong txBytes = new AtomicLong(0); // injected bytes
    private String driverName;
    private SpacecraftConfiguration spacecraftConfig;
    private IServiceCoreContext context;
    private String host;
    private int port;
    private volatile Socket socket;
    private volatile boolean closing;

    private final ExecutorService dataForwarderExecutor;

    private Thread readingDataThread = null;

    protected AbstractFullDuplexTcpConnector(String name, String description) {
        super(name, description);
        this.dataForwarderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, getName() + " - forward thread");
            t.setDaemon(true);
            return t;
        });
    }

    protected final void setConnectionInformation(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        Instant now = Instant.now();
        if(lastSamplingTime != null) {
            long theRxBytes = rxBytes.get(); // Not atomic, but ... who cares
            long theTxBytes = txBytes.get(); // Not atomic, but ... who cares
            rxBytes.set(0);
            txBytes.set(0);
            long msDiff = now.toEpochMilli() - lastSamplingTime.toEpochMilli();
            long rxRate = Math.round((theRxBytes * 8000.0) / (msDiff));
            long txRate = Math.round((theTxBytes * 8000.0) / (msDiff));
            lastSamplingTime = now;
            return Pair.of(txRate, rxRate);
        } else {
            lastSamplingTime = now;
            return null;
        }
    }

    @Override
    protected synchronized void doConnect() throws TransportException {
        if(socket == null) {
            closing = false;
            updateConnectionStatus(TransportConnectionStatus.CONNECTING);
            try {
                // Connect, start thread to read data units and send those to the raw data broker for processing
                socket = new Socket(this.host, this.port);
                final InputStream dataStream = socket.getInputStream();
                readingDataThread = new Thread(() -> {
                    readIncomingData(dataStream);
                }, getName() + "- reading thread");
                updateConnectionStatus(TransportConnectionStatus.OPEN);
                updateAlarmState(AlarmState.NOMINAL);
                readingDataThread.setDaemon(true);
                readingDataThread.start();
            } catch (IOException e) {
                updateConnectionStatus(TransportConnectionStatus.ERROR);
                updateAlarmState(AlarmState.ERROR);
                throw new TransportException(e);
            }
        }
        // Do nothing
    }

    private void readIncomingData(InputStream inputStream) {
        while(this.socket != null) {
            try {
                // Read the data unit
                byte[] dataUnit = readDataUnit(inputStream);
                // Record reception time
                Instant receptionTime = Instant.now();
                // If connection drop, exception
                if(dataUnit == null || dataUnit.length == 0) {
                    throw new IOException("End of stream");
                }
                // Increase RX counter
                rxBytes.addAndGet(dataUnit.length);
                // Process the data unit
                processDataUnit(dataUnit, receptionTime);
            } catch (IOException e) {
                readingError(e, "connection error on read");
            } catch (Exception e) {
                readingError(e, "unknown error");
            }
        }
    }

    protected abstract void processDataUnit(byte[] dataUnit, Instant receptionTime);
    protected abstract byte[] readDataUnit(InputStream inputStream) throws Exception;

    protected RawData createRawData(Instant generationTime, String name, String type,
                                    String route, String source, Quality quality,
                                    IUniqueId relatedItem, byte[] contents,
                                    Instant receptionTime, Object extension) {
        return new RawData(context.getRawDataBroker().nextRawDataId(),
                generationTime,
                name,
                type,
                route,
                source,
                quality,
                relatedItem,
                contents,
                receptionTime,
                driverName,
                extension);
    }

    protected boolean distributeRawData(RawData data) {
        try {
            context.getRawDataBroker().distribute(Collections.singletonList(data));
            return true;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, getName() + ": error when distributing data unit from " + host + ":" + port + ": " + e.getMessage(), e);
            updateAlarmState(AlarmState.WARNING);
            return false;
        }
    }

    private void readingError(Exception e, String reason) {
        if(!closing) {
            LOG.log(Level.SEVERE, getName() + ": reading of data unit failed: " + reason + ": " + e.getMessage(), e);
            try {
                internalDisconnect();
            } catch (TransportException ex) {
                // Ignore
            }
            updateAlarmState(AlarmState.ERROR);
            updateConnectionStatus(TransportConnectionStatus.ERROR);
        }
    }

    @Override
    protected synchronized void doDisconnect() {
        if(socket != null) {
            closing = true;
            updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
            socket = null;
        }
        if(readingDataThread != null) {
            readingDataThread = null;
        }
        // Done
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOT_CHECKED);
    }

    @Override
    protected synchronized void doDispose() {
        try {
            disconnect();
        } catch (TransportException e) {
            // Do nothing
        }
        dataForwarderExecutor.shutdownNow();
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    protected void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation) {
        this.driverName = driverName;
        this.spacecraftConfig = configuration;
        this.context = context;
        initialiseInternalConfiguration(driverName, configuration, context, connectorInformation);
    }

    protected abstract void initialiseInternalConfiguration(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation);

    /**
     * This method must be internally called by subclasses to send data via the connection.
     *
     * @param data
     * @param trackingInformation
     * @param activityId
     * @param activityOccurrenceId
     * @return true if the data unit is queued for release, false otherwise
     */
    protected boolean sendDataUnit(byte[] data, Object trackingInformation, int activityId, IUniqueId activityOccurrenceId) {
        if(this.dataForwarderExecutor.isShutdown()) {
            LOG.log(Level.SEVERE, String.format("%s: transmission of data unit with ID %s failed: connector disposed", getName(), activityOccurrenceId));
            reportActivityRelease(trackingInformation, activityId, activityOccurrenceId, Instant.now(), ActivityReportState.FATAL);
            return false;
        }
        reportActivityRelease(trackingInformation, activityId, activityOccurrenceId, Instant.now(), ActivityReportState.OK);
        reportActivityReceptionPending(trackingInformation, activityId, activityOccurrenceId, Instant.now());
        this.dataForwarderExecutor.submit(() -> {
            Socket sock = this.socket;
            if (sock == null) {
                LOG.severe(String.format("%s: transmission of data unit with ID %s failed: connector is disconnected", getName(), activityOccurrenceId));
                reportActivityRelease(trackingInformation, activityId, activityOccurrenceId, Instant.now(), ActivityReportState.FATAL);
                return;
            }
            // Try to send the data unit
            Instant sent = null;
            try {
                LOG.log(Level.INFO, String.format("%s: sending data unit with ID %d: %s", activityOccurrenceId, StringUtil.toHexDump(data)));
                sock.getOutputStream().write(data);
                sent = Instant.now();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, String.format("%s: transmission of data unit with ID %d failed: connector error on write", getName(), activityOccurrenceId), e);
                reportActivityReceptionFailure(trackingInformation, activityId, activityOccurrenceId, Instant.now());
                return;
            }
            txBytes.addAndGet(data.length);
            // Sent
            reportActivityReceptionOk(trackingInformation, activityId, activityOccurrenceId, sent);
        });
        return true;
    }

    /**
     * Subclasses can override to use the tracking information
     *
     * @param trackingInformation
     * @param activityId
     * @param activityOccurrenceId
     * @param progressTime
     * @param status
     */
    protected void reportActivityRelease(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime, ActivityReportState status) {
        reportActivityState(activityId, activityOccurrenceId, progressTime, ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, status, ActivityOccurrenceState.TRANSMISSION);
    }

    /**
     * Subclasses can override to use the tracking information
     *
     * @param trackingInformation
     * @param activityId
     * @param activityOccurrenceId
     * @param progressTime
     */
    protected void reportActivityReceptionPending(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        reportActivityState(activityId, activityOccurrenceId, progressTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ENDPOINT_RECEPTION, ActivityReportState.PENDING, ActivityOccurrenceState.TRANSMISSION);
    }

    /**
     * Subclasses can override to use the tracking information
     *
     * @param trackingInformation
     * @param activityId
     * @param activityOccurrenceId
     * @param progressTime
     */
    protected void reportActivityReceptionOk(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        reportActivityState(activityId, activityOccurrenceId, progressTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ENDPOINT_RECEPTION, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION);
    }

    /**
     * Subclasses can override to use the tracking information
     *
     * @param trackingInformation
     * @param activityId
     * @param activityOccurrenceId
     * @param progressTime
     */
    protected void reportActivityReceptionFailure(Object trackingInformation, int activityId, IUniqueId activityOccurrenceId, Instant progressTime) {
        reportActivityState(activityId, activityOccurrenceId, progressTime, ActivityOccurrenceState.TRANSMISSION, Constants.STAGE_ENDPOINT_RECEPTION, ActivityReportState.FATAL, ActivityOccurrenceState.TRANSMISSION);
    }

    private void reportActivityState(int activityId, IUniqueId activityOccurrenceId, Instant progressTime, ActivityOccurrenceState currentState, String name, ActivityReportState status, ActivityOccurrenceState nextState) {
        context.getProcessingModel().reportActivityProgress(ActivityProgress.of(activityId, activityOccurrenceId, name, progressTime, currentState, null, status, nextState, null));
    }
}
