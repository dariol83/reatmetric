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

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpParameterStateSubscription extends AbstractHttpSubscription<ParameterDataFilter, ParameterData> implements IParameterDataSubscriber {

    private static final Logger LOG = Logger.getLogger(HttpParameterStateSubscription.class.getName());

    private final Map<String, ParameterData> data = new TreeMap<>();

    public HttpParameterStateSubscription(ParameterDataFilter filter, HttpServerDriver driver) {
        super(filter, driver);
        LOG.log(Level.FINER, "HTTP Parameter State Subscription " + getKey() + " created");
    }

    @Override
    protected void doDeregister() throws ReatmetricException, RemoteException {
        deregister(this);
    }

    @Override
    public List<ParameterData> getUpdates() {
        access();
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
        register(this, getFilter());
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

    private void register(IParameterDataSubscriber sub, ParameterDataFilter filter) throws ReatmetricException, RemoteException {
        getDriver().getContext().getServiceFactory().getParameterDataMonitorService().subscribe(sub, filter);
    }

    private void deregister(IParameterDataSubscriber sub) throws ReatmetricException, RemoteException {
        getDriver().getContext().getServiceFactory().getParameterDataMonitorService().unsubscribe(sub);
    }
}
