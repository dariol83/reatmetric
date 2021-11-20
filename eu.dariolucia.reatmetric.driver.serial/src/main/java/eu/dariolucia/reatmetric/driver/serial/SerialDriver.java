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

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageSubscriber;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
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
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
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
    private IDriverListener driverSubscriber;
    private SystemStatus driverStatus;
    // Driver specific properties
    private ProtocolManager protocolManager;
    private SerialConfiguration configuration;
    // IMonitoringDataManager implementation required fields - Parameters
    private final AtomicInteger idSequencer = new AtomicInteger(0);
    private final List<Integer> freedIds = new LinkedList<>();
    private final Map<Integer, SystemEntityPath> registeredParameter = new LinkedHashMap<>();
    private final Map<SystemEntityPath, ParameterData> registeredParameterCache = new LinkedHashMap<>();
    private final Lock parameterLock = new ReentrantLock();
    private final IParameterDataSubscriber parameterDataSubscriber = this::parameterDataItemReceived;
    // IMonitoringDataManager implementation required fields - Operational messages
    private final List<OperationalMessage> latestLogs = new ArrayList<>(100);
    private final Lock logLock = new ReentrantLock();
    private final IOperationalMessageSubscriber messageDataSubscriber = this::operationalMessageDataItemReceived;
    // Serial interface required fields
    private volatile boolean serialReadingActive = false;
    private final Thread serialReaderThread = new Thread(this::manageSerialDevice, "ReatMetric - Serial Device Reader");

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
        this.driverStatus = SystemStatus.NOMINAL;
        this.driverSubscriber = subscriber;
        // Create the protocol manager
        this.protocolManager = new ProtocolManager(this);

        try {
            // Read the configuration
            this.configuration = SerialConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));

            // Start reading from the configured serial port
            startReadingFromSerial();

            // Subscribe to operational messages
            subscribeToMessages();

            // Inform that everything is fine
            this.driverStatus = SystemStatus.NOMINAL;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
        } catch (Exception e) {
            this.driverStatus = SystemStatus.ALARM;
            subscriber.driverStatusUpdate(this.name, this.driverStatus);
            throw new DriverException(e);
        }
    }

    private void startReadingFromSerial() {
        this.serialReadingActive = true;
        this.serialReaderThread.start();
    }

    private void stopReadingFromSerial() {
        this.serialReadingActive = false;
        try {
            this.serialReaderThread.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    private void manageSerialDevice() {
        // This method executes inside the thread serialReaderThread
        // First, let's dump the serial port devices in the system
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort sp : ports) {
            LOG.log(Level.INFO, "Serial port device detected: " + sp, new Object[]{ this.name });
        }
        // Now open the selected one
        SerialPort comPort;
        while(this.serialReadingActive) {
            // Open the interface
            try {
                comPort = SerialPort.getCommPort(configuration.getDevice());
            } catch (SerialPortInvalidPortException e) {
                // If the serial port is invalid, no way this can work: SEVERE message, driver status and bail out
                LOG.log(Level.SEVERE, "Error while opening serial device " + configuration.getDevice() + ": " + e.getMessage(), new Object[]{ this.name });
                this.driverStatus = SystemStatus.WARNING;
                this.driverSubscriber.driverStatusUpdate(this.name, this.driverStatus);
                this.serialReadingActive = false;
                break;
            }

            // If you are here, you have a com port matching, then open it
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, this.configuration.getTimeout() * 1000, 0);
            // TODO configure port settings from configuration: baud rate, parity, stop, data bits, RTS, etc
            comPort.openPort();
            // Now try to read until \r\n (0x0D0A), send the data to the protocol manager and write back the response
            StringBuilder readString = new StringBuilder();
            boolean carriageReturnEncountered = false;
            while(this.serialReadingActive) {
                try {
                    // This buffer is large enough to store even the longest request message (parameter path > 1000 chars)
                    // and since the request is only one line and then a response must be received otherwise the protocol
                    // is violated, this is good enough
                    byte[] readBuffer = new byte[1024];

                    int numRead = comPort.readBytes(readBuffer, readBuffer.length);
                    if (numRead == -1) {
                        Thread.sleep(1000); // Sleep one second and retry
                    } else {
                        for (int i = 0; i < numRead; ++i) {
                            byte charRead = readBuffer[i];
                            // Append the character
                            readString.append((char) charRead);
                            if (charRead == 0x0D) { // If it is a \r, flag it as encountered
                                carriageReturnEncountered = true;
                            } else if (carriageReturnEncountered && charRead == 0x0A) { // If it is a \n and preceded by \r, done
                                // String found and complete, process it
                                String command = readString.toString();
                                // Reset the reading
                                carriageReturnEncountered = false;
                                readString = new StringBuilder();
                                // Process command
                                byte[] toSend = this.protocolManager.event(command);
                                // Send bytes
                                comPort.writeBytes(toSend, toSend.length);
                            } else { // It is a character (ok) or a \n not preceded by a \r (weird...)
                                carriageReturnEncountered = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    // An exception here means the reading had a problem: break the inner loop, close the com port
                    LOG.log(Level.SEVERE, "Error in reading/writing from device " + comPort + ": " + e.getMessage(), new Object[]{ this.name });
                    break;
                }
            }
            comPort.closePort();
        }
        LOG.log(Level.INFO, "Serial driver " + this.name + " handling thread terminated", new Object[]{ this.name });
    }

    @Override
    public SystemStatus getDriverStatus() {
        return this.driverStatus;
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
        stopReadingFromSerial();

        // Unsubscribe from everything
        deregisterAllParameter();
        unsubscribeToMessages();

        // Dispose the protocol manager
        this.protocolManager.dispose();
        this.protocolManager = null;
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Arrays.asList(
                DebugInformation.of(this.name, "State", this.protocolManager.isRegistered() ? "Connected" : "Waiting", null, null),
                DebugInformation.of(this.name, "Client", Objects.requireNonNullElse(this.protocolManager.getCurrentClient(), "N/A"), null, null)
        );
    }

    // --------------------------------------------------------------------
    // IMonitoringDataManager methods - These methods are called only from the Protocol Manager
    // --------------------------------------------------------------------

    @Override
    public int registerParameter(String parameterPath) {
        parameterLock.lock();
        try {
            // If the registered parameters are equal or more than 99, reject
            if(this.registeredParameter.size() >= 99) {
                return -1;
            }
            // Convert first to a system element path
            SystemEntityPath path = SystemEntityPath.fromString(parameterPath);
            // If the map already has it, then ignore the request and return the used ID
            for (Map.Entry<Integer, SystemEntityPath> e : this.registeredParameter.entrySet()) {
                if (e.getValue().equals(path)) {
                    return e.getKey();
                }
            }
            // If you reach this stage, register the parameter and try to subscribe to the processing model
            // for updates
            int id = getNextParameterId();
            this.registeredParameter.put(id, path);
            try {
                registerForUpdate(path);
                return id;
            } catch (Exception e) {
                // Failed in registering
                LOG.log(Level.WARNING, String.format("Cannot register parameter %s to processing model: %s", parameterPath, e.getMessage()), new Object[]{this.name});
                deregisterForUpdate(id);
                return -1;
            }
        } finally {
            parameterLock.unlock();
        }
    }

    @Override
    public boolean deregisterParameter(int parameterId) {
        parameterLock.lock();
        try {
            return deregisterForUpdate(parameterId);
        } finally {
            parameterLock.unlock();
        }
    }

    @Override
    public void deregisterAllParameter() {
        parameterLock.lock();
        try {
            Set<Integer> toRemove = new HashSet<>(this.registeredParameter.keySet());
            this.registeredParameter.clear();
            this.registeredParameterCache.clear();
            this.freedIds.addAll(toRemove);
            try {
                context.getServiceFactory().getParameterDataMonitorService().unsubscribe(this.parameterDataSubscriber);
            } catch (RemoteException | ReatmetricException e) {
                // Ignore at this stage, just log
                LOG.log(Level.WARNING, String.format("Cannot deregister from processing model: %s", e.getMessage()), new Object[] { this.name });
                LOG.log(Level.FINEST, String.format("Cannot deregister from processing model: %s", e.getMessage()), e);
            }
        } finally {
            parameterLock.unlock();
        }
    }

    @Override
    public List<ParameterData> updateParameters() {
        parameterLock.lock();
        try {
            return new ArrayList<>(this.registeredParameterCache.values());
        } finally {
            parameterLock.unlock();
        }
    }

    @Override
    public List<OperationalMessage> updateLogs() {
        logLock.lock();
        try {
            List<OperationalMessage> copiedList = new LinkedList<>(latestLogs);
            latestLogs.clear();
            return copiedList;
        } finally {
            logLock.unlock();
        }
    }

    // --------------------------------------------------------------------
    // IParameterDataSubscriber delegate method - These methods are called by the ReatMetric Core
    // --------------------------------------------------------------------

    public void parameterDataItemReceived(List<ParameterData> dataItems) {
        parameterLock.lock();
        try {
            for(ParameterData pd : dataItems) {
                if(this.registeredParameterCache.containsKey(pd.getPath())) {
                    // Add to cache
                    this.registeredParameterCache.put(pd.getPath(), pd);
                }
                // Else, simply ignore
            }
        } finally {
            parameterLock.unlock();
        }
    }

    // --------------------------------------------------------------------
    // IOperationalMessageSubscriber delegate method - These methods are called by the ReatMetric Core
    // --------------------------------------------------------------------

    public void operationalMessageDataItemReceived(List<OperationalMessage> dataItems) {
        logLock.lock();
        try {
            latestLogs.addAll(dataItems);
            while(latestLogs.size() > 100) { // Inefficient like hell...
                latestLogs.remove(0);
            }
        } finally {
            logLock.unlock();
        }
    }

    // --------------------------------------------------------------------
    // Internal methods
    // --------------------------------------------------------------------

    private boolean deregisterForUpdate(int id) {
        if(registeredParameter.containsKey(id)) {
            SystemEntityPath path = registeredParameter.remove(id);
            if(path != null) {
                registeredParameterCache.remove(path);
            }
            freedIds.add(id);
            try {
                refreshSubscription();
            } catch (ReatmetricException e) {
                // Not need to worry
                LOG.log(Level.WARNING, String.format("Cannot deregister parameter %d to processing model: %s", id, e.getMessage()), new Object[] { this.name });
            }
            return true;
        } else {
            return false;
        }
    }

    private void registerForUpdate(SystemEntityPath parameterPath) throws ReatmetricException {
        // Check if the parameter actually exist: if not, exception
        AbstractSystemEntityDescriptor desc = context.getProcessingModel().getDescriptorOf(parameterPath);
        if(desc.getType() != SystemEntityType.PARAMETER) {
            throw new DriverException("provided path " + parameterPath + " does not refer to a parameter");
        }
        // The parameter is there, refresh the subscription
        refreshSubscription();
        // Add a null value to the cache map
        initialiseCacheMap(parameterPath);
    }

    private void initialiseCacheMap(SystemEntityPath parameterPath) {
        ParameterData pd = new ParameterData(new LongUniqueId(0), Instant.now(), 0, parameterPath.getLastPathElement(), parameterPath, null, null, "", Validity.UNKNOWN,
                AlarmState.UNKNOWN, null, Instant.now(), null);
        this.registeredParameterCache.putIfAbsent(parameterPath, pd);
    }

    private void refreshSubscription() throws ReatmetricException {
        ParameterDataFilter pdf = new ParameterDataFilter(null, this.registeredParameter.values(), null, null, null, null);
        try {
            context.getServiceFactory().getParameterDataMonitorService().subscribe(this.parameterDataSubscriber, pdf);
        } catch (RemoteException e) {
            throw new ReatmetricException(e);
        }
    }

    private int getNextParameterId() {
        // If an ID is available, return that
        if(freedIds.size() > 0) {
            return freedIds.remove(0);
        } else {
            // Else, return the sequencer
            return idSequencer.getAndIncrement();
        }
    }

    private void subscribeToMessages() {
        OperationalMessageFilter omf = new OperationalMessageFilter(null, null, null, null);
        try {
            context.getServiceFactory().getOperationalMessageMonitorService().subscribe(this.messageDataSubscriber, omf);
        } catch (RemoteException | ReatmetricException e) {
            // Ignore with warning
            LOG.log(Level.WARNING, String.format("Cannot register to operational message monitoring: %s", e.getMessage()), new Object[] { this.name });
            LOG.log(Level.FINEST, String.format("Cannot register to operational message monitoring: %s", e.getMessage()), e);
        }
    }

    private void unsubscribeToMessages() {
        try {
            context.getServiceFactory().getOperationalMessageMonitorService().unsubscribe(this.messageDataSubscriber);
        } catch (RemoteException | ReatmetricException e) {
            // Ignore with warning
            LOG.log(Level.WARNING, String.format("Cannot deregister from operational message monitoring: %s", e.getMessage()), new Object[] { this.name });
            LOG.log(Level.FINEST, String.format("Cannot deregister from operational message monitoring: %s", e.getMessage()), e);
        }
    }
}
