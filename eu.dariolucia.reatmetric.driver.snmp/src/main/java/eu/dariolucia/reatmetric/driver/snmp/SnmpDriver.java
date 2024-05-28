/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.snmp;

import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.core.api.AbstractDriver;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class SnmpDriver extends AbstractDriver implements IRawDataRenderer {

    private static final Logger LOG = Logger.getLogger(SnmpDriver.class.getName());

    public static final String SNMP_MESSAGE_TYPE = "SNMP";

    private SnmpConfiguration configuration;

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

    @Override
    public String getHandler() {
        return null;
    }

    @Override
    public List<String> getSupportedTypes() {
        return null;
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException {
        return null;
    }
}
