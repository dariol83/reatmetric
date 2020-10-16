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
import eu.dariolucia.reatmetric.api.common.Pair;
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
import eu.dariolucia.reatmetric.driver.automation.internal.*;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This driver provides the capability to execute automation and automation procedures written in Javascript, Groovy or
 * Python.
 * <p>
 * The driver provides a simple API to scripts under execution, to easily access archived and processing data.
 * <p>
 * As limitation related to the Python language, Python scripts will not return any value.
 */
public class AutomationDriver implements IDriver, IActivityHandler {

    private static final Logger LOG = Logger.getLogger(AutomationDriver.class.getName());

    private static final String CONFIGURATION_FILE = "configuration.xml";
    private static final String EXECUTION_STAGE = "Execution";

    private volatile SystemStatus status = SystemStatus.UNKNOWN;

    private volatile String name;
    private volatile boolean running;
    private volatile IServiceCoreContext context;
    private volatile IProcessingModel model;
    private volatile IDriverListener subscriber;
    private volatile AutomationConfiguration configuration;

    // API file contents
    private volatile String jsApiData;
    private volatile String groovyApiData;
    private volatile String pythonApiData;

    // For activity execution
    private volatile ExecutorService executor;
    private volatile DataSubscriptionManager dataSubscriptionManager;

    // For activity abortion (done under lock)
    private final Map<Pair<Integer, IUniqueId>, IScriptExecutor> runningExecutors = new HashMap<>();
    private final Set<Pair<Integer, IUniqueId>> pendingAborts = new HashSet<>();

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
            this.executor = new ThreadPoolExecutor(configuration.getMaxParallelScripts(), // start with 1 thread
                    configuration.getMaxParallelScripts(), // max size
                    600, // idle timeout
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(100), // more than 100 automation procedures in the queue means that the next one is rejected
                    (t) -> {
                        Thread toReturn = new Thread(t, "AutomationDriver Activity Handler Thread");
                        toReturn.setDaemon(true);
                        return toReturn;
                    }
            ); // queue with a size

            this.dataSubscriptionManager = new DataSubscriptionManager(context.getServiceFactory().getActivityOccurrenceDataMonitorService(),
                    context.getServiceFactory().getEventDataMonitorService(),
                    context.getServiceFactory().getParameterDataMonitorService());
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(Constants.API_JS_RESOURCE_FILE);
            jsApiData = readContents(is);
            is = this.getClass().getClassLoader().getResourceAsStream(Constants.API_GROOVY_RESOURCE_FILE);
            groovyApiData = readContents(is);
            is = this.getClass().getClassLoader().getResourceAsStream(Constants.API_PYTHON_RESOURCE_FILE);
            pythonApiData = readContents(is);

            this.running = true;
            // Inform that everything is fine
            subscriber.driverStatusUpdate(this.name, SystemStatus.NOMINAL);
        } catch (Exception e) {
            this.status = SystemStatus.ALARM;
            this.subscriber.driverStatusUpdate(this.name, this.status);
            throw new DriverException(e);
        }
    }

    private String readContents(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String read;
        while ((read = br.readLine()) != null) {
            sb.append(read).append((char) 10);
        }
        return sb.toString();
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
    public synchronized void dispose() {
        running = false;
        for (IScriptExecutor executor : runningExecutors.values()) {
            executor.abort();
        }
        runningExecutors.clear();
        pendingAborts.clear();
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
        if (!running) {
            throw new ActivityHandlingException("Driver not running");
        }
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
    public synchronized void abortActivity(int activityId, IUniqueId activityOccurrenceId) {
        // abort execution/evaluation (if possible), execution stage shall be reported as FATAL
        Pair<Integer, IUniqueId> key = Pair.of(activityId, activityOccurrenceId);
        IScriptExecutor exec = runningExecutors.get(key);
        if (exec == null) {
            pendingAborts.add(key);
        } else {
            exec.abort();
        }
    }

    private void execute(IActivityHandler.ActivityInvocation activityInvocation, IProcessingModel model) {
        try {
            // Record verification
            announce(activityInvocation, model, Instant.now(), ActivityReportState.PENDING, null, ActivityOccurrenceState.EXECUTION);
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
            Object result;
            IScriptExecutor exec = buildScriptExecutor(activityInvocation, fileName, contents);
            // Execute the script and retrieve result
            registerExecution(activityInvocation, exec);
            result = exec.execute();
            deregisterExecution(activityInvocation);
            // Report final state
            announce(activityInvocation, model, Instant.now(), ActivityReportState.OK, result, ActivityOccurrenceState.VERIFICATION);
        } catch (Exception e) {
            deregisterExecution(activityInvocation);
            LOG.log(Level.SEVERE, "Execution of procedure " + activityInvocation.getActivityOccurrenceId() + " failed: " + e.getMessage(), e);
            announce(activityInvocation, model, Instant.now(), ActivityReportState.FATAL, null, ActivityOccurrenceState.EXECUTION);
        }
    }

    private IScriptExecutor buildScriptExecutor(ActivityInvocation activityInvocation, String fileName, String contents) throws ActivityHandlingException {
        if (fileName.endsWith(Constants.JS_EXTENSION)) {
            return new JavascriptExecutor(this.dataSubscriptionManager, this.context, this.jsApiData, contents, activityInvocation, fileName);
        } else if (fileName.endsWith(Constants.GROOVY_EXTENSION)) {
            return new GroovyExecutor(this.dataSubscriptionManager, this.context, this.groovyApiData, contents, activityInvocation, fileName);
        } else if (fileName.endsWith(Constants.PYTHON_EXTENSION)) {
            return new PythonExecutor(this.dataSubscriptionManager, this.context, this.pythonApiData, contents, activityInvocation, fileName);
        } else {
            throw new ActivityHandlingException("Script type of " + fileName + " not supported: extension not recognized");
        }
    }

    private synchronized void deregisterExecution(ActivityInvocation activityInvocation) {
        Pair<Integer, IUniqueId> key = Pair.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId());
        pendingAborts.remove(key);
        runningExecutors.remove(key);
    }

    private synchronized void registerExecution(ActivityInvocation activityInvocation, IScriptExecutor exec) throws IllegalStateException {
        Pair<Integer, IUniqueId> key = Pair.of(activityInvocation.getActivityId(), activityInvocation.getActivityOccurrenceId());
        if (pendingAborts.contains(key)) {
            throw new IllegalStateException("Activity ID " + key.getSecond() + " pending abort: execution aborted");
        } else {
            pendingAborts.add(key);
            runningExecutors.put(key, exec);
        }
    }

    protected static void announce(ActivityInvocation invocation, IProcessingModel model, Instant genTime, ActivityReportState reportState, Object result, ActivityOccurrenceState nextState) {
        model.reportActivityProgress(ActivityProgress.of(invocation.getActivityId(), invocation.getActivityOccurrenceId(), AutomationDriver.EXECUTION_STAGE, genTime, ActivityOccurrenceState.EXECUTION, genTime, reportState, nextState, result));
    }

    @Override
    public List<DebugInformation> currentDebugInfo() {
        // Not needed
        return Collections.emptyList();
    }
}
