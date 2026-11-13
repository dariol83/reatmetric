/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.automation.js.internal;

import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.core.api.IOperationalMessageBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.automation.base.common.AutomationConstants;
import eu.dariolucia.reatmetric.driver.automation.js.common.Constants;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.Test;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JavascriptExecutorTest {

    private static final Instant GENERATION_TIME = Instant.parse("2026-11-13T10:00:00Z");
    private static final String SCRIPT_FILE = "executor-test.js";

    @Test
    void bindsActivityArgumentsAndExcludesTheScriptFileName() throws Exception {
        Fixture fixture = new Fixture();
        JavascriptExecutor executor = fixture.executor(
                "label + ':' + (count + 1) + ':' + enabled + ':' + typeof FILENAME",
                Map.of("label", "run", "count", 7, "enabled", true,
                        AutomationConstants.ARGUMENT_FILE_NAME, "private-script-name.js"));

        assertEquals("run:8:true:undefined", executor.execute());
    }

    @Test
    void permitsPublicJavaHostMethodCallsFromAnArgument() throws Exception {
        HostCallbacks host = new HostCallbacks();

        assertEquals("host:value", new Fixture().executor("host.describe('value')", Map.of("host", host)).execute());
        assertEquals(1, host.calls.get());
    }

    @Test
    void returnsAJavaStringThatRemainsUsableAfterTheContextCloses() throws Exception {
        Object result = new Fixture().executor("'ready'.toUpperCase()", Map.of()).execute();

        assertInstanceOf(String.class, result);
        assertEquals("READY", result);
    }

    @Test
    void convertsBooleanIntegerAndFractionalResultsToJavaValues() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals(Boolean.TRUE, fixture.executor("3 < 4", Map.of()).execute());
        assertEquals(42, assertInstanceOf(Number.class, fixture.executor("6 * 7", Map.of()).execute()).intValue());
        assertEquals(10.5, assertInstanceOf(Number.class, fixture.executor("21 / 2", Map.of()).execute()).doubleValue());
    }

    @Test
    void convertsJavascriptNullAndUndefinedToJavaNull() throws Exception {
        Fixture fixture = new Fixture();

        assertNull(fixture.executor("null", Map.of()).execute());
        assertNull(fixture.executor("void 0", Map.of()).execute());
    }

    @Test
    void bundledLoggingHelpersCallTheRealScriptManagerAndMessageBroker() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals("done", fixture.executor("info('started'); warning('watch'); alarm('stop'); 'done'", Map.of()).execute());
        assertEquals(List.of(
                new LoggedMessage("Script", "started", SCRIPT_FILE, Severity.INFO, true),
                new LoggedMessage("Script", "watch", SCRIPT_FILE, Severity.WARN, true),
                new LoggedMessage("Script", "stop", SCRIPT_FILE, Severity.ALARM, true)), fixture.messages);
    }

    @Test
    void bundledLookupHelpersReturnRealProcessingDataAndNullWhenAbsent() throws Exception {
        Fixture fixture = new Fixture();
        String script = "var p = parameter('ROOT.PARAM'); var e = event('ROOT.EVENT'); " +
                "p.getSourceValue() == 5 && p.getEngValue() == 10 && p.getValidity().name() == 'VALID' " +
                "&& e.getQualifier() == 'ready' && e.getSeverity().name() == 'INFO' " +
                "&& event('ROOT.MISSING') === null";

        assertEquals(Boolean.TRUE, fixture.executor(script, Map.of()).execute());
        assertEquals(List.of("ROOT.PARAM", "ROOT.EVENT", "ROOT.MISSING"), fixture.requestedPaths);
    }

    @Test
    void bundledInjectionHelperPassesTheValueToTheProcessingModel() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals(Boolean.TRUE, fixture.executor("inject_parameter('ROOT.PARAM', 37)", Map.of()).execute());
        assertEquals(1, fixture.injectedSamples.size());
        ParameterSample sample = fixture.injectedSamples.get(0);
        assertEquals(100, sample.getId());
        assertEquals(37, assertInstanceOf(Number.class, sample.getValue()).intValue());
    }

    @Test
    void wrapsJavascriptSyntaxErrorsAsScriptExceptions() throws IOException {
        JavascriptExecutor executor = new Fixture().executor("function (", Map.of());

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("SyntaxError"), exception::getMessage);
    }

    @Test
    void preservesTheMessageOfAJavascriptRuntimeError() throws IOException {
        JavascriptExecutor executor = new Fixture().executor("throw new Error('mission failed')", Map.of());

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("mission failed"), exception::getMessage);
    }

    @Test
    void abortBeforeExecutionReportsAScriptExceptionWithoutCallingTheHost() throws IOException {
        HostCallbacks host = new HostCallbacks();
        JavascriptExecutor executor = new Fixture().executor("host.describe('never')", Map.of("host", host));
        executor.abort();

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertTrue(exception.getMessage().contains("aborted"), exception::getMessage);
        assertEquals(0, host.calls.get());
    }

    @Test
    void abortCancelsAnActiveScriptWithinABoundedTime() throws Exception {
        HostCallbacks host = new HostCallbacks();
        JavascriptExecutor executor = new Fixture().executor("host.awaitRelease(); while (true) {}", Map.of("host", host));
        ExecutorService workers = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "javascript-executor-test");
            thread.setDaemon(true);
            return thread;
        });
        Future<Object> execution = workers.submit(executor::execute);
        Future<?> abortion = null;
        try {
            assertTrue(host.entered.await(15, TimeUnit.SECONDS), "Script did not enter the host callback");
            abortion = workers.submit(executor::abort);
            host.release.countDown();

            ExecutionException exception = assertThrows(ExecutionException.class,
                    () -> execution.get(15, TimeUnit.SECONDS));

            ScriptException scriptException = assertInstanceOf(ScriptException.class, exception.getCause());
            PolyglotException cancellation = assertInstanceOf(PolyglotException.class, scriptException.getCause());
            assertTrue(cancellation.isCancelled(), "Engine must report cancellation of the active script");
            abortion.get(15, TimeUnit.SECONDS);
        } finally {
            host.release.countDown();
            if (abortion == null) {
                abortion = workers.submit(executor::abort);
            }
            try {
                abortion.get(15, TimeUnit.SECONDS);
            } finally {
                execution.cancel(true);
                workers.shutdownNow();
            }
        }
    }

    private static String bundledInitScript() throws IOException {
        try (InputStream stream = JavascriptExecutorTest.class.getResourceAsStream("/" + Constants.API_JS_RESOURCE_FILE)) {
            assertNotNull(stream, "Bundled JavaScript API resource is missing");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private record LoggedMessage(String id, String message, String source, Severity severity, boolean store) {
    }

    private static final class Fixture {
        private final List<LoggedMessage> messages = new ArrayList<>();
        private final List<String> requestedPaths = new ArrayList<>();
        private final List<ParameterSample> injectedSamples = new ArrayList<>();
        private final ParameterData parameter = new ParameterData(new LongUniqueId(1), GENERATION_TIME, 100,
                "PARAM", SystemEntityPath.fromString("ROOT.PARAM"), 10L, 5L, "TM", Validity.VALID,
                AlarmState.NOMINAL, null, GENERATION_TIME, null);
        private final EventData event = new EventData(new LongUniqueId(2), GENERATION_TIME, 101,
                "EVENT", SystemEntityPath.fromString("ROOT.EVENT"), "ready", "DEVICE", "TM", "device",
                Severity.INFO, null, null, GENERATION_TIME, null);
        private final IOperationalMessageBroker broker = (id, message, source, severity, extension, entity, store) -> {
            messages.add(new LoggedMessage(id, message, source, severity, store));
            return null;
        };
        private final IProcessingModel processing = proxy(IProcessingModel.class, (instance, method, arguments) -> {
            if (method.getName().equals("getExternalIdOf")) {
                assertEquals(parameter.getPath(), arguments[0]);
                return parameter.getExternalId();
            }
            if (method.getName().equals("injectParameters")) {
                for (Object sample : (List<?>) arguments[0]) {
                    injectedSamples.add((ParameterSample) sample);
                }
                return null;
            }
            if (method.getName().equals("get") && arguments.length == 1) {
                if (arguments[0] instanceof ParameterDataFilter filter) {
                    requestedPaths.add(filter.getParameterPathList().iterator().next().asString());
                    return filter.test(parameter) ? List.of(parameter) : List.of();
                }
                if (arguments[0] instanceof EventDataFilter filter) {
                    requestedPaths.add(filter.getEventPathList().iterator().next().asString());
                    return filter.test(event) ? List.of(event) : List.of();
                }
            }
            throw new AssertionError("Unexpected processing-model call: " + method);
        });
        private final IServiceCoreContext context = proxy(IServiceCoreContext.class, (instance, method, arguments) -> {
            return switch (method.getName()) {
                case "getProcessingModel" -> processing;
                case "getOperationalMessageBroker" -> broker;
                default -> throw new AssertionError("Unexpected core-context call: " + method);
            };
        });

        private JavascriptExecutor executor(String script, Map<String, Object> arguments) throws IOException {
            IActivityHandler.ActivityInvocation invocation = new IActivityHandler.ActivityInvocation(
                    new LongUniqueId(3), 200, GENERATION_TIME, SystemEntityPath.fromString("ROOT.SCRIPT"),
                    Constants.T_SCRIPT_TYPE, arguments, Map.of(), Constants.AUTOMATION_ROUTE, "test");
            return new JavascriptExecutor(null, context, bundledInitScript(), script, invocation, SCRIPT_FILE);
        }
    }

    public static final class HostCallbacks {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        public String describe(String value) {
            calls.incrementAndGet();
            return "host:" + value;
        }

        public void awaitRelease() throws InterruptedException {
            entered.countDown();
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test did not release the host callback");
            }
        }
    }
}
