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

public class Splitter extends StationEquipment {

    private volatile boolean status;
    private volatile boolean input;
    private volatile boolean output1;
    private volatile boolean output2;
    private volatile boolean output3;
    private volatile double power;

    public Splitter(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input = true;
        output1 = true;
        output2 = true;
        output3 = true;
        power = 1500;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input ? (byte) 1 : (byte) 0);
        dos.writeByte(output1 ? (byte) 1 : (byte) 0);
        dos.writeByte(output2 ? (byte) 1 : (byte) 0);
        dos.writeByte(output3 ? (byte) 1 : (byte) 0);
        dos.writeDouble(power);
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
        } else if (commandId == 2) { // Operate on output status
            int outputId = bb.getInt();
            int switchOn = bb.getInt();
            switch(outputId) {
                case 1: output1 = switchOn == 1; break;
                case 2: output2 = switchOn == 1; break;
                case 3: output3 = switchOn == 1; break;
                default: return false;
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
        return Pair.of(commandTag, commandId == 0 || commandId == 1 || commandId == 2);
    }

    @Override
    public byte getEquipmentId() {
        return 6;
    }

    @Override
    protected void computeNewState() {
        if(status && input) {
            int base = 600 + (output1 ? 300 : 0) + (output2 ? 300 : 0) + (output3 ? 300 : 0);
            power = base + 500 * (Math.random() - 0.5);
        } else {
            power = 0.0;
        }
        poll();
    }
}
