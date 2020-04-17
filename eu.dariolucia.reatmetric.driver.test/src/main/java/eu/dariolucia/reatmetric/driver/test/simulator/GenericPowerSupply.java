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

public class GenericPowerSupply extends StationEquipment {

    private volatile boolean status;
    private volatile double tension;
    private volatile double current;
    private volatile int protection;
    private volatile boolean output;

    private final int equipmentId;
    private final double targetTension;
    private final double targetCurrent;

    public GenericPowerSupply(ScheduledExecutorService scheduler, int equipmentId, double targetTension, double targetCurrent) {
        super(scheduler);
        // Initial state
        status = true;
        tension = 0.0;
        current = 0.0;
        protection = 1;
        output = true;

        this.targetCurrent = targetCurrent;
        this.targetTension = targetTension;
        this.equipmentId = equipmentId;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeDouble(tension);
        dos.writeDouble(current);
        dos.writeByte(protection);
        dos.writeByte(output ? (byte) 1 : (byte) 0);
    }

    @Override
    public void doExecute(byte[] command) {
        // TODO: implement
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
        return (byte) equipmentId;
    }

    @Override
    protected void computeNewState() {
        if(status) {
            tension = targetTension + 30 * (Math.random() - 0.5);
            if(output) {
                current = targetCurrent + 10 * (Math.random() - 0.5);
            } else {
                current = 0.0;
            }
        } else {
            tension = 0.0;
            current = 0.0;
        }
        poll();
    }
}
