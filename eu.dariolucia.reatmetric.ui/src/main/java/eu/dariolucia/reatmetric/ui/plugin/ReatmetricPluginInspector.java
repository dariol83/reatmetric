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


package eu.dariolucia.reatmetric.ui.plugin;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;

import java.util.*;

/**
 *
 * @author dario
 */
public class ReatmetricPluginInspector {

    private final Map<String, IReatmetricSystem> serviceFactories = new TreeMap<>();

    public synchronized List<String> getAvailableSystems() {
        if(this.serviceFactories.isEmpty()) {
            ServiceLoader<IReatmetricSystem> loader
                    = ServiceLoader.load(IReatmetricSystem.class);
            for (IReatmetricSystem cp : loader) {
                String system = cp.getName();
                if (system != null) {
                    this.serviceFactories.put(system, cp);
                }
            }
        }
        //
        return new ArrayList<>(this.serviceFactories.keySet());
    }
    
    public synchronized IReatmetricSystem getSystem(String system) {
        return this.serviceFactories.get(system);
    }
}
