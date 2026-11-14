/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.automation.python.internal;

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
import eu.dariolucia.reatmetric.api.processing.input.EventOccurrence;
import eu.dariolucia.reatmetric.core.api.IOperationalMessageBroker;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.automation.base.common.AutomationConstants;
import eu.dariolucia.reatmetric.driver.automation.python.common.Constants;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PythonExecutorTest {

    private static final Instant GENERATION_TIME = Instant.parse("2026-11-13T10:00:00Z");
    private static final String SCRIPT_FILE = "executor-test.py";

    @Test
    void bindsActivityArgumentsAndExcludesTheScriptFileName() throws Exception {
        PythonExecutor executor = new Fixture().executor(
                "_result = '%s:%d:%s:%s' % (label, count + 1, enabled, 'FILENAME' in globals())",
                Map.of("label", "run", "count", 7, "enabled", true,
                        AutomationConstants.ARGUMENT_FILE_NAME, "private-script-name.py"));

        assertEquals("run:8:True:False", executor.execute());
    }

    @Test
    void permitsPublicJavaHostMethodCallsFromAnArgument() throws Exception {
        HostCallbacks host = new HostCallbacks();

        assertEquals("host:value", new Fixture().executor("_result = host.describe('value')", Map.of("host", host)).execute());
        assertEquals(1, host.calls.get());
    }

    @Test
    void importsJavaTypesAndReturnsTheJavaObjectAfterInterpreterClosure() throws Exception {
        Object result = new Fixture().executor("from java.util import ArrayList\n" +
                "values = ArrayList()\nvalues.add(label)\n_result = values", Map.of("label", "ready")).execute();

        ArrayList<?> values = assertInstanceOf(ArrayList.class, result);
        assertEquals(List.of("ready"), values);
    }

    @Test
    void convertsResultScalarsToJavaValuesAndPreservesPythonTwoDivision() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals(Boolean.TRUE, fixture.executor("_result = 3 < 4", Map.of()).execute());
        assertEquals(42, assertInstanceOf(Number.class, fixture.executor("_result = 6 * 7", Map.of()).execute()).intValue());
        assertEquals(10, assertInstanceOf(Number.class, fixture.executor("_result = 21 / 2", Map.of()).execute()).intValue());
        assertEquals(10.5, assertInstanceOf(Number.class, fixture.executor("_result = 21 / 2.0", Map.of()).execute()).doubleValue());
        assertEquals("READY", fixture.executor("_result = 'ready'.upper()", Map.of()).execute());
    }

    @Test
    void returnsNullWhenThereIsNoResultOrTheResultIsNone() throws Exception {
        Fixture fixture = new Fixture();

        assertNull(fixture.executor("value = 42", Map.of()).execute());
        assertNull(fixture.executor("_result = None", Map.of()).execute());
    }

    @Test
    void bundledLoggingHelpersCallTheRealScriptManagerAndMessageBroker() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals("done", fixture.executor("info('started')\nwarning('watch')\nalarm('stop')\n_result = 'done'", Map.of()).execute());
        assertEquals(List.of(
                new LoggedMessage("Script", "started", SCRIPT_FILE, Severity.INFO, true),
                new LoggedMessage("Script", "watch", SCRIPT_FILE, Severity.WARN, true),
                new LoggedMessage("Script", "stop", SCRIPT_FILE, Severity.ALARM, true)), fixture.messages);
    }

    @Test
    void bundledLookupHelpersReturnProcessingDataAndNoneWhenAbsent() throws Exception {
        Fixture fixture = new Fixture();
        String script = "p = parameter('ROOT.PARAM')\ne = event('ROOT.EVENT')\n" +
                "_result = (p.getSourceValue() == 5 and p.getEngValue() == 10 " +
                "and p.getValidity().name() == 'VALID' and e.getQualifier() == 'ready' " +
                "and e.getSeverity().name() == 'INFO' and event('ROOT.MISSING') is None)";

        assertEquals(Boolean.TRUE, fixture.executor(script, Map.of()).execute());
        assertEquals(List.of("ROOT.PARAM", "ROOT.EVENT", "ROOT.MISSING"), fixture.requestedPaths);
    }

    @Test
    void bundledEventHelperPassesQualifierAndReportToTheProcessingModel() throws Exception {
        Fixture fixture = new Fixture();

        assertEquals(Boolean.TRUE, fixture.executor("_result = raise_event('ROOT.EVENT', 'ready', 'report')", Map.of()).execute());
        assertEquals(1, fixture.raisedEvents.size());
        EventOccurrence occurrence = fixture.raisedEvents.get(0);
        assertEquals(101, occurrence.getId());
        assertEquals("ready", occurrence.getQualifier());
        assertEquals("report", occurrence.getReport());
        assertEquals("ROOT.SCRIPT", occurrence.getSource());
    }

    @Test
    void wrapsPythonSyntaxErrorsAsScriptExceptions() throws IOException {
        PythonExecutor executor = new Fixture().executor("def broken(", Map.of());

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("SyntaxError"), exception::getMessage);
    }

    @Test
    void preservesTheMessageOfAPythonRuntimeError() throws IOException {
        PythonExecutor executor = new Fixture().executor("raise ValueError('mission failed')", Map.of());

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("mission failed"), exception::getMessage);
    }

    @Test
    void abortBeforeExecutionReportsAScriptExceptionWithoutCallingTheHost() throws IOException {
        HostCallbacks host = new HostCallbacks();
        PythonExecutor executor = new Fixture().executor("_result = host.describe('never')", Map.of("host", host));
        executor.abort();

        ScriptException exception = assertThrows(ScriptException.class, executor::execute);

        assertTrue(exception.getMessage().contains("aborted"), exception::getMessage);
        assertEquals(0, host.calls.get());
    }

    private static String bundledInitScript() throws IOException {
        try (InputStream stream = PythonExecutorTest.class.getResourceAsStream("/" + Constants.API_PYTHON_RESOURCE_FILE)) {
            assertNotNull(stream, "Bundled Python API resource is missing");
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
        private final List<EventOccurrence> raisedEvents = new ArrayList<>();
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
                assertEquals(event.getPath(), arguments[0]);
                return event.getExternalId();
            }
            if (method.getName().equals("raiseEvent")) {
                raisedEvents.add((EventOccurrence) arguments[0]);
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

        private PythonExecutor executor(String script, Map<String, Object> arguments) throws IOException {
            IActivityHandler.ActivityInvocation invocation = new IActivityHandler.ActivityInvocation(
                    new LongUniqueId(3), 200, GENERATION_TIME, SystemEntityPath.fromString("ROOT.SCRIPT"),
                    Constants.T_SCRIPT_TYPE, arguments, Map.of(), Constants.AUTOMATION_ROUTE, "test");
            return new PythonExecutor(null, context, bundledInitScript(), script, invocation, SCRIPT_FILE);
        }
    }

    public static final class HostCallbacks {
        private final AtomicInteger calls = new AtomicInteger();

        public String describe(String value) {
            calls.incrementAndGet();
            return "host:" + value;
        }
    }
}
