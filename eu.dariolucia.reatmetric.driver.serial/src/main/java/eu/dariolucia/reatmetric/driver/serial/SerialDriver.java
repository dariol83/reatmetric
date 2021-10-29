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

package eu.dariolucia.reatmetric.driver.serial;

import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.serial.definition.SerialConfiguration;
import eu.dariolucia.reatmetric.driver.serial.protocol.IMonitoringDataManager;
import eu.dariolucia.reatmetric.driver.serial.protocol.ProtocolManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Serial driver that can wait for requests on a serial/USB port and provide formatted information of monitoring data
 * based on a low-bandwidth, client-oriented protocol.
 */
public class SerialDriver implements IDriver, IMonitoringDataManager {

    private static final Logger LOG = Logger.getLogger(SerialDriver.class.getName());

    public static final String CONFIGURATION_FILE = "configuration.xml";

    // Driver generic properties
    private String name;
    private IServiceCoreContext context;
    private IDriverListener subscriber;
    // Driver specific properties
    private ProtocolManager protocolManager;
    private SerialConfiguration configuration;

    public SerialDriver() {
        //
    }

    // --------------------------------------------------------------------
    // IDriver methods
    // --------------------------------------------------------------------

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        this.name = name;
        this.context = context;
        this.subscriber = subscriber;

        // Create the protocol manager
        this.protocolManager = new ProtocolManager(this);

        try {
            // Read the configuration
            this.configuration = SerialConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));

            // Start reading from the configured serial port
            // TODO

            // Inform that everything is fine
            subscriber.driverStatusUpdate(this.name, SystemStatus.NOMINAL);
        } catch (Exception e) {
            subscriber.driverStatusUpdate(this.name, SystemStatus.ALARM);
            throw new DriverException(e);
        }
    }

    @Override
    public SystemStatus getDriverStatus() {
        return SystemStatus.NOMINAL;
    }

    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.emptyList();
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.emptyList();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        // Stop reading from the configured serial port
        // TODO

        // Dispose the protocol manager
        this.protocolManager.dispose();
        this.protocolManager = null;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Arrays.asList(
                DebugInformation.of("Serial Driver", "State", this.protocolManager.isRegistered() ? "Connected" : "Waiting", null, null),
                DebugInformation.of("Serial Driver", "Client", Objects.requireNonNullElse(this.protocolManager.getCurrentClient(), "N/A"), null, null)
        );
    }

    // --------------------------------------------------------------------
    // IMonitoringDataManager methods
    // --------------------------------------------------------------------

    @Override
    public int registerParameter(String parameterPath) {
        return 0;
    }

    @Override
    public boolean deregisterParameter(int parameterId) {
        return false;
    }

    @Override
    public void deregisterAllParameter() {

    }

    @Override
    public List<ParameterData> updateParameters() {
        return null;
    }

    @Override
    public List<OperationalMessage> updateLogs() {
        return null;
    }

    // --------------------------------------------------------------------
    // Internal methods
    // --------------------------------------------------------------------
}
