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

import eu.dariolucia.reatmetric.api.archive.IArchive;
import eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.configuration.AbstractInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ResumeInitialisationConfiguration;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.core.configuration.TimeInitialisationConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IService;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;

import java.io.IOException;
import java.time.Instant;
import java.util.ServiceLoader;

public abstract class AbstractPacketService<T> implements IService {

    private String driverName;
    private SpacecraftConfiguration spacecraftConfiguration;
    private ServiceCoreConfiguration serviceCoreConfiguration;
    private IServiceCoreContext context;
    private IServiceBroker serviceBroker;
    private T configuration;

    @Override
    public void initialise(String serviceConfigurationPath, String driverName, SpacecraftConfiguration configuration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context, IServiceBroker serviceBroker) throws ReatmetricException {
        this.driverName = driverName;
        this.spacecraftConfiguration = configuration;
        this.serviceCoreConfiguration = coreConfiguration;
        this.context = context;
        this.serviceBroker = serviceBroker;
        try {
            this.configuration = loadConfiguration(serviceConfigurationPath);
            // Initialise from archive?
            if(this.serviceCoreConfiguration.getInitialisation() != null) {
                initialiseModel(this.serviceCoreConfiguration.getInitialisation());
            }
        } catch (IOException e) {
            throw new ReatmetricException(e);
        }
        postInitialisation();
    }

    protected void initialiseModel(AbstractInitialisationConfiguration initialisation) throws ReatmetricException {
        if(initialisation instanceof TimeInitialisationConfiguration) {
            // Get the time coefficient with generation time at the specified one from the reference archive
            String location = ((TimeInitialisationConfiguration) initialisation).getArchiveLocation();
            Instant time = ((TimeInitialisationConfiguration) initialisation).getTime().toInstant();
            if(location == null) {
                // No archive location -> use current archive
                initialiseModelFrom(context().getArchive(), time);
            } else {
                // Archive location -> use external archive
                initialiseFromExternalArchive(location, time);
            }
        } else if(initialisation instanceof ResumeInitialisationConfiguration) {
            // Get the latest state information in the raw data broker
            Instant latestGenerationTime = context().getArchive().getArchive(IRawDataArchive.class).retrieveLastGenerationTime();
            // If latestGenerationTime is null, it means that the archive is empty for this data type
            if(latestGenerationTime != null) {
                initialiseModelFrom(context().getArchive(), latestGenerationTime);
            }
        } else {
            throw new IllegalArgumentException("Initialisation configuration for onboard scheduling service not supported: " + initialisation.getClass().getName());
        }
    }

    private void initialiseFromExternalArchive(String location, Instant time) throws ReatmetricException {
        ServiceLoader<IArchiveFactory> archiveLoader = ServiceLoader.load(IArchiveFactory.class);
        if (archiveLoader.findFirst().isPresent()) {
            IArchive externalArchive = archiveLoader.findFirst().get().buildArchive(location);
            externalArchive.connect();
            initialiseModelFrom(externalArchive, time);
            externalArchive.dispose();
        } else {
            throw new ReatmetricException("Initialisation archive configured to " + location + ", but no archive factory deployed");
        }
    }

    protected abstract void initialiseModelFrom(IArchive externalArchive, Instant time) throws ReatmetricException;

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
