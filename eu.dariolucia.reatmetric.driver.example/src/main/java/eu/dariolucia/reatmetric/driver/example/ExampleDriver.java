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

package eu.dariolucia.reatmetric.driver.example;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceReport;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.core.api.AbstractDriver;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class ExampleDriver extends AbstractDriver {

    public static final String ROUTE_NAME = "Example Route";
    public static final String ACTIVITY_TYPE = "Example Activity Type";
    public static final String RESET_EXECUTION_NAME = "Reset Execution";
    private final String PARAMETER_NAME = "Example-Parameter";
    private final String EVENT_NAME = "Example-Event";
    private final String ACTIVITY_NAME = "Example-Activity";

    private SystemEntityPath parentSystemElement;

    private int parameterId;
    private int eventId;
    private int activityId;

    private ExampleConnector connector;
    private ExampleHandler handler;

    public List<DebugInformation> currentDebugInfo() {
        return Collections.emptyList();
    }

    protected SystemStatus startProcessing() throws DriverException {
        // Resolve the paths into IDs
        try {
            this.parameterId = getContext().getProcessingModel().getDescriptorOf(this.parentSystemElement.append(PARAMETER_NAME)).getExternalId();
            this.eventId = getContext().getProcessingModel().getDescriptorOf(this.parentSystemElement.append(EVENT_NAME)).getExternalId();
            this.activityId = getContext().getProcessingModel().getDescriptorOf(this.parentSystemElement.append(ACTIVITY_NAME)).getExternalId();
        } catch (ReatmetricException e) {
            throw new DriverException(e);
        }
        // Create the connector
        this.connector = new ExampleConnector("Example Connector", "Example Connector Description", this);
        // Create the activity handler
        this.handler = new ExampleHandler(this);
        // If we are here, all fine
        return SystemStatus.NOMINAL;
    }

    protected SystemStatus processConfiguration(String driverConfiguration, ServiceCoreConfiguration coreConfiguration, IServiceCoreContext context) throws DriverException {
        this.parentSystemElement = SystemEntityPath.fromString(driverConfiguration);
        return SystemStatus.NOMINAL;
    }

    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this.handler);
    }

    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.singletonList(this.connector);
    }

    public void newValue(long toDistribute) {
        // Parameter injection
        ParameterSample sample = ParameterSample.of(this.parameterId, toDistribute);
        getContext().getProcessingModel().injectParameters(Collections.singletonList(sample));
        // Event injection
        if(toDistribute % 10 == 0) {
            EventOccurrence event = EventOccurrence.of(this.eventId);
            getContext().getProcessingModel().raiseEvent(event);
        }
    }

    public boolean isConnectorStarted() {
        return this.connector.getConnectionStatus() == TransportConnectionStatus.OPEN;
    }

    public boolean isActivitySupported(SystemEntityPath path, int requestedActivity) {
        return path.equals(this.parentSystemElement.append(ACTIVITY_NAME)) && requestedActivity == this.activityId;
    }

    public void executeCounterReset(IActivityHandler.ActivityInvocation activityInvocation) {
        // Informing that we are proceeding with the release of the activity occurrence, and that, if it works, we go
        // directly in the EXECUTION state
        reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.PENDING,
                ActivityOccurrenceState.EXECUTION);
        if(!isConnectorStarted()) {
            // Connector not started, release failed
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                    ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.RELEASE);
            // That's it
            return;
        } else {
            // Connector started, release OK, pending execution
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                    ActivityOccurrenceState.RELEASE, ActivityOccurrenceReport.RELEASE_REPORT_NAME, ActivityReportState.OK,
                    ActivityOccurrenceState.EXECUTION);
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                    ActivityOccurrenceState.EXECUTION, RESET_EXECUTION_NAME, ActivityReportState.PENDING,
                    ActivityOccurrenceState.VERIFICATION);
        }
        // Execution of the activity
        boolean resetCounter = this.connector.resetCounter();
        if(resetCounter) {
            // Good, activity finished OK
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                    ActivityOccurrenceState.EXECUTION, RESET_EXECUTION_NAME, ActivityReportState.OK,
                    ActivityOccurrenceState.VERIFICATION);
        } else {
            // Bad, activity finished with error
            reportActivityState(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId(), Instant.now(),
                    ActivityOccurrenceState.EXECUTION, RESET_EXECUTION_NAME, ActivityReportState.FATAL,
                    ActivityOccurrenceState.EXECUTION);
        }
    }
}
