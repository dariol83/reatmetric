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

package eu.dariolucia.reatmetric.driver.spacecraft.services;

import eu.dariolucia.ccsds.encdec.pus.TmPusHeader;
import eu.dariolucia.ccsds.encdec.structure.DecodingResult;
import eu.dariolucia.ccsds.tmtc.transport.pdu.SpacePacket;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.activity.TcTracker;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Interface for packet service implementations.
 */
public interface IService extends IServicePacketSubscriber {

    /**
     * Return the name of the packet service implementation
     * @return the name of the packet service implementation
     */
    String getName();

    /**
     * Return the filter to be used for subscription to the {@link IServiceBroker}. This method is called after initialise.
     *
     * @return the filter to use to subscribe to the broker
     */
    IServicePacketFilter getSubscriptionFilter();

    /**
     * Initialise the service implementation.
     *
     * @param serviceConfigurationPath absolute path to the service configuration
     * @param driverName name of the driver
     * @param configuration spacecraft driver configuration
     * @param coreConfiguration service core configuration
     * @param context service core context
     * @param serviceBroker service broker
     */
    void initialise(String serviceConfigurationPath,
                    String driverName,
                    SpacecraftConfiguration configuration,
                    ServiceCoreConfiguration coreConfiguration,
                    IServiceCoreContext context,
                    IServiceBroker serviceBroker) throws ReatmetricException;

    /**
     * Return the service type handled by this implementation.
     *
     * @return the service type
     */
    int getServiceType();

    /**
     * Return true if the service implementation will handle the provided tracked TC, otherwise false.
     *
     * @param trackedTc the encoded TC and related invocation
     * @return true if the service implementation will handle the provided tracked TC, otherwise false
     */
    boolean isDirectHandler(TcTracker trackedTc);

    /**
     * Stop and dispose the service implementation
     */
    void dispose();

    @Override
    default void onTcPacket(TcPhase phase, Instant phaseTime, TcTracker tcTracker) {
        // Override if needed
    }

    @Override
    default void onTmPacket(RawData packetRawData, SpacePacket spacePacket, TmPusHeader tmPusHeader, DecodingResult decoded) {
        // Override if needed
    }

    default void registerRawDataRenderers(Map<String, Function<RawData, LinkedHashMap<String, String>>> serviceRenderers) {
        // Override if needed
    }

    /**
     * This method is called to inform implementations that the locate(...) function on the service broker is available and it
     * can be used to look up for implementations.
     */
    default void finaliseServiceLoading() {
        // Override if needed
    }
}
