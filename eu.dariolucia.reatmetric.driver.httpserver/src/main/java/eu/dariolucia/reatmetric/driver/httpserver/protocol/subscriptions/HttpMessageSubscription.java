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
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpMessageSubscription extends AbstractHttpSubscription<OperationalMessageFilter, OperationalMessage> implements IOperationalMessageSubscriber {

    private static final int MAX_QUEUE_SIZE = 50;

    private static final Logger LOG = Logger.getLogger(HttpMessageSubscription.class.getName());

    private final List<OperationalMessage> data = new LinkedList<>();

    public HttpMessageSubscription(OperationalMessageFilter filter, HttpServerDriver driver) {
        super(filter, driver);
        LOG.log(Level.FINER, "HTTP Operational Message Subscription " + getKey() + " created");
    }

    @Override
    protected void doDeregister() throws ReatmetricException, RemoteException {
        getDriver().deregister(this);
    }

    @Override
    public List<OperationalMessage> getUpdates() {
        access();
        List<OperationalMessage> toReturn;
        synchronized (this.data) {
            toReturn = new ArrayList<>(this.data);
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
    public void dataItemsReceived(List<OperationalMessage> dataItems) {
        if(!isInitialised()) {
            LOG.log(Level.FINER, "HTTP Operational Message Subscription " + getKey() + ": callback received but not initialised, ignoring");
            // Ignore late updates
            return;
        }
        synchronized (this.data) {
            // Add the data
            this.data.addAll(dataItems);
            // Remove data if exceeds
            if(this.data.size() > MAX_QUEUE_SIZE) {
                this.data.subList(0, this.data.size() - MAX_QUEUE_SIZE).clear();
            }
        }
    }
}
