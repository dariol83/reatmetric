/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.remoting.connector.proxy;

import eu.dariolucia.reatmetric.api.common.Pair;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This activation cache is to keep track of RMI object activation, to avoid multiple activation exceptions.
 */
public final class ObjectActivationCache {

    private static final String JVM_PROPERTY_RMI_EXPORT_PORT_KEY = "reatmetric.remoting.connector.export.port";
    private static final int RMI_EXPORT_PORT_DEFAULT = 0;

    private static final Logger LOG = Logger.getLogger(ObjectActivationCache.class.getName());

    private static final ObjectActivationCache INSTANCE = new ObjectActivationCache();

    static ObjectActivationCache instance() {
        return INSTANCE;
    }

    private final Map<Object, Pair<Remote, AtomicInteger>> cache = new HashMap<>();

    private int exportPort;

    private ObjectActivationCache() {
        String rmiExportPortStr = System.getProperty(JVM_PROPERTY_RMI_EXPORT_PORT_KEY);
        this.exportPort = RMI_EXPORT_PORT_DEFAULT;
        try {
            if(rmiExportPortStr != null) {
                this.exportPort = Integer.parseInt(rmiExportPortStr);
            }
        } catch (Exception e) {
            LOG.severe("Cannot initialise RMI connector export port from system property " + JVM_PROPERTY_RMI_EXPORT_PORT_KEY + ": " + rmiExportPortStr + ": " + e.getMessage());
        }
    }

    synchronized Remote activate(Remote o) throws RemoteException {
        LOG.log(Level.INFO, "Requesting export of object " + o);
        Pair<Remote, AtomicInteger> item = cache.get(o);
        if(item == null) {
            // First activation
            item = Pair.of(UnicastRemoteObject.exportObject(o, this.exportPort), new AtomicInteger(0));
            LOG.log(Level.INFO, "Object activated: " + item.getFirst());
            cache.put(o, item);
        }
        item.getSecond().incrementAndGet();
        return item.getFirst();
    }

    synchronized void deactivate(Remote o, boolean force) throws NoSuchObjectException {
        Pair<Remote, AtomicInteger> item = cache.get(o);
        if(item == null) {
            // Weird: log warning and return
            LOG.log(Level.WARNING, "Unexpected deactivation of object " + o + ", ignored");
            return;
        }
        // First activation
        int references = item.getSecond().decrementAndGet();
        if(references == 0) {
            UnicastRemoteObject.unexportObject(o, force);
            cache.remove(o);
        }
    }

}
