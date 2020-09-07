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

public class Turbine extends StationEquipment {

    private volatile boolean status;
    private volatile boolean input;
    private volatile double output;
    private volatile double rpm;

    public Turbine(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input = true;
        output = 100.0;
        rpm = 200.0;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input ? (byte) 1 : (byte) 0);
        dos.writeDouble(output);
        dos.writeDouble(rpm);
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
            boolean oldStatus = status;
            status = switchOn == 1;
            if(oldStatus != status) {
                generateEvent(0); // Status changed event
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
        return Pair.of(commandTag, commandId == 0 || commandId == 1);
    }

    @Override
    public byte getEquipmentId() {
        return (byte) 9;
    }

    @Override
    protected void computeNewState() {
        if (status && input) {
            output = 100 + 200 * (Math.random() - 0.5);
            rpm = output * 2;
            if(rpm > 350) {
                generateEvent(1);
            }
        } else {
            output = 0.0;
            rpm = 0.0;
        }
        poll();
    }
}
