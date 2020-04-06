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
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class ReatmetricServiceHolder {

    private static final Logger LOG = Logger.getLogger(ReatmetricServiceHolder.class.getName());

    private volatile IReatmetricSystem system;
    
    private final List<IReatmetricServiceListener> listeners = new CopyOnWriteArrayList<IReatmetricServiceListener>();
    
    public synchronized void setSystem(IReatmetricSystem system) {
        this.listeners.forEach(IReatmetricServiceListener::startGlobalOperationProgress);
        IReatmetricSystem oldSystem = this.system;
        if(oldSystem != null) {
            this.listeners.forEach(o -> o.systemDisconnected(oldSystem));
            try {
                oldSystem.dispose();
            } catch (ReatmetricException e) {
                LOG.log(Level.WARNING, "Exception while disposing system " + oldSystem.getName() + ": " + e.getMessage(), e);
            }
        }
        this.system = system;
        this.listeners.forEach(o -> o.systemStatusUpdate(SystemStatus.UNKNOWN));
        if(this.system != null) {
            try {
                this.system.initialise(this::statusUpdateFunction);
                this.listeners.forEach(o -> o.systemConnected(this.system));
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Exception while initialising system " + this.system.getName() + ": " + e.getMessage(), e);
            }
        }
        this.listeners.forEach(IReatmetricServiceListener::stopGlobalOperationProgress);
    }

    private void statusUpdateFunction(SystemStatus systemStatus) {
        this.listeners.forEach(o -> o.systemStatusUpdate(systemStatus));
    }

    public IReatmetricSystem getSystem() {
        return this.system;
    }
    
    public synchronized void addSubscriber(IReatmetricServiceListener l) {
        this.listeners.add(l);
        if(this.system != null) {
            l.systemConnected(this.system);
        }
    }
    
    public void removeSubscriber(IReatmetricServiceListener l) {
        this.listeners.remove(l);
    }
}
