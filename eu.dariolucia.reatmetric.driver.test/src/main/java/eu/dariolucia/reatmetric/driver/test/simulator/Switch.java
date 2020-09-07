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

public class Switch extends StationEquipment {

    private volatile int position;

    public Switch(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        position = 1;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(position);
    }

    @Override
    public boolean doExecute(byte[] command) {
        ByteBuffer bb = ByteBuffer.wrap(command);
        byte firstByte = bb.get(); // Equipment
        int commandId = bb.getInt();
        int commandTag = bb.getInt();
        if(commandId == 0) { // Change switch position
            position = bb.getInt();
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
        return Pair.of(commandTag, commandId == 0); // Switch position
    }

    @Override
    public byte getEquipmentId() {
        return 5;
    }

    @Override
    protected void computeNewState() {
        poll();
    }
}
