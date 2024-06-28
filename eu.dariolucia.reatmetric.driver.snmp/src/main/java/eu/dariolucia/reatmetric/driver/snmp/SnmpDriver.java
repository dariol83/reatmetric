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
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.AbstractDriver;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpDevice;
import org.snmp4j.PDU;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.VariableBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class SnmpDriver extends AbstractDriver implements IRawDataRenderer {

    private static final Logger LOG = Logger.getLogger(SnmpDriver.class.getName());

    public static final String SNMP_MESSAGE_TYPE = "SNMP";
    public static final String CONFIGURATION_FILE_NAME = "configuration.xml";

    private SnmpConfiguration configuration;
    private final Map<String, SnmpTransportConnector> transportConnectorMap = new LinkedHashMap<>();

    private SnmpActivityHandler activityHandler;

    @Override
    public List<DebugInformation> currentDebugInfo() {
        return Collections.emptyList();
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return new LinkedList<>(transportConnectorMap.values());
    }

    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.singletonList(this);
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this.activityHandler);
    }

    @Override
    protected SystemStatus startProcessing() {
        // Create the transport connectors
        createTransportConnectors();
        // Create the activity handler
        createActivityHandler();
        // Done
        return SystemStatus.NOMINAL;
    }

    private void createActivityHandler() {
        this.activityHandler = new SnmpActivityHandler(configuration, transportConnectorMap);
    }

    private void createTransportConnectors() {
        for(SnmpDevice device : this.configuration.getSnmpDeviceList()) {
            SnmpTransportConnector connector = new SnmpTransportConnector(getName(),
                    device,
                    getContext().getRawDataBroker(),
                    getContext().getProcessingModel());
            connector.prepare();
            transportConnectorMap.put(device.getName(), connector);
        }
    }

    @Override
    protected SystemStatus processConfiguration(String driverConfiguration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context) throws DriverException {
        if(LOG.isLoggable(Level.INFO)) {
            LOG.info(String.format("Loading driver configuration at %s", driverConfiguration));
        }
        try {
            this.configuration = SnmpConfiguration.load(new FileInputStream(driverConfiguration + File.separator + CONFIGURATION_FILE_NAME));
            return SystemStatus.NOMINAL;
        } catch (IOException e) {
            throw new DriverException(e);
        }
    }

    @Override
    public String getHandler() {
        return getName();
    }

    @Override
    public List<String> getSupportedTypes() {
        return Collections.singletonList(SNMP_MESSAGE_TYPE);
    }

    @Override
    public LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException {
        if (!rawData.getHandler().equals(getHandler())) {
            throw new ReatmetricException("Raw data with handler " + rawData.getHandler() + " cannot be processed by driver " + configuration.getName() + ", expecting handler " + getHandler());
        }
        if (!rawData.getType().equals(SNMP_MESSAGE_TYPE)) {
            throw new ReatmetricException("Raw data with type " + rawData.getType() + " cannot be processed by driver " + configuration.getName() + ", expecting types " + getSupportedTypes());
        }
        LinkedHashMap<String, String> toReturn = new LinkedHashMap<>();
        PDU message = (PDU) rawData.getData();
        if(message == null) {
            byte[] contents = rawData.getContents();
            message = new PDU();
            try {
                message.decodeBER(new BERInputStream(ByteBuffer.wrap(contents)));
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Cannot decode SNMP PDU message from raw data: " + e.getMessage(), e);
                return toReturn;
            }
        }
        // Now message has all the info
        toReturn.put("Message Type", String.valueOf(message.getType()));
        toReturn.put("Error Status", message.getErrorStatusText());
        toReturn.put("Request ID", String.valueOf(message.getRequestID().getValue()));
        int nums = 0;
        for(VariableBinding vb : message.getVariableBindings()) {
            toReturn.put("OID #" + nums, vb.getOid().toString());
            toReturn.put("Value #" + nums, vb.getVariable().toString());
            toReturn.put("Type #" + nums, vb.getVariable().getSyntaxString());
            ++nums;
        }
        return toReturn;
    }
}
