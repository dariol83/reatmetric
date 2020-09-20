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

import java.rmi.RemoteException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
    
    private final List<IReatmetricServiceListener> listeners = new CopyOnWriteArrayList<>();

    private final Timer aliveTimer = new Timer("ReatMetric UI System Alive Checker", true);
    private volatile TimerTask aliveChecker;
    
    public synchronized void setSystem(IReatmetricSystem system) {
        this.listeners.forEach(IReatmetricServiceListener::startGlobalOperationProgress);
        // Stop time checker
        stopTimer();
        // If there is an old system, dispose it
        IReatmetricSystem oldSystem = this.system;
        if(oldSystem != null) {
            this.listeners.forEach(o -> o.systemDisconnected(oldSystem));
            try {
                // Given the amount of concurrency, wait some time before disposing the system... I know, not the best.
                Thread.sleep(1000);
                oldSystem.dispose();
            } catch (ReatmetricException | InterruptedException | RemoteException e) {
                LOG.log(Level.WARNING, "Exception while disposing system: " + e.getMessage(), e);
            }
        }
        this.system = system;
        this.listeners.forEach(o -> o.systemStatusUpdate(SystemStatus.UNKNOWN));
        if(this.system != null) {
            try {
                this.system.initialise(this::statusUpdateFunction);
                this.listeners.forEach(o -> o.systemConnected(this.system));
                // Start time checker
                startTimer();
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Exception while initialising system: " + e.getMessage(), e);
                statusUpdateFunction(SystemStatus.ALARM);
                this.listeners.forEach(o -> o.systemDisconnected(this.system));
            }
        }
        this.listeners.forEach(IReatmetricServiceListener::stopGlobalOperationProgress);
    }

    private synchronized void startTimer() {
        if(aliveChecker == null) {
            aliveChecker = new TimerTask() {
                @Override
                public void run() {
                    IReatmetricSystem theSystem = system;
                    if(theSystem != null && aliveChecker == this) {
                        try {
                            theSystem.getStatus();
                        } catch (RemoteException e) {
                            // System is disconnected
                            LOG.log(Level.SEVERE, "System disconnected");
                            setSystem(null);
                        }
                    }
                }
            };
            aliveTimer.schedule(aliveChecker, 1000, 2000);
        }
    }

    private synchronized void stopTimer() {
        if(aliveChecker != null) {
            aliveChecker.cancel();
            aliveChecker = null;
        }
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

    public boolean isPresent() {
        return this.system != null;
    }
}
