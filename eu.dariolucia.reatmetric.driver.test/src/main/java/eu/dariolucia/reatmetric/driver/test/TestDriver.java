/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.util.List;

public class TestDriver implements IDriver {
    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration) throws DriverException {

    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return null;
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return null;
    }

    @Override
    public void dispose() throws DriverException {

    }
}
