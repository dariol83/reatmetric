/*
 * Copyright (c)  2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.automation.base.common;

import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataSubscriber;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportConnectionStatus;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;

import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptExecutionManager {

    private static final Logger LOG = Logger.getLogger(ScriptExecutionManager.class.getName());

    private final IServiceCoreContext context;
    private final IActivityHandler.ActivityInvocation activityInvocation;
    private final String fileName;
    private final DataSubscriptionManager dataSubscriptionManager;

    private volatile boolean aborted = false;

    public ScriptExecutionManager(DataSubscriptionManager dataSubscriptionManager, IServiceCoreContext context, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.context = context;
        this.activityInvocation = activityInvocation;
        this.fileName = fileName;
        this.dataSubscriptionManager = dataSubscriptionManager;
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

    public SystemEntity system_entity(String path) {
        checkAborted();
        try {
            return context.getServiceFactory().getSystemModelMonitorService().getSystemEntityAt(SystemEntityPath.fromString(path));
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot fetch system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public EventData wait_for_event(String path, int timeoutSeconds) {
        checkAborted();
        try {
            EventWait waiter = new EventWait(path, timeoutSeconds);
            EventData eventReceived = null;
            try {
                eventReceived = waiter.waitForEvent();
            } finally {
                waiter.dispose();
            }
            return eventReceived;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot wait for event " + path + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public ParameterData wait_for_parameter(String path, int timeoutSeconds) {
        checkAborted();
        try {
            ParameterWait waiter = new ParameterWait(path, timeoutSeconds);
            ParameterData parameter = null;
            try {
                parameter = waiter.waitForParameter();
            } finally {
                waiter.dispose();
            }
            return parameter;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot wait for parameter " + path + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public void enable(String path) {
        checkAborted();
        try {
            context.getServiceFactory().getSystemModelMonitorService().enable(SystemEntityPath.fromString(path));
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot enable system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    public void disable(String path) {
        checkAborted();
        try {
            context.getServiceFactory().getSystemModelMonitorService().disable(SystemEntityPath.fromString(path));
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot disable system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    public void ignore(String path) {
        checkAborted();
        try {
            context.getServiceFactory().getSystemModelMonitorService().ignore(SystemEntityPath.fromString(path));
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot ignore system entity " + path + " from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    public boolean inject_parameter(String paramPath, Object value) {
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

    public boolean raise_event(String eventPath, String qualifier, Object report) {
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

    public ActivityInvocationBuilder prepare_activity(String activityPath) {
        checkAborted();
        try {
            return new ActivityInvocationBuilder(activityPath);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "Cannot invoke activity " + activityPath + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public TransportConnectionStatus connector_status(String connectorName) {
        checkAborted();
        try {
            return context.getServiceFactory().getTransportConnectors().stream()
                    .filter(o -> {
                        try {
                            return o.getName().equals(connectorName);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .map(o -> {
                        try {
                            return o.getConnectionStatus();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot retrieve status of transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return null;
        }
    }

    public boolean start_connector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.connect();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot connect transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean stop_connector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.disconnect();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot disconnect transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean abort_connector(String connectorName) {
        checkAborted();
        try {
            for(ITransportConnector c : context.getServiceFactory().getTransportConnectors()) {
                if(c.getName().equals(connectorName)) {
                    c.abort();
                    return true;
                }
            }
            return false;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot abort transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean init_connector(String connectorName, String[] keys, Object[] values) {
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
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot initialise transport connector " + connectorName + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    public boolean delete_scheduled_activity(String externalId) {
        checkAborted();
        try {
            context.getScheduler().remove(new ScheduledActivityDataFilter(null, null, null, null, null, Collections.singletonList(externalId)));
            return true;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Cannot delete scheduled item with ID " + externalId + " from automation " + fileName + ": " + e.getMessage(), e);
            return false;
        }
    }

    private void logMessage(String message, Severity severity) {
        checkAborted();
        try {
            context.getOperationalMessageBroker().distribute("Script", message, fileName, severity, null, null, true);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "logMessage exception from automation " + fileName + ": " + e.getMessage(), e);
        }
    }

    private class EventWait implements IEventDataSubscriber {

        private final Semaphore semaphore;
        private final int externalId;
        private final int timeout;
        private volatile EventData receivedEvent;

        public EventWait(String path, int timeoutSeconds) throws ReatmetricException, RemoteException {
            this.externalId = context.getServiceFactory().getSystemModelMonitorService().getExternalIdOf(SystemEntityPath.fromString(path));
            this.timeout = timeoutSeconds;
            this.semaphore = new Semaphore(0);
        }

        @Override
        public void dataItemsReceived(List<EventData> dataItems) {
            for(EventData ed : dataItems) {
                if(ed.getExternalId() == externalId) {
                    receivedEvent = ed;
                    semaphore.release();
                }
            }
        }

        public EventData waitForEvent() {
            dataSubscriptionManager.subscribe(this, externalId);
            try {
                for(int i = 0; i < timeout; ++i) {
                    checkAborted();
                    semaphore.tryAcquire(1, TimeUnit.SECONDS);
                }
                return receivedEvent; // Do not bother about concurrency, return one of the events you got if more than one
            } catch (InterruptedException e) {
                // No event
                return null;
            }
        }

        public void dispose() {
            dataSubscriptionManager.unsubscribe(this, externalId);
        }
    }

    private class ParameterWait implements IParameterDataSubscriber {

        private final Semaphore semaphore;
        private final int externalId;
        private final int timeout;
        private volatile ParameterData receivedParameter;

        public ParameterWait(String path, int timeoutSeconds) throws ReatmetricException, RemoteException {
            this.externalId = context.getServiceFactory().getSystemModelMonitorService().getExternalIdOf(SystemEntityPath.fromString(path));
            this.timeout = timeoutSeconds;
            this.semaphore = new Semaphore(0);
        }

        @Override
        public void dataItemsReceived(List<ParameterData> dataItems) {
            for(ParameterData ed : dataItems) {
                if(ed.getExternalId() == externalId) {
                    receivedParameter = ed;
                    semaphore.release();
                }
            }
        }

        public ParameterData waitForParameter() {
            dataSubscriptionManager.subscribe(this, externalId);
            try {
                for(int i = 0; i < timeout; ++i) {
                    checkAborted();
                    semaphore.tryAcquire(1, TimeUnit.SECONDS);
                }
                return receivedParameter; // Do not bother about concurrency, return one of the parameters you got if more than one
            } catch (InterruptedException e) {
                // No parameter
                return null;
            }
        }

        public void dispose() {
            dataSubscriptionManager.unsubscribe(this, externalId);
        }
    }

    public class ActivityInvocationBuilder {

        private final ActivityDescriptor descriptor;
        private final ActivityRequest.Builder requestBuilder;

        public ActivityInvocationBuilder(String activityPath) throws ReatmetricException {
            try {
                SystemEntityPath systemEntityPath = SystemEntityPath.fromString(activityPath);
                this.descriptor = (ActivityDescriptor) context.getProcessingModel().getDescriptorOf(systemEntityPath);
                this.requestBuilder = ActivityRequest.newRequest(this.descriptor.getExternalId(), systemEntityPath);
            } catch (ProcessingModelException e) {
                LOG.log(Level.SEVERE,"Cannot request invocation of " + activityPath + " from automation " + fileName + ": " + e.getMessage(), e);
                throw e;
            }
        }

        public ActivityInvocationBuilder with_route(String route) {
            checkAborted();
            this.requestBuilder.withRoute(route);
            return this;
        }

        public ActivityInvocationBuilder with_property(String k, String v) {
            checkAborted();
            this.requestBuilder.withProperty(k, v);
            return this;
        }

        public ActivityInvocationBuilder with_argument(String k, Object value, boolean engineering) {
            checkAborted();
            this.requestBuilder.withArgument(new PlainActivityArgument(k, engineering ? null : value, engineering ? value : null, engineering));
            return this;
        }

        public ActivityInvocationBuilder with_argument(AbstractActivityArgument arg) {
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

        public boolean execute_and_wait() {
            checkAborted();
            return execute().wait_for_completion();
        }

        public boolean execute_and_wait(int timeoutSeconds) {
            checkAborted();
            return execute().wait_for_completion(timeoutSeconds);
        }

        public SchedulingActivityInvocationBuilder prepare_schedule(String source, String externalId, Integer durationSeconds) {
            checkAborted();
            ActivityRequest request = this.requestBuilder.build();
            return new SchedulingActivityInvocationBuilder(SchedulingRequest.newRequest(request, source, externalId, durationSeconds != null ? Duration.ofSeconds(durationSeconds) : descriptor.getExpectedDuration()));
        }
    }

    public class SchedulingActivityInvocationBuilder {
        private final SchedulingRequest.Builder builder;
        private CreationConflictStrategy strategy = CreationConflictStrategy.ADD_ANYWAY;

        public SchedulingActivityInvocationBuilder(SchedulingRequest.Builder builder) {
            this.builder = builder;
        }

        public SchedulingActivityInvocationBuilder with_resource(String resource) {
            checkAborted();
            builder.withResource(resource);
            return this;
        }

        public SchedulingActivityInvocationBuilder with_resources(String... resources) {
            checkAborted();
            builder.withResources(resources);
            return this;
        }

        public SchedulingActivityInvocationBuilder with_resources(Collection<String> resources) {
            checkAborted();
            builder.withResources(resources);
            return this;
        }

        public SchedulingActivityInvocationBuilder with_latest_invocation_time(Instant time) {
            checkAborted();
            builder.withLatestInvocationTime(time);
            return this;
        }

        public SchedulingActivityInvocationBuilder with_conflict_strategy(ConflictStrategy conflictStrategy) {
            checkAborted();
            builder.withConflictStrategy(conflictStrategy);
            return this;
        }

        public SchedulingActivityInvocationBuilder with_creation_conflict_strategy(CreationConflictStrategy strategy) {
            checkAborted();
            this.strategy = strategy;
            return this;
        }

        public boolean schedule_absolute(Instant scheduledTime) {
            checkAborted();
            SchedulingRequest result = builder.build(new AbsoluteTimeSchedulingTrigger(scheduledTime));
            try {
                context.getScheduler().schedule(result, this.strategy);
                return true;
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Failed scheduling of activity " + result.getRequest().getPath() + " from automation file " + fileName);
                return false;
            }
        }

        public boolean schedule_relative(int secondsDelay, String... predecessors) {
            checkAborted();
            SchedulingRequest result = builder.build(new RelativeTimeSchedulingTrigger(new LinkedHashSet<>(Arrays.asList(predecessors)), secondsDelay));
            try {
                context.getScheduler().schedule(result, this.strategy);
                return true;
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Failed scheduling of activity " + result.getRequest().getPath() + " from automation file " + fileName);
                return false;
            }
        }

        public boolean schedule_event(String eventPath, int millisecondsProtectionTime) {
            checkAborted();
            SchedulingRequest result = builder.build(new EventBasedSchedulingTrigger(SystemEntityPath.fromString(eventPath), millisecondsProtectionTime, true));
            try {
                context.getScheduler().schedule(result, this.strategy);
                return true;
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Failed scheduling of activity " + result.getRequest().getPath() + " from automation file " + fileName);
                return false;
            }
        }
    }

    private class ActivityInvocationResult implements IActivityOccurrenceDataSubscriber {
        private IActivityExecutionService executionService;
        private final ActivityRequest request;

        private volatile IUniqueId activityId;

        private final Queue<ActivityOccurrenceData> dataToProcess = new LinkedList<>();
        private volatile ActivityOccurrenceData lastReport = null;
        private volatile boolean invocationFailed = false;
        private volatile boolean completed = false;

        public ActivityInvocationResult(ActivityRequest request) {
            try {
                this.executionService = context.getServiceFactory().getActivityExecutionService();
            } catch (ReatmetricException | RemoteException e) {
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
            try {
                // Subscribe
                dataSubscriptionManager.subscribe(this, request.getId());
                // Invoke
                this.activityId = executionService.startActivity(request);
            } catch (ReatmetricException | RemoteException e) {
                LOG.log(Level.SEVERE, "Failed invocation of activity " + this.activityId + " from automation file " + fileName);
                invocationFailed = true;
                dataSubscriptionManager.unsubscribe(this, request.getId());
            }
        }

        public boolean wait_for_completion(int timeoutSeconds) {
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

        public boolean wait_for_completion() {
            checkAborted();
            return wait_for_completion(0);
        }

        public boolean is_invocation_failed() {
            return this.invocationFailed;
        }

        public boolean is_completed() {
            return this.completed;
        }

        public ActivityReportState current_status() {
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
                if(aod.getInternalId().equals(this.activityId) && aod.getCurrentState() == ActivityOccurrenceState.COMPLETED) {
                    dataSubscriptionManager.unsubscribe(this, request.getId());
                }
                synchronized (this) {
                    dataToProcess.add(aod);
                    notifyAll();
                }
            }
        }
    }
}
