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

package eu.dariolucia.reatmetric.driver.automation;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceState;
import eu.dariolucia.reatmetric.api.activity.ActivityReportState;
import eu.dariolucia.reatmetric.api.common.DebugInformation;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.SystemStatus;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.core.api.IDriver;
import eu.dariolucia.reatmetric.core.api.IDriverListener;
import eu.dariolucia.reatmetric.core.api.IRawDataRenderer;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.core.api.exceptions.DriverException;
import eu.dariolucia.reatmetric.core.configuration.ServiceCoreConfiguration;
import eu.dariolucia.reatmetric.driver.automation.common.Constants;
import eu.dariolucia.reatmetric.driver.automation.definition.AutomationConfiguration;
import eu.dariolucia.reatmetric.driver.automation.internal.ScriptExecutionManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import javax.script.*;
import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This driver provides the capability to execute automation and automation procedures written in Javascript.
 * <p>
 * The driver provides a simple API to scripts under execution, to easily access archived and processing data.
 *
 * TODO: implement support for Groovy and Python scripts ...
 */
public class AutomationDriver implements IDriver, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(AutomationDriver.class.getName());
    private static final String CONFIGURATION_FILE = "configuration.xml";
    public static final String EXECUTION_STAGE = "Execution";

    private volatile SystemStatus status = SystemStatus.UNKNOWN;

    private volatile String name;
    private volatile boolean running;
    private volatile IServiceCoreContext context;
    private volatile IProcessingModel model;
    private volatile IDriverListener subscriber;
    private volatile AutomationConfiguration configuration;

    private volatile String jsApiData;

    // For activity execution
    private volatile ExecutorService executor;

    public AutomationDriver() {
        //
    }

    @Override
    public void initialise(String name, String driverConfigurationDirectory, IServiceCoreContext context, ServiceCoreConfiguration coreConfiguration, IDriverListener subscriber) throws DriverException {
        try {
            this.name = name;
            this.context = context;
            this.subscriber = subscriber;

            this.configuration = AutomationConfiguration.load(new FileInputStream(driverConfigurationDirectory + File.separator + CONFIGURATION_FILE));
            this.executor = Executors.newFixedThreadPool(configuration.getMaxParallelScripts(), (t) -> {
                Thread toReturn = new Thread(t, "AutomationDriver Activity Handler Thread");
                toReturn.setDaemon(true);
                return toReturn;
            });
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(Constants.API_JS_RESOURCE_FILE);
            jsApiData = readContents(is);

            this.running = true;
            // Inform that everything is fine
            subscriber.driverStatusUpdate(this.name, SystemStatus.NOMINAL);
        } catch (Exception e) {
            updateStatus(SystemStatus.ALARM);
            throw new DriverException(e);
        }
    }

    private String readContents(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String read = null;
        while ((read = br.readLine()) != null) {
            sb.append(read).append((char) 10);
        }
        return sb.toString();
    }

    private void updateStatus(SystemStatus s) {
        boolean toNotify = s != this.status;
        this.status = s;
        if (toNotify) {
            this.subscriber.driverStatusUpdate(this.name, this.status);
        }
    }

    @Override
    public SystemStatus getDriverStatus() {
        return status;
    }

    /**
     * Renderers are used to visualise raw data in human readable format.
     * This driver has no renderers.
     *
     * @return the supported raw data renderers
     */
    @Override
    public List<IRawDataRenderer> getRawDataRenderers() {
        return Collections.emptyList();
    }

    /**
     * For this driver, the activity handler is the driver itself.
     *
     * @return the activity handler
     */
    @Override
    public List<IActivityHandler> getActivityHandlers() {
        return Collections.singletonList(this);
    }

    /**
     * This driver has no declared connectors.
     *
     * @return the transport connectors
     */
    @Override
    public List<ITransportConnector> getTransportConnectors() {
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        running = false;
        // Clean up omitted... it should be done
    }

    @Override
    public void registerModel(IProcessingModel model) {
        // Register it for activity purposes
        this.model = model;
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        // Deregister it for activity purposes
        this.model = null;
    }

    @Override
    public List<String> getSupportedRoutes() {
        return Collections.singletonList(Constants.AUTOMATION_ROUTE);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(Constants.T_SCRIPT_TYPE);
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        if (!activityInvocation.getType().equals(Constants.T_SCRIPT_TYPE)) {
            throw new ActivityHandlingException("Type " + activityInvocation.getType() + " not supported");
        }
        if (activityInvocation.getArguments() == null) {
            throw new ActivityHandlingException("Activity invocation has null argument map");
        }
        executor.submit(() -> execute(activityInvocation, model));
    }

    @Override
    public boolean getRouteAvailability(String route) {
        return route.equals(Constants.AUTOMATION_ROUTE);
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // TODO: record running scripts in a map, find way to tell GraalVM to abort execution/evaluation (if possible), execution stage shall be reported as FATAL
    }

    private void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        try {
            // Record verification
            announce(activityInvocation, model, Instant.now(), EXECUTION_STAGE, ActivityReportState.PENDING, ActivityOccurrenceState.EXECUTION, null, ActivityOccurrenceState.EXECUTION);
            // Look up automation: first argument plus argument name
            String fileName = (String) activityInvocation.getArguments().get(Constants.ARGUMENT_FILE_NAME);
            if (fileName == null) {
                // Use the full path name as automation name
                fileName = activityInvocation.getPath().asString();
            }
            File f = new File(configuration.getScriptFolder() + File.separator + fileName);
            if (!f.exists()) {
                throw new FileNotFoundException("File " + f.getAbsolutePath() + " does not exist");
            }
            String contents = Files.readString(f.toPath());
            Object result = null;
            if(f.getName().endsWith(Constants.JS_EXTENSION)) {
                // Run automation and retrieve result
                result = executeJs(contents, activityInvocation, fileName);
            } else {
                throw new ActivityHandlingException("Script type of " + f.getName() + " not supported: extension not recognized");
            }
            // Report final state
            announce(activityInvocation, model, Instant.now(), EXECUTION_STAGE, ActivityReportState.OK, ActivityOccurrenceState.EXECUTION, result, ActivityOccurrenceState.VERIFICATION);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Execution of procedure " + activityInvocation.getActivityOccurrenceId() + " failed: " + e.getMessage(), e);
            announce(activityInvocation, model, Instant.now(), EXECUTION_STAGE, ActivityReportState.FATAL, ActivityOccurrenceState.EXECUTION, null, ActivityOccurrenceState.EXECUTION);
        }
    }

    protected static void announce(IActivityHandler.ActivityInvocation invocation, IProcessingModel model, Instant genTime, String name, ActivityReportState reportState, ActivityOccurrenceState occState, Object result, ActivityOccurrenceState nextState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), name, genTime, occState, genTime, reportState, nextState, result));
    }

    public Object executeJs(String file, IActivityHandler.ActivityInvocation invocation, String fileName) throws ScriptException {
        try {
            try (Engine jsEngine = Engine.create()) {
                try (Context context = Context.newBuilder()
                        .engine(jsEngine)
                        .allowAllAccess(true)
                        .build()) {
                    Value bindings = context.getBindings("js");
                    for (Map.Entry<String, Object> entry : invocation.getArguments().entrySet()) {
                        if (!entry.getKey().equals(Constants.ARGUMENT_FILE_NAME)) {
                            bindings.putMember(entry.getKey(), entry.getValue());
                        }
                    }
                    // Add API functions
                    ScriptExecutionManager manager = new ScriptExecutionManager(this.context, invocation, fileName);
                    bindings.putMember(Constants.BINDING_SCRIPT_MANAGER, manager);
                    context.eval("js", jsApiData);
                    return context.eval("js", file);
                }
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        } catch (Error e) {
            LOG.log(Level.SEVERE, "Unexpected error when executing script " + fileName + ": " + e.getMessage(), e);
            throw new ScriptException(e.getMessage());
        }
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        // TODO: implement if needed
        return Collections.emptyList();
    }
}
