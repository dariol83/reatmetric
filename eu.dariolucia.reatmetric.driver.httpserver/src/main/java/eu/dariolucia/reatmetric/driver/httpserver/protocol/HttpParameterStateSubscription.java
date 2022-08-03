package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpParameterStateSubscription extends AbstractSubscriptionHandler<ParameterDataFilter, ParameterData> implements IParameterDataSubscriber {

    private static final Logger LOG = Logger.getLogger(HttpParameterStateSubscription.class.getName());

    private final Map<String, ParameterData> data = new TreeMap<>();

    HttpParameterStateSubscription(ParameterDataFilter filter, HttpServerDriver driver) {
        super(filter, driver);
        LOG.log(Level.FINER, "HTTP Parameter State Subscription " + getKey() + " created");
    }

    @Override
    protected void doDeregister() throws ReatmetricException, RemoteException {
        getDriver().deregister(this);
    }

    @Override
    public List<ParameterData> getUpdates() {
        List<ParameterData> toReturn;
        synchronized (this.data) {
            toReturn = new ArrayList<>(this.data.values());
            // Remove the data here
            this.data.clear();
        }
        return toReturn;
    }

    @Override
    protected void doRegister() throws ReatmetricException, RemoteException {
        getDriver().register(this, getFilter());
    }

    @Override
    public void dataItemsReceived(List<ParameterData> dataItems) {
        if(!isInitialised()) {
            LOG.log(Level.FINER, "HTTP Parameter State Subscription " + getKey() + ": callback received but not initialised, ignoring");
            // Ignore late updates
            return;
        }
        synchronized (this.data) {
            // Override/add the data
            dataItems.forEach(o -> this.data.put(o.getPath().toString(), o));
        }
    }
}
