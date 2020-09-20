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
import eu.dariolucia.reatmetric.api.rawdata.IRawDataProvisionService;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;

public class RawDataProvisionServiceProxy extends AbstractProvisionServiceProxy<RawData, RawDataFilter, IRawDataSubscriber, IRawDataProvisionService> implements IRawDataProvisionService {

    public RawDataProvisionServiceProxy(IRawDataProvisionService delegate) {
        super(delegate);
    }

    @Override
    public RawData getRawDataContents(IUniqueId uniqueId) throws ReatmetricException, RemoteException {
        return delegate.getRawDataContents(uniqueId);
    }

    @Override
    public LinkedHashMap<String, String> getRenderedInformation(RawData rawData) throws ReatmetricException, RemoteException {
        return delegate.getRenderedInformation(rawData);
    }
}
