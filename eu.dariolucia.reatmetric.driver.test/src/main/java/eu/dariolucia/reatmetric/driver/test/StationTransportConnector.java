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
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.test.simulator.IStationMonitor;
import eu.dariolucia.reatmetric.driver.test.simulator.StationSimulator;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The objective of this connector is to forward commands to the controlled system, and provides monitoring data and
 * events back to the ReatMetric system. The raw data broker is used to provide raw data into the system for further
 * processing by the driver. {@link eu.dariolucia.reatmetric.api.transport.ITransportConnector} are meant to be simple
 * communication media to connect the M&amp;C system to the controlled system, and they are not meant to do any processing,
 * besides recognizing the type of data and giving it a name.
 */
public class StationTransportConnector extends AbstractTransportConnector implements IStationMonitor {

    private static final Logger LOG = Logger.getLogger(StationTransportConnector.class.getName());

    private final StationSimulator simulator = new StationSimulator();

    private volatile long txBytes;
    private volatile long rxBytes;
    private volatile long lastBitrateInstant;

    private final IRawDataBroker broker;
    private final String driverName;
    private volatile boolean connected;

    protected StationTransportConnector(String driverName, String name, String description, IRawDataBroker broker) {
        super(name, description);
        this.driverName = driverName;
        this.broker = broker;
    }

    public void send(byte[] command) throws IOException {
        if(!connected) {
            throw new IOException("Not connected");
        }
        synchronized(this) {
            txBytes += command.length;
        }
        simulator.execute(command);
    }

    @Override
    protected synchronized Pair<Long, Long> computeBitrate() {
        long now = System.currentTimeMillis();
        Pair<Long, Long> toReturn = Pair.of(
                (long) ((txBytes * 8.0) / ((now - lastBitrateInstant) / 1000.0)),
                (long) ((rxBytes * 8.0) / ((now - lastBitrateInstant) / 1000.0)));
        txBytes = 0;
        rxBytes = 0;
        lastBitrateInstant = now;
        return toReturn;
    }

    @Override
    protected void doConnect() {
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        this.simulator.connect(this);
        this.connected = true;
        updateConnectionStatus(TransportConnectionStatus.OPEN);
        updateAlarmState(AlarmState.NOMINAL);
    }

    @Override
    protected void doDisconnect() {
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        this.simulator.disconnect();
        this.connected = false;
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOT_APPLICABLE);
    }

    @Override
    protected void doDispose() {
        try {
            disconnect();
        } catch (TransportException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void abort() throws TransportException {
        disconnect();
    }

    @Override
    public void notifyPacket(byte[] data) {
        synchronized(this) {
            rxBytes += data.length;
        }
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            byte first = dis.readByte();
            int type = first & 0x0F;
            long timestamp = dis.readLong();
            RawData rd = new RawData(broker.nextRawDataId(), Instant.ofEpochMilli(timestamp), getRawDataName(first), getRawDataType(type), TestDriver.STATION_ROUTE, TestDriver.STATION_SOURCE, Quality.GOOD, null, data, Instant.now(), driverName, null);
            broker.distribute(Collections.singletonList(rd));
        } catch (IOException | ReatmetricException e) {
            LOG.log(Level.SEVERE, "Packet notification error: " + e.getMessage(), e);
        }
    }

    private String getRawDataType(int type) {
        switch(type) {
            case 0:
                return TestDriver.STATION_TM;
            case 1:
                return TestDriver.STATION_EVENT;
            case 2:
            case 3:
            case 4:
                return TestDriver.STATION_ACK;
            case 15:
                return TestDriver.STATION_CMD;
            default:
                return "UNKNOWN";
        }
    }

    private String getRawDataName(int d) {
        int eqId = (d & 0xF0) >>> 4;
        int type = d & 0x0F;
        switch(eqId) {
            case 1:
                return "POWER SUPPLY " + getRawDataType(type);
            case 2:
                return "DIESEL 1 " + getRawDataType(type);
            case 3:
                return "DIESEL 2 " + getRawDataType(type);
            case 4:
                return "POWER MATRIX " + getRawDataType(type);
            case 5:
                return "SWITCH " + getRawDataType(type);
            case 6:
                return "SPLITTER " + getRawDataType(type);
            case 7:
                return "VENTILATION " + getRawDataType(type);
            case 8:
                return "THERMAL " + getRawDataType(type);
            case 9:
                return "TURBINE " + getRawDataType(type);
            default:
                return "UNKNOWN";
        }
    }

    public boolean isReady() {
        return connected;
    }
}
