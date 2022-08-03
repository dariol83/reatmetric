package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractSubscriptionHandler<T extends AbstractDataItemFilter, K extends AbstractDataItem> {

    private static final Logger LOG = Logger.getLogger(AbstractSubscriptionHandler.class.getName());

    private final T filter;
    private final HttpServerDriver driver;
    private final UUID key;

    private volatile boolean initialised = false;

    public AbstractSubscriptionHandler(T filter, HttpServerDriver driver) {
        this.filter = filter;
        this.driver = driver;
        this.key = UUID.randomUUID();
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

    final public UUID getKey() {
        return key;
    }

    final protected HttpServerDriver getDriver() {
        return driver;
    }

    final protected T getFilter() {
        return filter;
    }

    final protected boolean isInitialised() {
        return initialised;
    }
}
