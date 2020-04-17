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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StationSimulator {

    private final ScheduledExecutorService scheduler;
    private final Map<Integer, StationEquipment> equipments = new HashMap<>();

    public StationSimulator() {
        scheduler = Executors.newScheduledThreadPool(1);
        equipments.put(1, new GenericPowerSupply(scheduler, 1, 220, 15));
        equipments.put(2, new GenericPowerSupply(scheduler, 2, 220, 15));
        equipments.put(3, new GenericPowerSupply(scheduler, 3, 220, 15));
        equipments.put(4, new Matrix(scheduler));
        equipments.put(5, new Switch(scheduler));
        equipments.put(6, new Splitter(scheduler));
    }

    public void connect(IStationMonitor monitor) {
        equipments.values().forEach(o -> o.connect(monitor));
    }

    public void disconnect() {
        equipments.values().forEach(StationEquipment::disconnect);
    }

    public void poll() {
        equipments.values().forEach(StationEquipment::poll);
    }

    public void execute(byte[] data) {
        scheduler.execute(() -> {
            ByteBuffer bb = ByteBuffer.wrap(data);
            byte firstByte = bb.get();
            int eqId = firstByte >>> 4;
            forward(data, eqId);
        });
    }

    private void forward(byte[] data, int equipmentId) {
        StationEquipment se = equipments.get(equipmentId);
        if(se != null) {
            se.execute(data);
        }
    }
}
