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

package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.input.OperationalMessageRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface is used to request the storage of operational messages.
 */
public interface IOperationalMessageCollectorService extends Remote {

    /**
     * Request the storage and distribution of the specified message.
     *
     * @param request the message request
     * @return the ID of the operational message created by this request
     * @throws ReatmetricException in case of problems during the creation of the message
     */
    IUniqueId logMessage(OperationalMessageRequest request) throws ReatmetricException, RemoteException;

}
