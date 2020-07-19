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
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptExecutionManager {

    private static final Logger LOG = Logger.getLogger(ScriptExecutionManager.class.getName());

    private final IServiceCoreContext context;
    private final IActivityHandler.ActivityInvocation activityInvocation;
    private final String fileName;

    public ScriptExecutionManager(IServiceCoreContext context, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.context = context;
        this.activityInvocation = activityInvocation;
        this.fileName = fileName;
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
        ParameterDataFilter pdf = new ParameterDataFilter(null, Collections.singletonList(SystemEntityPath.fromString(paramPath)), null, null, null, null);
        List<AbstractDataItem> values = context.getProcessingModel().get(pdf);
        if(values.isEmpty()) {
            return null;
        } else {
            return (ParameterData) values.get(0);
        }
    }

    public EventData event(String eventPath) {
        EventDataFilter edf = new EventDataFilter(null, Collections.singletonList(SystemEntityPath.fromString(eventPath)), null, null, null, null, null);
        List<AbstractDataItem> values = context.getProcessingModel().get(edf);
        if(values.isEmpty()) {
            return null;
        } else {
            return (EventData) values.get(0);
        }
    }

    public boolean set(String paramPath, Object value) {
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

    public ActivityInvocationBuilder prepareActivity(String activityPath) throws ReatmetricException {
        return new ActivityInvocationBuilder(activityPath);
    }

    private void logMessage(String message, Severity severity) {
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
                int id = context.getProcessingModel().getExternalIdOf(SystemEntityPath.fromString(activityPath));
                this.requestBuilder = ActivityRequest.newRequest(id);
            } catch (ProcessingModelException e) {
                LOG.log(Level.SEVERE,"Cannot request invocation of " + activityPath + " from automation " + fileName + ": " + e.getMessage(), e);
                throw e;
            }
        }

        public ActivityInvocationBuilder withRoute(String route) {
            this.requestBuilder.withRoute(route);
            return this;
        }

        public ActivityInvocationBuilder withProperty(String k, String v) {
            this.requestBuilder.withProperty(k, v);
            return this;
        }

        public ActivityInvocationBuilder withArgument(String k, Object value, boolean engineering) {
            this.requestBuilder.withArgument(new PlainActivityArgument(k, engineering ? null : value, engineering ? value : null, engineering));
            return this;
        }

        public ActivityInvocationBuilder withArgument(AbstractActivityArgument arg) {
            this.requestBuilder.withArgument(arg);
            return this;
        }

        public ActivityInvocationResult execute() {
            ActivityRequest request = this.requestBuilder.build();
            ActivityInvocationResult result = new ActivityInvocationResult(request);
            result.invoke();
            return result;
        }

        public boolean executeAndWait() {
            return execute().waitForCompletion();
        }

        public boolean executeAndWait(int timeoutSeconds) {
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
            if(invocationFailed) {
                return false;
            }
            boolean interrupted = false;
            boolean timeoutElapsed = false;
            Instant timeoutDate = Instant.now().plusSeconds(timeoutSeconds);
            synchronized (this) {
                while(true) {
                    while (dataToProcess.isEmpty()) {
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
            return waitForCompletion(0);
        }

        public boolean isInvocationFailed() {
            return this.invocationFailed;
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public ActivityReportState currentStatus() {
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
