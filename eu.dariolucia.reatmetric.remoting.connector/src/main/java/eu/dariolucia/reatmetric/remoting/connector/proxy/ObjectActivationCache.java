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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This activation cache is to keep track of RMI object activation, to avoid multiple activation exceptions.
 */
public final class ObjectActivationCache {

    private static final ObjectActivationCache INSTANCE = new ObjectActivationCache();

    static ObjectActivationCache instance() {
        return INSTANCE;
    }

    private final Map<Object, Pair<Remote, AtomicInteger>> cache = new HashMap<>();

    synchronized Remote activate(Remote o, int port) throws RemoteException {
        Pair<Remote, AtomicInteger> item = cache.get(o);
        if(item == null) {
            // First activation
            item = Pair.of(UnicastRemoteObject.exportObject(o, port), new AtomicInteger(0));
            cache.put(o, item);
        }
        item.getSecond().incrementAndGet();
        return item.getFirst();
    }

    synchronized void deactivate(Remote o, boolean force) throws NoSuchObjectException {
        Pair<Remote, AtomicInteger> item = cache.get(o);
        if(item == null) {
            // Weird: log warning and return
            // TODO
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
