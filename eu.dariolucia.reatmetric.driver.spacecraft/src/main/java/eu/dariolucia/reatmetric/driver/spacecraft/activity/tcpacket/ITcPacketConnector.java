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

package eu.dariolucia.reatmetric.driver.spacecraft.activity.tcpacket;

import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;

import java.util.List;

public interface ITcPacketConnector extends ITransportConnector {

    /**
     * Configure the connector after its creation.
     *
     * @param driverName the name of the driver using the connector instance
     * @param configuration the configuration of the driver
     * @param context the service context
     * @param serviceBroker the service broker
     * @param connectorInformation the configuration string provided in the connector configuration
     */
    void configure(String driverName, SpacecraftConfiguration configuration, IServiceCoreContext context, IServiceBroker serviceBroker, String connectorInformation);

    void sendTcPacket(SpacePacket sp, TcTracker tcTracker);

    List<String> getSupportedRoutes();
}
