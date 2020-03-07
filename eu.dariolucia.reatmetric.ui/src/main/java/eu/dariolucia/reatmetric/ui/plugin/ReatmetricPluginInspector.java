/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
