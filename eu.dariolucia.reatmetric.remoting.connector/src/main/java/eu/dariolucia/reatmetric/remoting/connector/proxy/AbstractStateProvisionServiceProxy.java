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

package eu.dariolucia.reatmetric.remoting.connector.proxy;

import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;

public class AbstractStateProvisionServiceProxy<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>, U extends IDataItemSubscriber<T>, V extends IDataItemStateProvisionService<U, K, T>> extends AbstractProvisionServiceProxy<T, K, U, V> implements IDataItemStateProvisionService<U, K, T> {

    public AbstractStateProvisionServiceProxy(V delegate) {
        super(delegate);
    }

    @Override
    public List<T> retrieve(Instant time, K filter) throws ReatmetricException, RemoteException {
        return delegate.retrieve(time, filter);
    }

}
