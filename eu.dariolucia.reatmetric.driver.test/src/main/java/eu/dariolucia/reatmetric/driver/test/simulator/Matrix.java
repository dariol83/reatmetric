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

public class Matrix extends StationEquipment {

    private volatile boolean status;
    private volatile boolean input1;
    private volatile boolean input2;
    private volatile boolean output;
    private volatile int wiring;
    private volatile int diagnostic1;
    private volatile int diagnostic2;
    private volatile int diagnostic3;
    private volatile int diagnostic4;

    public Matrix(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input1 = true;
        input2 = false;
        output = true;
        wiring = 1;
        diagnostic1 = (int) (Math.random() * 100);
        diagnostic2 = (int) (Math.random() * 100);
        diagnostic3 = (int) (Math.random() * 100);
        diagnostic4 = (int) (Math.random() * 100);
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input1 ? (byte) 1 : (byte) 0);
        dos.writeByte(input2 ? (byte) 1 : (byte) 0);
        dos.writeByte(output ? (byte) 1 : (byte) 0);
        dos.writeByte(wiring);
        dos.writeByte(diagnostic1);
        dos.writeByte(diagnostic2);
        dos.writeByte(diagnostic3);
        dos.writeByte(diagnostic4);
    }

    @Override
    public boolean doExecute(byte[] command) {
        ByteBuffer bb = ByteBuffer.wrap(command);
        byte firstByte = bb.get();
        int commandId = bb.getInt();
        int commandTag = bb.getInt();
        if (commandId == 0) { // Operate on output
            int switchOn = bb.getInt();
            output = switchOn == 1;
            return true;
        } else if (commandId == 1) { // Operate on status
            int switchOn = bb.getInt();
            status = switchOn == 1;
            return true;
        } else if (commandId == 2) { // Operate on input status
            int inputId = bb.getInt();
            int switchOn = bb.getInt();
            switch(inputId) {
                case 1: input1 = switchOn == 1; break;
                case 2: input2 = switchOn == 1; break;
                default: return false;
            }
            return true;
        } else if (commandId == 3) { // Operate on wiring
            wiring = bb.getInt();
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
        return 4;
    }

    @Override
    protected void computeNewState() {
        if(!status) {
            diagnostic1 = -1;
            diagnostic2 = -1;
            diagnostic3 = -1;
            diagnostic4 = -1;
        } else {
            diagnostic1 = (int) (Math.random() * 100);
            diagnostic2 = (int) (Math.random() * 100);
            diagnostic3 = (int) (Math.random() * 100);
            diagnostic4 = (int) (Math.random() * 100);
        }
        poll();
    }
}
