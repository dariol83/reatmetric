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

    public VentilationGrid(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        input = true;
        fan1 = 0;
        fan2 = 0;
        fan3 = 0;
        fan4 = 0;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(input ? (byte) 1 : (byte) 0);
        dos.writeDouble(fan1);
        dos.writeDouble(fan2);
        dos.writeDouble(fan3);
        dos.writeDouble(fan4);
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
        return 7;
    }

    @Override
    protected void computeNewState() {
        if(status && input) {
            fan1 = 220 + 60 * (Math.random() - 0.5);
            fan2 = 220 + 60 * (Math.random() - 0.5);
            fan3 = 220 + 60 * (Math.random() - 0.5);
            fan4 = 220 + 60 * (Math.random() - 0.5);
        } else {
            fan1 = 0;
            fan2 = 0;
            fan3 = 0;
            fan4 = 0;
        }
        poll();
    }
}
