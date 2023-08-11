/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket;

import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.core.api.AbstractDriver;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class SocketDriver extends AbstractDriver {

    private static final Logger LOG = Logger.getLogger(SocketDriver.class.getName());

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return null;
    }

    @Override
    protected SystemStatus startProcessing() throws DriverException {
        return null;
    }

    @Override
    protected SystemStatus processConfiguration(String driverConfiguration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context) throws DriverException {
        return null;
    }
}
