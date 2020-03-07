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
import eu.dariolucia.reatmetric.ui.controller.ConnectorsBrowserViewController;

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

    private volatile IServiceFactory system;
    
    private final List<IReatmetricServiceListener> listeners = new CopyOnWriteArrayList<IReatmetricServiceListener>();
    
    public synchronized void setSystem(IServiceFactory system) {
        this.listeners.forEach(IReatmetricServiceListener::startGlobalOperationProgress);
        IServiceFactory oldSystem = this.system;
        if(oldSystem != null) {
            this.listeners.forEach(o -> o.systemDisconnected(oldSystem));
            try {
                oldSystem.dispose();
            } catch (ReatmetricException e) {
                LOG.log(Level.WARNING, "Exception while disposing system " + oldSystem.getSystem() + ": " + e.getMessage(), e);
            }
        }
        this.system = system;
        if(this.system != null) {
            try {
                this.system.initialise();
                this.listeners.forEach(o -> o.systemConnected(this.system));
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Exception while initialising system " + this.system.getSystem() + ": " + e.getMessage(), e);
            }
        }
        this.listeners.forEach(IReatmetricServiceListener::stopGlobalOperationProgress);
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
