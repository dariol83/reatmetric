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

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
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
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnmpTransportConnector extends AbstractTransportConnector {

    private static final Logger LOG = Logger.getLogger(SnmpTransportConnector.class.getName());
    public static final String EXECUTION_REPORT_NAME = "Execution";

    private final String driverName;
    private final SnmpDevice device;
    private final IRawDataBroker rawDataBroker;
    private final IProcessingModel processingModel;
    private final Timer deviceTimer;
    private final CommunityTarget<Address> target;
    private volatile Snmp connection;

    private final List<TimerTask> pollingTasks = new LinkedList<>();

    protected SnmpTransportConnector(String driverName, SnmpDevice device, IRawDataBroker rawDataBroker, IProcessingModel processingModel) {
        super(device.getName(), "");
        this.driverName = driverName;
        this.device = device;
        this.rawDataBroker = rawDataBroker;
        this.processingModel = processingModel;
        this.deviceTimer = new Timer("SNMP Device " + getName() + " Timer Service", true);
        // Initialise
        this.device.getDeviceConfiguration().initialise(device.getPath(), this.processingModel);
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
                TimerTask task = buildTimerTask(gc);
                // Remember timer tasks, so that disconnect can stop these tasks
                this.pollingTasks.add(task);
                this.deviceTimer.schedule(task, 0, gc.getPollingTime());
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
                Snmp theConnection = null;
                synchronized (SnmpTransportConnector.this) {
                    theConnection = connection;
                    if (theConnection == null) {
                        // This is weird, do nothing
                        LOG.log(Level.WARNING, "SNMP Polling Task for group " + group.getName() + " started but no connection is available");
                        return;
                    }
                }
                PDU request = group.preparePollRequest();
                try {
                    ResponseEvent<?> responseEvent = sendRequest(theConnection, request);
                    if ((responseEvent != null) && (responseEvent.getResponse() != null)) {
                        Instant generationTime = Instant.now();
                        distributeRawData(responseEvent, generationTime, group);
                        List<ParameterSample> parameterSamples = group.mapResponse(device, responseEvent, generationTime);
                        injectSamples(parameterSamples);
                        updateAlarmState(AlarmState.NOMINAL);
                    } else {
                        if(LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "Response from endpoint " + device.getConnectionString() + " not received/null for group " + group.getName(), new Object[] { device.getName() });
                        }
                        updateAlarmState(AlarmState.ALARM);
                    }
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Request to endpoint " + device.getConnectionString() + " returned an exception for group " + group.getName() + ": " + e.getMessage(), e);
                    updateAlarmState(AlarmState.ALARM);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Request to endpoint " + device.getConnectionString() + " returned an unknown exception for group " + group.getName() + ": " + e.getMessage(), e);
                    updateAlarmState(AlarmState.ALARM);
                }
            }
        };
    }

    private void distributeRawData(ResponseEvent<?> responseEvent, Instant generationTime, GroupConfiguration group) {
        if(!group.isDistributePdu()) {
            return;
        }
        try {
            PDU response = responseEvent.getResponse();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            response.encodeBER(bos);
            bos.close();
            byte[] serialize = bos.toByteArray();
            RawData rd = new RawData(rawDataBroker.nextRawDataId(), generationTime, group.getName(),
                    SnmpDriver.SNMP_MESSAGE_TYPE, device.getName(), device.getName(), Quality.GOOD, null, serialize,
                    generationTime, driverName, null);
            rd.setData(response);
            rawDataBroker.distribute(Collections.singletonList(rd));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Error while distributing SNMP PDU from route " + device.getName(), e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error while encoding SNMP PDU for distribution from route " + device.getName(), e);
        }
    }

    private void injectSamples(List<ParameterSample> parameterSamples) {
        this.processingModel.injectParameters(parameterSamples);
    }

    private ResponseEvent<?> sendRequest(Snmp theConnection, PDU request) throws IOException {
        synchronized (theConnection) {
            return theConnection.send(request, target);
        }
    }

    @Override
    protected synchronized void doDisconnect() {
        if(this.connection == null) {
            return;
        }
        updateAlarmState(AlarmState.NOT_APPLICABLE);
        updateConnectionStatus(TransportConnectionStatus.DISCONNECTING);
        this.pollingTasks.forEach(TimerTask::cancel);
        this.pollingTasks.clear();
        try {
            this.connection.close();
        } catch (IOException ex) {
            // Ignore
        }
        this.connection = null;
        updateConnectionStatus(TransportConnectionStatus.IDLE);
        updateAlarmState(AlarmState.NOMINAL);
    }

    @Override
    protected synchronized void doDispose() {
        this.deviceTimer.purge();
        this.deviceTimer.cancel();
        try {
            if(this.connection != null) {
                this.connection.close();
            }
        } catch (IOException ex) {
            // Ignore
        }
        this.connection = null;
    }

    @Override
    public void abort() throws TransportException, RemoteException {
        disconnect();
    }

    public synchronized void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException {
        if(connection == null || getConnectionStatus() != TransportConnectionStatus.OPEN) {
            throw new ActivityHandlingException("Connector " + getName() + " not started");
        }
        // OK, forward in separate task, use timer, one-off
        this.deviceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                dispatchActivity(activityInvocation);
            }
        }, 0);
    }

    private void dispatchActivity(IActivityHandler.ActivityInvocation activityInvocation) {
        Instant time = Instant.now();
        // Notify attempt to dispatch
        reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.PENDING,
                ActivityOccurrenceState.TRANSMISSION);
        // Get the connection
        Snmp theConnection = null;
        synchronized (SnmpTransportConnector.this) {
            theConnection = connection;
            if (theConnection == null) {
                // No connection, release failed
                reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                        ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL,
                        ActivityOccurrenceState.RELEASE);
                return;
            }
        }

        PDU request = encodeSetRequest(activityInvocation);
        if(request == null) {
            LOG.log(Level.SEVERE, "Response from endpoint " + device.getConnectionString() + " not received/null for activity " + activityInvocation.getPath(), new Object[] { device.getName() });
            // Cannot encode request
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.RELEASE);
            return;
        }
        try {
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK,
                    ActivityOccurrenceState.EXECUTION);
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.EXECUTION, EXECUTION_REPORT_NAME, ActivityReportState.PENDING,
                    ActivityOccurrenceState.EXECUTION);
            ResponseEvent<?> responseEvent = sendRequest(theConnection, request);
            if ((responseEvent != null) && (responseEvent.getResponse() != null)) {
                reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                        ActivityOccurrenceState.EXECUTION, EXECUTION_REPORT_NAME, ActivityReportState.OK,
                        ActivityOccurrenceState.VERIFICATION);
            } else {
                reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                        ActivityOccurrenceState.EXECUTION, EXECUTION_REPORT_NAME, ActivityReportState.FATAL,
                        ActivityOccurrenceState.EXECUTION);
                if(LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.SEVERE, "Response from endpoint " + device.getConnectionString() + " not received/null for activity " + activityInvocation.getPath(), new Object[] { device.getName() });
                }
            }
        } catch (IOException e) {
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.EXECUTION, EXECUTION_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.EXECUTION);
            LOG.log(Level.SEVERE, "Request to endpoint " + device.getConnectionString() + " returned an exception for activity " + activityInvocation.getPath() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), time,
                    ActivityOccurrenceState.EXECUTION, EXECUTION_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.EXECUTION);
            LOG.log(Level.SEVERE, "Request to endpoint " + device.getConnectionString() + " returned an unknown exception for activity " + activityInvocation.getPath() + ": " + e.getMessage(), e);
        }
    }

    private PDU encodeSetRequest(IActivityHandler.ActivityInvocation activityInvocation) {
        try {
            PDU pdu = new PDU();
            String oid = activityInvocation.getArguments().get("OID").toString();
            OID theOid = new OID(oid);
            Object value = activityInvocation.getArguments().get("Value");
            Variable theVar;
            if(value == null) {
                theVar = new Null();
            } else if(value instanceof String) {
                theVar = new OctetString((String) value);
            } else if(value instanceof Number) {
                theVar = new Integer32(((Number) value).intValue());
            } else {
                throw new IllegalArgumentException("value type " + value.getClass().getSimpleName() + " not supported");
            }
            pdu.setVariableBindings(Collections.singletonList(new VariableBinding(theOid, theVar)));
            pdu.setType(PDU.SET);
            return pdu;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot encode SNMP set request: " + e.getMessage(), e);
            return null;
        }
    }

    public void reportActivityState(int activityId, IUniqueId activityOccurrenceId, Instant time, ActivityOccurrenceState state, String releaseReportName, ActivityReportState status, ActivityOccurrenceState nextState) {
        processingModel.reportActivityProgress(ActivityProgress.of(activityId, activityOccurrenceId, releaseReportName, time, state, null, status, nextState, null));
    }
}
