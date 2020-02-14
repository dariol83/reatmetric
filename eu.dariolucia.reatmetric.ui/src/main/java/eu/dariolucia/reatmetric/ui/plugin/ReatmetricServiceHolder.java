/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.plugin;

import eu.dariolucia.reatmetric.api.IServiceFactory;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author dario
 */
public class ReatmetricServiceHolder {
    
    private volatile IServiceFactory system;
    
    private final List<IReatmetricServiceListener> listeners = new CopyOnWriteArrayList<IReatmetricServiceListener>();
    
    public synchronized void setSystem(IServiceFactory system) {
        IServiceFactory oldSystem = this.system;
        if(oldSystem != null) {
            this.listeners.forEach(o -> o.systemDisconnected(oldSystem));
            try {
                oldSystem.dispose();
            } catch (ReatmetricException e) {
                // TODO: log
                e.printStackTrace();
            }
        }
        this.system = system;
        if(this.system != null) {
            try {
                this.system.initialise();
            } catch (ReatmetricException e) {
                // TODO: log
                e.printStackTrace();
            }
            this.listeners.forEach(o -> o.systemConnected(this.system));
        }
    }
    
    public IServiceFactory getSystem() {
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
