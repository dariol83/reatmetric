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


package eu.dariolucia.reatmetric.api.parameters;

import eu.dariolucia.reatmetric.api.common.IDataItemStateProvisionService;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.rmi.RemoteException;

/**
 * Provision service interface for parameter data.
 * This interface extends the data item state provision service to provide access to parameter data
 * and parameter descriptors.
 *
 * @author dario
 */
public interface IParameterDataProvisionService extends IDataItemStateProvisionService<IParameterDataSubscriber, ParameterDataFilter, ParameterData> {

    /**
     * Returns the parameter descriptor for the parameter at the specified path.
     *
     * @param path the system entity path of the parameter, cannot be null
     * @return the parameter descriptor
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    ParameterDescriptor getDescriptor(SystemEntityPath path) throws ReatmetricException, RemoteException;

    /**
     * Returns the parameter descriptor for the parameter with the specified external ID.
     *
     * @param externalId the external ID of the parameter
     * @return the parameter descriptor
     * @throws ReatmetricException if a problem arises with the retrieval operation
     * @throws RemoteException in case of remoting problem
     */
    ParameterDescriptor getDescriptor(int externalId) throws ReatmetricException, RemoteException;
}
