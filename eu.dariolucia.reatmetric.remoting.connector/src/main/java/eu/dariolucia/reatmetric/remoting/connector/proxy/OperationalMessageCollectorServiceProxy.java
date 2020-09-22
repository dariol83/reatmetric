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

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageCollectorService;
import eu.dariolucia.reatmetric.api.messages.input.OperationalMessageRequest;

import java.rmi.RemoteException;

public class OperationalMessageCollectorServiceProxy implements IOperationalMessageCollectorService {

    protected final IOperationalMessageCollectorService delegate;

    public OperationalMessageCollectorServiceProxy(IOperationalMessageCollectorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public IUniqueId logMessage(OperationalMessageRequest request) throws ReatmetricException, RemoteException {
        return delegate.logMessage(request);
    }
}
