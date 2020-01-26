/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.plugin;

import eu.dariolucia.reatmetric.api.IServiceFactory;
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
            this.listeners.forEach(o -> o.systemRemoved(oldSystem));
        }
        this.system = system;
        if(this.system != null) {
            this.listeners.forEach(o -> o.systemAdded(this.system));
        }
    }
    
    public IServiceFactory getSystem() {
        return this.system;
    }
    
    public synchronized void addSubscriber(IReatmetricServiceListener l) {
        this.listeners.add(l);
        if(this.system != null) {
            l.systemAdded(this.system);
        }
    }
    
    public void removeSubscriber(IReatmetricServiceListener l) {
        this.listeners.remove(l);
    }
}
