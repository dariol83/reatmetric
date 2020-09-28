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

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

public class VentilationGrid extends StationEquipment {

    private volatile boolean status;
    private volatile boolean input;
    private volatile double fan1;
    private volatile double fan2;
    private volatile double fan3;
    private volatile double fan4;
    private volatile int fan1status;
    private volatile int fan1input;
    private volatile int fan2status;
    private volatile int fan2input;
    private volatile int fan3status;
    private volatile int fan3input;
    private volatile int fan4status;
    private volatile int fan4input;

    public VentilationGrid(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input = true;
        fan1 = 0;
        fan2 = 0;
        fan3 = 0;
        fan4 = 0;
        fan1status = 1;
        fan1input = 1;
        fan2status = 1;
        fan2input = 1;
        fan3status = 1;
        fan3input = 1;
        fan4status = 1;
        fan4input = 1;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input ? (byte) 1 : (byte) 0);
        dos.writeDouble(fan1);
        dos.writeDouble(fan2);
        dos.writeDouble(fan3);
        dos.writeDouble(fan4);
        dos.writeInt(fan1status);
        dos.writeInt(fan1input);
        dos.writeInt(fan2status);
        dos.writeInt(fan2input);
        dos.writeInt(fan3status);
        dos.writeInt(fan3input);
        dos.writeInt(fan4status);
        dos.writeInt(fan4input);
    }

    @Override
    public boolean doExecute(byte[] command) {
        ByteBuffer bb = ByteBuffer.wrap(command);
        byte firstByte = bb.get();
        int commandId = bb.getInt();
        int commandTag = bb.getInt();
        if (commandId == 0) { // Operate on input
            int switchOn = bb.getInt();
            input = switchOn == 1;
            return true;
        } else if (commandId == 1) { // Operate on status
            int switchOn = bb.getInt();
            status = switchOn == 1;
            return true;
        } else if (commandId == 2) { // Operate on fan status
            int fanId = bb.getInt();
            int switchOn = bb.getInt();
            switch(fanId) {
                case 1: fan1status = switchOn; break;
                case 2: fan2status = switchOn; break;
                case 3: fan3status = switchOn; break;
                case 4: fan4status = switchOn; break;
                default: return false;
            }
            return true;
        } else if (commandId == 3) { // Do nothing except waiting for n seconds
            int n = bb.getInt();
            try {
                Thread.sleep(n * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected Pair<Integer, Boolean> accept(byte[] command) {
        ByteBuffer bb = ByteBuffer.wrap(command);
        byte firstByte = bb.get();
        int commandId = bb.getInt();
        int commandTag = bb.getInt();
        return Pair.of(commandTag, commandId == 0 || commandId == 1 || commandId == 2 || commandId == 3);
    }

    @Override
    public byte getEquipmentId() {
        return 7;
    }

    @Override
    protected void computeNewState() {
        if(status && input) {
            fan1 = (fan1status == 1) ? 220 + 60 * (Math.random() - 0.5) : 0;
            fan2 = (fan2status == 1) ? 220 + 60 * (Math.random() - 0.5) : 0;
            fan3 = (fan3status == 1) ? 220 + 60 * (Math.random() - 0.5) : 0;
            fan4 = (fan4status == 1) ? 220 + 60 * (Math.random() - 0.5) : 0;
        } else {
            fan1 = 0;
            fan2 = 0;
            fan3 = 0;
            fan4 = 0;
        }
        poll();
    }
}
