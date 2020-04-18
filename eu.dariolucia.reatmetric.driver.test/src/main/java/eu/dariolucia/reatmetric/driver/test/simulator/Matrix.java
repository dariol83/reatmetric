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

    public Matrix(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input1 = true;
        input2 = false;
        output = true;
        wiring = 1;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input1 ? (byte) 1 : (byte) 0);
        dos.writeByte(input2 ? (byte) 1 : (byte) 0);
        dos.writeByte(output ? (byte) 1 : (byte) 0);
        dos.writeByte(wiring);
    }

    @Override
    public boolean doExecute(byte[] command) {
        // TODO: implement
        return false;
    }

    @Override
    protected Pair<Integer, Boolean> accept(byte[] command) {
        ByteBuffer bb = ByteBuffer.wrap(command);
        byte firstByte = bb.get();
        int commandId = bb.getInt();
        int commandTag = bb.getInt();
        // TODO: implement command Id checking
        return Pair.of(commandTag, true);
    }

    @Override
    public byte getEquipmentId() {
        return 4;
    }

    @Override
    protected void computeNewState() {
        poll();
    }
}
