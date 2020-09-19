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

import eu.dariolucia.reatmetric.api.IReatmetricRegister;
import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class ReatmetricPluginInspector {

    private static final Logger LOG = Logger.getLogger(ReatmetricPluginInspector.class.getName());

    private final Map<String, IReatmetricSystem> serviceFactories = new TreeMap<>();

    public synchronized List<String> getAvailableSystems() {
        if(this.serviceFactories.isEmpty()) {
            ServiceLoader<IReatmetricRegister> loader
                    = ServiceLoader.load(IReatmetricRegister.class);
            for (IReatmetricRegister reg : loader) {
                List<IReatmetricSystem> systems;
                try {
                    systems = reg.availableSystems();
                    for(IReatmetricSystem cp : systems) {
                        String system = cp.getName();
                        if (system != null) {
                            this.serviceFactories.put(system, cp);
                        }
                    }
                } catch (ReatmetricException | RemoteException e) {
                    LOG.log(Level.SEVERE, "Cannot load systems from registry " + reg + ": " + e.getMessage(), e);
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
