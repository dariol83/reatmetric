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
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
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

public class StationTransportConnector extends AbstractTransportConnector implements IStationMonitor {

    private static final Logger LOG = Logger.getLogger(StationTransportConnector.class.getName());

    private final StationSimulator simulator = new StationSimulator();

    private volatile long txBytes;
    private volatile long rxBytes;
    private volatile long lastBitrateInstant;

    private final IRawDataBroker broker;
    private volatile boolean connected;

    protected StationTransportConnector(String name, String description, IRawDataBroker broker) {
        super(name, description);
        this.broker = broker;
    }

    public void send(byte[] command) throws IOException {
        if(!connected) {
            throw new IOException("Not connected");
        }
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
        this.simulator.connect(this);
        this.connected = true;
    }

    @Override
    protected void doDisconnect() {
        this.simulator.disconnect();
        this.connected = false;
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
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            byte first = dis.readByte();
            int type = first & 0x0F;
            long timestamp = dis.readLong();
            RawData rd = new RawData(broker.nextRawDataId(), Instant.ofEpochMilli(timestamp), getRawDataName(first), getRawDataType(type), TestDriver.STATION_ROUTE, TestDriver.STATION_SOURCE, Quality.GOOD, null, data, Instant.now(), null);
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
