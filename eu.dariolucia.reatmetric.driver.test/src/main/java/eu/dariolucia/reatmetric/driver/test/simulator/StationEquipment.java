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

package eu.dariolucia.reatmetric.driver.test.simulator;

import eu.dariolucia.reatmetric.api.common.Pair;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class StationEquipment {

    protected final ScheduledExecutorService scheduler;
    protected volatile IStationMonitor monitor;

    public StationEquipment(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        this.scheduler.scheduleAtFixedRate(this::computeNewState, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public void connect(IStationMonitor monitor) {
        this.monitor = monitor;
    }

    public void disconnect() {
        this.monitor = null;
    }

    public void poll() {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bs);
            byte firstByte = getEquipmentId();
            firstByte <<= 4;
            firstByte |= 0x00;
            dos.write(firstByte);
            dos.writeLong(System.currentTimeMillis());
            doWriteMonitoringState(dos);
            dos.close();
            notifyPacket(bs.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void execute(byte[] command) {
        Pair<Integer, Boolean> cmd = accept(command);
        notifyCommandAccepted(cmd);
        if(cmd.getSecond()) {
            scheduler.execute(() ->  {
                notifyCommandReport(cmd.getFirst(), 0x03, true);
                boolean result = doExecute(command);
                notifyCommandReport(cmd.getFirst(), 0x04, true);
            });
        }
    }

    private void notifyCommandReport(int commandId, int reportType, boolean result) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bs);
            byte firstByte = getEquipmentId();
            firstByte <<= 4;
            firstByte |= reportType;
            dos.write(firstByte);
            dos.writeLong(System.currentTimeMillis());
            dos.writeInt(commandId);
            dos.writeByte(result ? 1 : 0);
            dos.close();
            notifyPacket(bs.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyCommandAccepted(Pair<Integer, Boolean> cmd) {
        notifyCommandReport(cmd.getFirst(), 0x02, cmd.getSecond());
    }

    protected abstract Pair<Integer, Boolean> accept(byte[] command);

    protected abstract void doWriteMonitoringState(DataOutputStream dos) throws IOException;

    public abstract boolean doExecute(byte[]command);

    public abstract byte getEquipmentId();

    protected abstract void computeNewState();

    protected void notifyPacket(byte[] data) {
        if(this.monitor != null) {
            scheduler.execute(() -> {
                IStationMonitor m = this.monitor;
                if (m != null) {
                    m.notifyPacket(data);
                }
            });
        }
    }
}
