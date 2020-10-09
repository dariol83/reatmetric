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

package eu.dariolucia.reatmetric.driver.spacecraft.activity.cltu;

import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.IForwardDataUnitStatusSubscriber;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.rmi.RemoteException;
import java.util.List;

public interface ICltuConnector extends ITransportConnector {

    /**
     * Configure the connector after its creation.
     *
     * @param driverName the name of the driver using the connector instance
     * @param configuration the configuration of the driver
     * @param context the service context
     * @param connectorInformation the configuration string provided in the connector configuration
     */
    void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, String connectorInformation) throws RemoteException;

    void register(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException;

    void deregister(IForwardDataUnitStatusSubscriber subscriber) throws RemoteException;

    void sendCltu(byte[] cltu, long externalId) throws RemoteException;

    List<String> getSupportedRoutes() throws RemoteException;
}
