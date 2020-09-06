/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.automation.internal;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptExecutionManager {

    private static final Logger LOG = Logger.getLogger(ScriptExecutionManager.class.getName());

    private final IServiceCoreContext context;
    private final IActivityHandler.ActivityInvocation activityInvocation;
    private final String fileName;
    private volatile boolean aborted = false;

    public ScriptExecutionManager(IServiceCoreContext context, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.context = context;
        this.activityInvocation = activityInvocation;
        this.fileName = fileName;
    }

    public synchronized void _abort() {
        this.aborted = true;
        notifyAll();
    }

    private void checkAborted() {
        if(aborted) {
            throw new IllegalStateException("Script aborted");
        }
    }

    public void info(String message) {
        logMessage(message, Severity.INFO);
    }

    public void warning(String message) {
        logMessage(message, Severity.WARN);
    }

    public void alarm(String message) {
        logMessage(message, Severity.ALARM);
    }

    public ParameterData parameter(String paramPath) {
        checkAborted();
        ParameterDataFilter pdf = new ParameterDataFilter(null, Collections.singletonList(SystemEntityPath.fromString(paramPath)), null, null, null, null);
        List<AbstractDataItem> values = context.getProcessingModel().get(pdf);
        if(values.isEmpty()) {
            return null;
        } else {
            return (ParameterData) values.get(0);
        }
    }

    public EventData event(String eventPath) {
        checkAborted();
        EventDataFilter edf = new EventDataFilter(null, Collections.singletonList(SystemEntityPath.fromString(eventPath)), null, null, null, null, null);
        List<AbstractDataItem> values = context.getProcessingModel().get(edf);
        if(values.isEmpty()) {
            return null;
        } else {
            return (EventData) values.get(0);
        }
    }

    public SystemEntity systemEntity(String path) {
        checkAborted();
        try {
            return context.getServiceFactory().getSystemModelMonitorService().getSystemEntityAt(SystemEntityPath.fromString(path));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot fetch system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public void enable(String path) {
        checkAborted();
        try {
            context.getServiceFactory().getSystemModelMonitorService().enable(SystemEntityPath.fromString(path));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot enable system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    public void disable(String path) {
        checkAborted();
        try {
            context.getServiceFactory().getSystemModelMonitorService().disable(SystemEntityPath.fromString(path));
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot disable system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
        }
    }


    public boolean set(String paramPath, Object value) {
        checkAborted();
        Instant now = Instant.now();
        try {
            int id = context.getProcessingModel().getExternalIdOf(SystemEntityPath.fromString(paramPath));
            ParameterSample sample = ParameterSample.of(id, now, now, null, value, null, null);
            context.getProcessingModel().injectParameters(Collections.singletonList(sample));
            return true;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot set parameter " + paramPath + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean raise(String eventPath, String qualifier, Object report) {
        checkAborted();
        Instant now = Instant.now();
        try {
            int id = context.getProcessingModel().getExternalIdOf(SystemEntityPath.fromString(eventPath));
            EventOccurrence eo = EventOccurrence.of(id, now, now, null, qualifier, report, null, activityInvocation.getPath().asString(), null);
            context.getProcessingModel().raiseEvent(eo);
            return true;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot raise event " + eventPath + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public ActivityInvocationBuilder prepareActivity(String activityPath) {
        checkAborted();
        try {
            return new ActivityInvocationBuilder(activityPath);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot invoke activity " + activityPath + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public TransportConnectionStatus connectorStatus(String connectorName) {
        checkAborted();
        try {
            return context.getServiceFactory().getTransportConnectors().stream().filter(o -> o.getName().equals(connectorName)).map(ITransportConnector::getConnectionStatus).findFirst().orElse(null);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot retrieve status of transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public boolean startConnector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.connect();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot connect transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean stopConnector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.disconnect();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot disconnect transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean abortConnector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.abort();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot abort transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean initConnector(String connectorName, String[] keys, Object[] values) {
        checkAborted();
        if(keys.length != values.length) {
            LOG.log(Level.SEVERE, "Cannot initialise transport connector " + connectorName + " from automation " + fileName + ": lists of keys-values differ in size");
            return false;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for(int i = 0; i < keys.length; ++i) {
            map.put(keys[i], values[i]);
        }
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.initialise(map);
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot initialise transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    private void logMessage(String message, Severity severity) {
        checkAborted();
        try {
            context.getOperationalMessageBroker().distribute("Script", message, fileName, severity, null, true);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "logMessage exception from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    public class ActivityInvocationBuilder {

        private final ActivityRequest.Builder requestBuilder;

        public ActivityInvocationBuilder(String activityPath) throws ReatmetricException {
            try {
                SystemEntityPath systemEntityPath = SystemEntityPath.fromString(activityPath);
                int id = context.getProcessingModel().getExternalIdOf(systemEntityPath);
                this.requestBuilder = ActivityRequest.newRequest(id, systemEntityPath);
            } catch (ProcessingModelException e) {
                LOG.log(Level.SEVERE,"Cannot request invocation of " + activityPath + " from automation " + fileName + ": " + e.getMessage(), e);
                throw e;
            }
        }

        public ActivityInvocationBuilder withRoute(String route) {
            checkAborted();
            this.requestBuilder.withRoute(route);
            return this;
        }

        public ActivityInvocationBuilder withProperty(String k, String v) {
            checkAborted();
            this.requestBuilder.withProperty(k, v);
            return this;
        }

        public ActivityInvocationBuilder withArgument(String k, Object value, boolean engineering) {
            checkAborted();
            this.requestBuilder.withArgument(new PlainActivityArgument(k, engineering ? null : value, engineering ? value : null, engineering));
            return this;
        }

        public ActivityInvocationBuilder withArgument(AbstractActivityArgument arg) {
            checkAborted();
            this.requestBuilder.withArgument(arg);
            return this;
        }

        public ActivityInvocationResult execute() {
            checkAborted();
            ActivityRequest request = this.requestBuilder.build();
            ActivityInvocationResult result = new ActivityInvocationResult(request);
            result.invoke();
            return result;
        }

        public boolean executeAndWait() {
            checkAborted();
            return execute().waitForCompletion();
        }

        public boolean executeAndWait(int timeoutSeconds) {
            checkAborted();
            return execute().waitForCompletion(timeoutSeconds);
        }
    }

    private class ActivityInvocationResult implements IActivityOccurrenceDataSubscriber {
        private IActivityExecutionService executionService;
        private IActivityOccurrenceDataProvisionService dataProvisionService;
        private final ActivityRequest request;

        private volatile IUniqueId activityId;

        private final Queue<ActivityOccurrenceData> dataToProcess = new LinkedList<>();
        private volatile ActivityOccurrenceData lastReport = null;
        private volatile boolean invocationFailed = false;
        private volatile boolean completed = false;

        public ActivityInvocationResult(ActivityRequest request) {
            try {
                this.executionService = context.getServiceFactory().getActivityExecutionService();
                this.dataProvisionService = context.getServiceFactory().getActivityOccurrenceDataMonitorService();
            } catch (ReatmetricException e) {
                LOG.log(Level.SEVERE, "Cannot obtain processing model services: " + e.getMessage(), e);
                invocationFailed = true;
            }
            this.request = request;
        }

        private void invoke() {
            if(this.invocationFailed) {
                return;
            }
            if(this.activityId != null) {
                return;
            }
            // Subscribe
            dataProvisionService.subscribe(this, new ActivityOccurrenceDataFilter(null, null, null, null, null, null, Collections.singletonList(request.getId())));
            // Invoke
            try {
                this.activityId = executionService.startActivity(request);
            } catch (ReatmetricException e) {
                invocationFailed = true;
                dataProvisionService.unsubscribe(this);
            }
        }

        public boolean waitForCompletion(int timeoutSeconds) {
            checkAborted();
            if(invocationFailed) {
                return false;
            }
            boolean interrupted = false;
            boolean timeoutElapsed = false;
            Instant timeoutDate = Instant.now().plusSeconds(timeoutSeconds);
            synchronized (this) {
                while(true) {
                    while (dataToProcess.isEmpty()) {
                        checkAborted();
                        try {
                            wait(1000, 0);
                        } catch (InterruptedException e) {
                            //
                            interrupted = true;
                            break;
                        }
                    }
                    if(timeoutSeconds > 0 && timeoutDate.isBefore(Instant.now())) {
                        // timeout elapsed
                        timeoutElapsed = true;
                    }
                    while (!dataToProcess.isEmpty()) {
                        ActivityOccurrenceData aod = dataToProcess.remove();
                        if(aod.getInternalId().equals(this.activityId)) {
                            this.lastReport = aod;
                            if(aod.getCurrentState() == ActivityOccurrenceState.COMPLETED) {
                                completed = true;
                                return aod.aggregateStatus() == ActivityReportState.OK;
                            }
                        }
                    }
                    if(interrupted || timeoutElapsed) {
                        break;
                    }
                }
                // If you are here, it means the thread was interrupted
                return false;
            }
        }

        public boolean waitForCompletion() {
            checkAborted();
            return waitForCompletion(0);
        }

        public boolean isInvocationFailed() {
            return this.invocationFailed;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public ActivityReportState currentStatus() {
            checkAborted();
            if(invocationFailed) {
                return ActivityReportState.FAIL;
            }
            ActivityOccurrenceData rep = this.lastReport;
            if(rep == null) {
                return ActivityReportState.UNKNOWN;
            } else {
                return rep.aggregateStatus();
            }
        }

        @Override
        public void dataItemsReceived(List<ActivityOccurrenceData> dataItems) {
            for(ActivityOccurrenceData aod : dataItems) {
                synchronized (this) {
                    dataToProcess.add(aod);
                    notifyAll();
                }
                if(aod.getInternalId().equals(this.activityId) && aod.getCurrentState() == ActivityOccurrenceState.COMPLETED) {
                    dataProvisionService.unsubscribe(this);
                }
            }
        }
    }
}
