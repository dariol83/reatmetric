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

package eu.dariolucia.reatmetric.remoting;

import eu.dariolucia.reatmetric.api.IReatmetricSystem;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

/**
 * Class enabling remoting of a {@link IReatmetricSystem} via Java RMI.
 */
public class ReatmetricSystemRemoting {

    private static final Logger LOG = Logger.getLogger(ReatmetricSystemRemoting.class.getName());

    private final IReatmetricSystem system;
    private final int port;
    private final String name;

    private Registry registry;
    private IReatmetricSystem activatedObject;

    public ReatmetricSystemRemoting(int port, String name, IReatmetricSystem system) {
        this.port = port;
        this.name = name;
        this.system = system;
    }

    public ReatmetricSystemRemoting(Registry registry, String name, IReatmetricSystem system) {
        this.port = 0;
        this.name = name;
        this.system = system;
        this.registry = registry;
    }

    public synchronized void activate() throws RemoteException, AlreadyBoundException {
        if(activatedObject != null) {
            throw new IllegalStateException("Object already activated");
        }
        LOG.info("Activating ReatMetric Remoting on port " + this.port + " with name " + this.name);
        if(this.registry == null) {
            if(this.port == 0) {
                throw new IllegalStateException("Port not specified, cannot create registry");
            }
            this.registry = LocateRegistry.createRegistry(this.port);
        }
        this.activatedObject = (IReatmetricSystem) UnicastRemoteObject.exportObject(system, 0);
        this.registry.bind(this.name, this.activatedObject);
    }

    public synchronized void deactivate() throws RemoteException, NotBoundException {
        if(activatedObject == null) {
            throw new IllegalStateException("Object not bound yet");
        }
        LOG.info("Deactivating ReatMetric Remoting on port " + this.port + " with name " + this.name);
        try {
            this.registry.unbind(this.name);
            UnicastRemoteObject.unexportObject(this.activatedObject, true);
        } finally {
            this.activatedObject = null;
        }
    }

    public IReatmetricSystem getSystem() {
        return system;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public synchronized boolean isActive() {
        return this.activatedObject != null && this.registry != null;
    }
}
