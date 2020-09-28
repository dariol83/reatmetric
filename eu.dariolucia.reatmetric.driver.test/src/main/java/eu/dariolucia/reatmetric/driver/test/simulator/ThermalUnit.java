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

public class ThermalUnit extends StationEquipment {

    private volatile boolean status;
    private volatile int globalStatus;
    private volatile boolean input;

    private volatile int aStatus;
    private volatile int aOverride;
    private volatile double aTemperature;
    private volatile boolean aProtection;

    private volatile int bStatus;
    private volatile int bOverride;
    private volatile double bTemperature;
    private volatile boolean bProtection;

    public ThermalUnit(ScheduledExecutorService scheduler) {
        super(scheduler);
        // Initial state
        status = true;
        globalStatus = 1;
        input = true;

        aStatus = 1;
        aOverride = 1;
        aTemperature = 30.0;
        aProtection = true;

        bStatus = 1;
        bOverride = 1;
        bTemperature = 30.0;
        bProtection = true;
    }

    @Override
    protected void doWriteMonitoringState(DataOutputStream dos) throws IOException {
        dos.writeByte(status ? (byte) 1 : (byte) 0);
        dos.writeByte(globalStatus);
        dos.writeByte(input ? (byte) 1 : (byte) 0);

        dos.writeByte(aStatus);
        dos.writeDouble(aTemperature);
        dos.writeByte(aOverride);
        dos.writeByte(aProtection ? (byte) 1 : (byte) 0);

        dos.writeByte(bStatus);
        dos.writeDouble(bTemperature);
        dos.writeByte(bOverride);
        dos.writeByte(bProtection ? (byte) 1 : (byte) 0);
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
        } else if (commandId == 2) { // Operate on unit status
            int fanId = bb.getInt();
            int switchOn = bb.getInt();
            switch(fanId) {
                case 1: aStatus = switchOn; break;
                case 2: bStatus = switchOn; break;
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
        return (byte) 8;
    }

    @Override
    protected void computeNewState() {
        if(status && input) {
            if(aStatus > 0) {
                aTemperature = aTemperature + 15 * (Math.random() - 0.2);
                if(aProtection && aTemperature > 36) {
                    aOverride = 2;
                    aTemperature = 30;
                } else if(aProtection) {
                    aOverride = 1;
                } else {
                    aOverride = 0;
                }
                if(aTemperature > 36) {
                    aStatus = 2;
                } else {
                    aStatus = 1;
                }
            }
            if(bStatus > 0) {
                bTemperature = bTemperature + 15 * (Math.random() - 0.2);
                if(bProtection && bTemperature > 36) {
                    bOverride = 2;
                    bTemperature = 30;
                } else if(bProtection) {
                    bOverride = 1;
                } else {
                    bOverride = 0;
                }
                if(bTemperature > 36) {
                    bStatus = 2;
                } else {
                    bStatus = 1;
                }
            }
            globalStatus = (aStatus > 0 || bStatus > 0) ? 1 : 0;
            if(aStatus == 0 || aStatus == 2 || bStatus == 0 || bStatus == 2) {
                globalStatus = 2;
            }
        } else {
            aStatus = 0;
            bStatus = 0;
            globalStatus = 0;
        }
        poll();
    }
}
