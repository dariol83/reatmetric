/*
 * Copyright (c)  2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractHttpSubscription<T extends AbstractDataItemFilter, K extends AbstractDataItem> {

    private static final Logger LOG = Logger.getLogger(AbstractHttpSubscription.class.getName());

    private final T filter;
    private final HttpServerDriver driver;
    private final UUID key;

    private volatile boolean initialised = false;

    private volatile Instant lastAccess;

    public AbstractHttpSubscription(T filter, HttpServerDriver driver) {
        this.filter = filter;
        this.driver = driver;
        this.key = UUID.randomUUID();
        this.lastAccess = Instant.now();
    }

    public void dispose() {
        if(!this.initialised) {
            throw new IllegalStateException("Not initialised"); // This is a bug
        }
        try {
            doDeregister();
        } catch (RemoteException | ReatmetricException e) {
            LOG.log(Level.FINE, "HTTP Subscription " + this.key + " dispose() failed", e);
        }
        this.initialised = false;
    }

    protected abstract void doDeregister() throws RemoteException, ReatmetricException;

    public abstract List<K> getUpdates();

    public boolean initialise() {
        if(this.initialised) {
            throw new IllegalStateException("Already initialised"); // This is a bug
        }
        try {
            this.initialised = true; // This is here to have a change to get the first callback
            doRegister();
        } catch (RemoteException | ReatmetricException e) {
            // log
            LOG.log(Level.FINE, "HTTP Subscription " + this.key + " initialise() failed", e);
            this.initialised = false;
        }
        return this.initialised;
    }

    protected abstract void doRegister() throws RemoteException, ReatmetricException;

    public final UUID getKey() {
        return key;
    }

    protected final HttpServerDriver getDriver() {
        return driver;
    }

    protected final T getFilter() {
        return filter;
    }

    protected final boolean isInitialised() {
        return initialised;
    }

    public final Instant getLastAccess() {
        return lastAccess;
    }

    protected final void access() {
        lastAccess = Instant.now();
    }
}
