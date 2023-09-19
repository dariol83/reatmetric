/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket;

import java.util.LinkedHashMap;
import java.util.Map;

public class Device {

    private final Map<String, DeviceSubsystem> id2subsystem = new LinkedHashMap<>();
    private final String name;

    public Device(String name) {
        this.name = name;
    }

    public DeviceSubsystem createSubsystem(String name) {
        DeviceSubsystem ds = new DeviceSubsystem(name);
        id2subsystem.put(name, ds);
        return ds;
    }

    public DeviceSubsystem getSubsystem(String name) {
        return id2subsystem.get(name);
    }

    public void dispose() {
        this.id2subsystem.values().forEach(DeviceSubsystem::dispose);
    }
}
