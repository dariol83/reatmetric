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
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.AbstractDriver;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.SocketConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.protocol.IDataProcessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SocketDriver extends AbstractDriver implements IDataProcessor {

    private static final Logger LOG = Logger.getLogger(SocketDriver.class.getName());
    private SocketConfiguration configuration;
    private SocketDriverConnector connector;
    private SocketActivityHandler activityHandler;

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return null;
    }

    @Override
    protected SystemStatus startProcessing() {
        // Create the transport connector
        createTransportConnector();
        // Create the activity handler
        createActivityHandler();
        // Forward the data processor interface for parameters/events forwarding and for activity verification and
        // for raw data, to be set on RouteConfiguration objects
        configuration.getConnections().forEach(o -> o.getRoute().setDataProcessor(this));
        // Done
        return SystemStatus.NOMINAL;
    }

    private void createActivityHandler() {
        this.activityHandler = new SocketActivityHandler(this.configuration, this.connector);
    }

    private void createTransportConnector() {
        this.connector = new SocketDriverConnector(this.configuration);
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.singletonList(this.connector);
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this.activityHandler);
    }

    @Override
    protected SystemStatus processConfiguration(String driverConfiguration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context) throws DriverException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info(String.format("Loading driver configuration at %s", driverConfiguration));
        }
        try {
            this.configuration = SocketConfiguration.load(new FileInputStream(driverConfiguration));
            return SystemStatus.NOMINAL;
        } catch (IOException e) {
            throw new DriverException(e);
        }
    }

    @Override
    public void dispose() throws DriverException {
        super.dispose();
        // TODO
    }

    @Override
    public void forwardRawData(RawData data) {
        try {
            getContext().getRawDataBroker().distribute(Collections.singletonList(data), true);
        } catch (ReatmetricException e) {
            LOG.log(Level.WARNING, "Cannot store raw data " + data + " : " + e.getMessage(), e);
        }
    }

    @Override
    public void forwardParameters(List<ParameterSample> samples) {

    }

    @Override
    public void forwardEvents(List<EventOccurrence> events) {

    }

    @Override
    public void forwardActivityProgress(ActivityProgress progressReport) {

    }

    @Override
    public IUniqueId getNextRawDataId() {
        return getContext().getRawDataBroker().nextRawDataId();
    }
}
