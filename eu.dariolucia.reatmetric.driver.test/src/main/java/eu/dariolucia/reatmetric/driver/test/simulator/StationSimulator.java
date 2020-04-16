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

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StationSimulator {

    private final ScheduledExecutorService scheduler;
    private final PowerSupply powerSupply;

    private volatile IStationMonitor monitor;

    public StationSimulator() {
        scheduler = Executors.newScheduledThreadPool(1);
        powerSupply = new PowerSupply(scheduler);
    }

    public void connect(IStationMonitor monitor) {
        this.powerSupply.connect(monitor);
    }

    public void disconnect() {
        this.powerSupply.disconnect();
    }

    public void poll() {
        this.powerSupply.poll();
    }

    public void execute(byte[] data) {
        scheduler.execute(() -> {
            ByteBuffer bb = ByteBuffer.wrap(data);
            byte firstByte = bb.get();
            int eqId = firstByte >>> 4;
            forward(data, 4);
        });
    }

    private void forward(byte[] data, int equipmentId) {
        if(equipmentId == powerSupply.getEquipmentId()) {
            powerSupply.execute(data);
        }  // else silent fail
    }

}
