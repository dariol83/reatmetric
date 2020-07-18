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

package eu.dariolucia.reatmetric.driver.script.internal;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;

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
        // TODO
        return null;
    }

    public EventData event(String eventPath) {
        // TODO
        return null;
    }

    public void inject(String paramPath, Object value, boolean raw) {
        // TODO
    }

    public void raise(String eventPath) {
        // TODO
    }

    public InvocationBuilder prepareActivity(String activityPath) {
        return new InvocationBuilder(context.getProcessingModel(), activityPath);
    }

    private void logMessage(String message, Severity severity) {
        try {
            context.getOperationalMessageBroker().distribute("Script", message, fileName, severity, null, true);
        } catch (ReatmetricException e) {
            LOG.log(Level.SEVERE, "logMessage exception from script " + fileName + ": " + e.getMessage(), e);
        }
    }

    public class InvocationBuilder {

        private final IProcessingModel processingModel;
        private final String activityPath;

        public InvocationBuilder(IProcessingModel processingModel, String activityPath) {
            this.processingModel = processingModel;
            this.activityPath = activityPath;
        }
    }
}
