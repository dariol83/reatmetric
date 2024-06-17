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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.transport.AbstractTransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.api.transport.exceptions.TransportException;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;
import eu.dariolucia.reatmetric.driver.snmp.configuration.GroupConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpDevice;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnmpTransportConnector extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(SnmpTransportConnector.class.getName());

    private final SnmpDevice device;
    private final IRawDataBroker rawDataBroker;
    private final IProcessingModel processingModel;
    private final Timer deviceTimer;
    private final CommunityTarget<Address> target;
    private volatile Snmp connection;

    protected SnmpTransportConnector(SnmpDevice device, IRawDataBroker rawDataBroker, IProcessingModel processingModel) {
        super(device.getName(), "");
        this.device = device;
        this.rawDataBroker = rawDataBroker;
        this.processingModel = processingModel;
        this.deviceTimer = new Timer("SNMP Device " + getName() + " Timer Service", true);
        // Build the target
        this.target = new CommunityTarget<>();
        Address targetAddress = GenericAddress.parse(device.getConnectionString());
        this.target.setCommunity(new OctetString(device.getCommunity()));
        this.target.setAddress(targetAddress);
        this.target.setRetries(device.getRetries());
        this.target.setTimeout(device.getTimeout());
        this.target.setVersion(device.getVersion().toValue());
    }

    @Override
    protected Pair<Long, Long> computeBitrate() {
        return null;
    }

    @Override
    protected synchronized void doConnect() {
        if(this.connection != null) {
            return;
        }
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.CONNECTING);
        try {
            TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
            this.connection = new Snmp(transport);
            // Do not forget this line!
            transport.listen();
            // Check
            updateConnectionStatus(TransportConnectionStatus.OPEN);
            // Now activate the periodic pollings
            for(GroupConfiguration gc : device.getDeviceConfiguration().getGroupConfigurationList()) {
                this.deviceTimer.schedule(buildTimerTask(gc), 0, gc.getPollingTime());
            }
        } catch (IOException e) {
            updateConnectionStatus(TransportConnectionStatus.ERROR);
            updateAlarmState(AlarmState.ERROR);
            if(this.connection != null) {
                try {
                    this.connection.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            this.connection = null;
        }
    }

    private TimerTask buildTimerTask(GroupConfiguration group) {
        return new TimerTask() {
            @Override
            public void run() {
                Snmp theConnection = connection;
                if(theConnection == null) {
                    this.cancel();
                    return;
                }
                PDU request = group.preparePollRequest();
                try {
                    ResponseEvent<?> responseEvent = sendRequest(theConnection, request);
                    if ((responseEvent != null) && (responseEvent.getResponse() != null)) {
                        List<ParameterSample> parameterSamples = group.mapResponse(device, responseEvent);
                        injectSamples(parameterSamples);
                    } else {
                        if(LOG.isLoggable(Level.WARNING)) {
                            LOG.warning("Response from endpoint " + connection + " not received/null for group " + group);
                        }
                        updateAlarmState(AlarmState.ALARM);
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Request to endpoint " + connection + " returned an exception for group " + group + ": " + e.getMessage(), e);
                    updateAlarmState(AlarmState.ALARM);
                }
            }
        };
    }

    private void injectSamples(List<ParameterSample> parameterSamples) {
        this.processingModel.injectParameters(parameterSamples);
    }

    private ResponseEvent<?> sendRequest(Snmp theConnection, PDU request) throws IOException {
        synchronized (theConnection) {
            return theConnection.send(request, target, null);
        }
    }

    @Override
    protected synchronized void doDisconnect() {
        if(this.connection == null) {
            return;
        }
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        try {
            this.connection.close();
        } catch (IOException ex) {
            // Ignore
        }
        this.connection = null;
        updateConnectionStatus(TransportConnectionStatus.IDLE);
    }

    @Override
    protected void doDispose() {
        this.deviceTimer.purge();
        this.deviceTimer.cancel();
        // TODO: check what you need to do
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }
}
