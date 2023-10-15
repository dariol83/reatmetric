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
import eu.dariolucia.reatmetric.driver.socket.configuration.connection.AbstractConnectionConfiguration;
import eu.dariolucia.reatmetric.driver.socket.configuration.protocol.IDataProcessor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
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
    private Timer globalDriverTimer;
    private ExecutorService actionThreadPool;

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Collections.emptyList();
    }

    @Override
    protected SystemStatus startProcessing() {
        // Init data
        this.globalDriverTimer = new Timer(getName() + " Timer Service");
        this.actionThreadPool = Executors.newSingleThreadExecutor((r) -> {
           Thread t = new Thread(r);
           t.setDaemon(true);
           t.setName(getName() + " Worker Thread");
           return t;
        });
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

    @Override
    public String getHandlerName() {
        return getName();
    }

    private void createActivityHandler() {
        this.activityHandler = new SocketActivityHandler(this.configuration, this.connector);
    }

    private void createTransportConnector() {
        this.connector = new SocketDriverConnector(this.configuration);
        this.connector.prepare();
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
    public void dispose() {
        this.configuration.getConnections().forEach(AbstractConnectionConfiguration::dispose);
        this.globalDriverTimer.cancel();
        this.actionThreadPool.shutdown();
        try {
            this.actionThreadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing
        }
        this.globalDriverTimer = null;
        this.actionThreadPool = null;
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
        if(samples != null && !samples.isEmpty()) {
            getContext().getProcessingModel().injectParameters(samples);
        }
    }

    @Override
    public void forwardEvents(List<EventOccurrence> events) {
        if(events != null && !events.isEmpty()) {
            for (EventOccurrence eo : events) {
                getContext().getProcessingModel().raiseEvent(eo);
            }
        }
    }

    @Override
    public void forwardActivityProgress(ActivityProgress progressReport) {
        getContext().getProcessingModel().reportActivityProgress(progressReport);
    }

    @Override
    public IUniqueId getNextRawDataId() {
        return getContext().getRawDataBroker().nextRawDataId();
    }

    @Override
    public Timer getTimerService() {
        return this.globalDriverTimer;
    }

    @Override
    public <V> Future<V> execute(Callable<V> task) {
        return this.actionThreadPool.submit(task);
    }

    @Override
    public Future<?> execute(Runnable task) {
        return this.actionThreadPool.submit(task);
    }
}
