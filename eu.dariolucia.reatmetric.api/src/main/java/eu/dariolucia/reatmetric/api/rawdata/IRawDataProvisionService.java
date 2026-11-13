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


package eu.dariolucia.reatmetric.api.rawdata;

import eu.dariolucia.reatmetric.api.common.IDataItemProvisionService;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service interface for provisioning raw data items.
 */
public interface IRawDataProvisionService extends IDataItemProvisionService<IRawDataSubscriber, RawDataFilter, RawData> {

    /**
     * Retrieves the raw data contents for the specified unique ID.
     *
     * @param uniqueId the unique identifier of the raw data
     * @return the raw data contents
     * @throws ReatmetricException if a Reatmetric error occurs
     * @throws RemoteException if a remote communication error occurs
     */
    RawData getRawDataContents(IUniqueId uniqueId) throws ReatmetricException, RemoteException;

    /**
     * Retrieves rendered information for the specified raw data.
     *
     * @param rawData the raw data to render
     * @return a map of rendered information key-value pairs
     * @throws ReatmetricException if a Reatmetric error occurs
     * @throws RemoteException if a remote communication error occurs
     */
    LinkedHashMap<String, String> getRenderedInformation(RawData rawData) throws ReatmetricException, RemoteException;

}
