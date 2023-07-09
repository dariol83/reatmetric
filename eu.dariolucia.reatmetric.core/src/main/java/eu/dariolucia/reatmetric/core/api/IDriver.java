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

package eu.dariolucia.reatmetric.core.api;

import eu.dariolucia.reatmetric.api.common.IDebugInfoProvider;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.DriverConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.util.List;

/**
 * This interface describes the functions of a ReatMetric driver.
 */
public interface IDriver extends IDebugInfoProvider {

    /**
     * Initialise the driver with the provided information. This method is called once per ReatMetric Core instantiation.
     *
     * @param name the name of the driver, as specified in the ReatMetric Core configuration {@link DriverConfiguration#getName()}
     * @param driverConfigurationDirectory the directory containing the configuration of the driver, as specified in the ReatMetric Core configuration {@link DriverConfiguration#getConfiguration()}
     * @param context the context of the ReatMetric Core system
     * @param coreConfiguration the configuration of the ReatMetric Core system
     * @param subscriber the subscriber to the driver state change
     * @throws DriverException in case of issues during the initialisation of the driver
     */
    void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException;

    /**
     * Return the current driver status.
     *
     * @return the current driver status, shall not be null
     */
    SystemStatus getDriverStatus();

    /**
     * Return the list of {@link IRawDataRenderer} that can be used to report detailed information of {@link eu.dariolucia.reatmetric.api.rawdata.RawData}
     * distributed and stored by this driver.
     *
     * @return the list of renderers, shall not be null
     */
    List<IRawDataRenderer> getRawDataRenderers();

    /**
     * Return the list of {@link IActivityHandler} managed by this driver.
     *
     * @return the list of activity handlers, shall not be null
     */
    List<IActivityHandler> getActivityHandlers();

    /**
     * Return the list of {@link ITransportConnector} managed by this driver.
     *
     * @return the list of transport connectors, shall not be null
     */
    List<ITransportConnector> getTransportConnectors();

    /**
     * Dispose the driver and release all resources. This method is called at the shutdown of the ReatMetric Core instance that
     * instantiated the driver.
     *
     * @throws DriverException in case of issues during the disposal of the driver resources
     */
    void dispose() throws DriverException;
}
