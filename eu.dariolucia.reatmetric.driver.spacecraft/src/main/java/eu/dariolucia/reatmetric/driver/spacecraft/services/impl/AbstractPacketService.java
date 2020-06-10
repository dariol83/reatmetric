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

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;

import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class AbstractPacketService<T> implements IService {

    private String serviceConfigurationPath;
    private String driverName;
    private SpacecraftConfiguration spacecraftConfiguration;
    private ServiceCoreConfiguration serviceCoreConfiguration;
    private IServiceCoreContext context;
    private IServiceBroker serviceBroker;
    private T configuration;

    @Override
    public void initialise(String serviceConfigurationPath, String driverName, SpacecraftConfiguration configuration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context, IServiceBroker serviceBroker) throws ReatmetricException {
        this.serviceConfigurationPath = serviceConfigurationPath;
        this.driverName = driverName;
        this.spacecraftConfiguration = configuration;
        this.serviceCoreConfiguration = coreConfiguration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        try {
            this.configuration = loadConfiguration(serviceConfigurationPath);
            postInitialisation();
        } catch (IOException e) {
            throw new ReatmetricException(e);
        }
    }

    protected void postInitialisation() throws ReatmetricException {
        // Sub-classes can extend
    }

    protected abstract T loadConfiguration(String serviceConfigurationPath) throws IOException;

    protected IServiceBroker serviceBroker() {
        return serviceBroker;
    }

    protected SpacecraftConfiguration spacecraftConfiguration() {
        return spacecraftConfiguration;
    }

    protected ServiceCoreConfiguration serviceCoreConfiguration() {
        return serviceCoreConfiguration;
    }

    protected IServiceCoreContext context() {
        return context;
    }

    protected T configuration() {
        return configuration;
    }

    protected IProcessingModel processingModel() {
        return context.getProcessingModel();
    }

    protected String driverName() {
        return driverName;
    }

}
