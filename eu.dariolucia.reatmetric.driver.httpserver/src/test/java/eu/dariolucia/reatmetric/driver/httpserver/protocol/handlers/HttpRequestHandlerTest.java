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

package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.activity.ActivityArrayArgumentDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityPlainArgumentDescriptor;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.processing.input.ArrayActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.PlainActivityArgument;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestHandlerTest {

    private static final String ACTIVITY_BODY = """
            {
              "id": 42, "path": "ROOT.DEVICE.SET", "route": "route-1", "source": "opérateur",
              "arguments": [
                {"name": "count", "type": "plain", "value": 17, "engineering": false},
                {"name": "level", "type": "plain", "value": 23, "engineering": true},
                {"name": "mode", "type": "plain", "value": 3, "engineering": false},
                {"name": "records", "type": "array", "records": [
                  {"elements": [
                    {"name": "small", "type": "plain", "value": 7, "engineering": false},
                    {"name": "large", "type": "plain", "value": 4294967296, "engineering": false}
                  ]}
                ]}
              ],
              "properties": {"comment": "caffè 🚀"}
            }
            """;

    @Test
    void activityPostForwardsTypedArgumentsAndReturnsOccurrenceId() throws Exception {
        AtomicReference<ActivityRequest> received = new AtomicReference<>();
        AtomicInteger invocations = new AtomicInteger();
        IActivityExecutionService execution = service(IActivityExecutionService.class, "startActivity", (proxy, method, args) -> {
            invocations.incrementAndGet();
            received.set((ActivityRequest) args[0]);
            return new LongUniqueId(9001);
        });
        ActivityRequestHandler handler = new ActivityRequestHandler(driver("getActivityExecutionService", execution));
        TestExchange exchange = new TestExchange("POST", "/test/activities/invoke", ACTIVITY_BODY);

        handler.handle(exchange);

        assertEquals(1, invocations.get());
        assertNotNull(received.get());
        ActivityRequest request = received.get();
        assertEquals(42, request.getId());
        assertEquals(SystemEntityPath.fromString("ROOT.DEVICE.SET"), request.getPath());
        assertEquals("route-1", request.getRoute());
        assertEquals("opérateur", request.getSource());
        assertEquals(Map.of("comment", "caffè 🚀"), request.getProperties());
        assertEquals(4, request.getArguments().size());
        PlainActivityArgument count = assertInstanceOf(PlainActivityArgument.class, request.getArguments().get(0));
        assertFalse(count.isEngineering());
        assertEquals(17L, count.getRawValue());
        assertNull(count.getEngValue());
        PlainActivityArgument level = assertInstanceOf(PlainActivityArgument.class, request.getArguments().get(1));
        assertTrue(level.isEngineering());
        assertEquals(23L, level.getEngValue());
        assertNull(level.getRawValue());
        PlainActivityArgument mode = assertInstanceOf(PlainActivityArgument.class, request.getArguments().get(2));
        assertEquals(3, mode.getRawValue());
        ArrayActivityArgument records = assertInstanceOf(ArrayActivityArgument.class, request.getArguments().get(3));
        assertEquals(1, records.getRecords().size());
        List<?> elements = records.getRecords().getFirst().getElements();
        assertEquals(7L, assertInstanceOf(PlainActivityArgument.class, elements.get(0)).getRawValue());
        assertEquals(4294967296L, assertInstanceOf(PlainActivityArgument.class, elements.get(1)).getRawValue());
        assertEquals(200, exchange.getResponseCode());
        assertEquals("{ \"id\" : \"9001\" }", exchange.responseText());
        assertEquals("application/json", exchange.getResponseHeaders().getFirst("Content-Type"));
        assertEquals("*", exchange.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        assertTrue(exchange.responseClosed);
    }

    @Test
    void activityServiceFailureReturnsInternalError() throws Exception {
        IActivityExecutionService execution = service(IActivityExecutionService.class, "startActivity", (proxy, method, args) -> {
            throw new ReatmetricException("Service unavailable");
        });
        ActivityRequestHandler handler = new ActivityRequestHandler(driver("getActivityExecutionService", execution));
        TestExchange exchange = new TestExchange("POST", "/test/activities/invoke", ACTIVITY_BODY);

        handler.handle(exchange);

        assertEquals(500, exchange.getResponseCode());
        assertEquals("", exchange.responseText());
        assertEquals(-1L, exchange.responseLength);
        assertTrue(exchange.responseClosed);
    }

    @Test
    void parameterArchivePostForwardsFilterAndTimeRangeAndReturnsData() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = start.plusSeconds(60);
        ParameterData matching = parameter(12, "ROOT.DEVICE.TEMP", "route-1", Validity.VALID);
        AtomicInteger retrievals = new AtomicInteger();
        IParameterDataProvisionService parameters = service(IParameterDataProvisionService.class, "retrieve", (proxy, method, args) -> {
            retrievals.incrementAndGet();
            assertEquals(start, args[0]);
            assertEquals(end, args[1]);
            ParameterDataFilter filter = assertInstanceOf(ParameterDataFilter.class, args[2]);
            assertEquals(SystemEntityPath.fromString("ROOT.DEVICE"), filter.getParentPath());
            assertEquals(Set.of(SystemEntityPath.fromString("ROOT.DEVICE.TEMP")), filter.getParameterPathList());
            assertEquals(Set.of("route-1"), filter.getRouteList());
            assertEquals(Set.of(Validity.VALID), filter.getValidityList());
            assertEquals(Set.of(AlarmState.NOMINAL), filter.getAlarmStateList());
            assertEquals(Set.of(12), filter.getExternalIdList());
            assertTrue(filter.test(matching));
            assertFalse(filter.test(parameter(12, "ROOT.DEVICE.TEMP", "other-route", Validity.VALID)));
            assertFalse(filter.test(parameter(12, "ROOT.DEVICE.TEMP", "route-1", Validity.INVALID)));
            return List.of(matching);
        });
        ParameterRequestHandler handler = new ParameterRequestHandler(driver("getParameterDataMonitorService", parameters));
        TestExchange exchange = new TestExchange("POST", "/test/parameters/retrieve?startTime=" + start.toEpochMilli() + "&endTime=" + end.toEpochMilli(), """
                {
                  "parentPath": "ROOT.DEVICE", "parameterPathList": ["ROOT.DEVICE.TEMP"],
                  "routeList": ["route-1"], "validityList": ["VALID"],
                  "alarmStateList": ["NOMINAL"], "externalIdList": [12]
                }
                """);

        handler.handle(exchange);

        assertEquals(1, retrievals.get());
        assertEquals(200, exchange.getResponseCode());
        List<Map<String, Object>> response = JsonPath.read(exchange.responseText(), "$[*]");
        assertEquals(1, response.size());
        assertEquals(12, response.getFirst().get("externalId"));
        assertEquals("ROOT.DEVICE.TEMP", response.getFirst().get("path"));
        assertEquals(12.5, response.getFirst().get("eng"));
        assertEquals(125, response.getFirst().get("raw"));
        assertEquals("VALID", response.getFirst().get("validity"));
        assertEquals("NOMINAL", response.getFirst().get("alarm"));
        assertTrue(exchange.responseClosed);
    }

    @Test
    void optionsRequestReturnsCorsHeadersWithoutCallingServices() throws Exception {
        ActivityRequestHandler handler = new ActivityRequestHandler(driver("unused", null));
        TestExchange exchange = new TestExchange("OPTIONS", "/test/activities/invoke", "");

        handler.handle(exchange);

        assertEquals(200, exchange.getResponseCode());
        assertEquals(List.of("POST", "GET", "DELETE", "OPTIONS"), exchange.getResponseHeaders().get("Allow"));
        assertEquals(List.of("Origin", "Content-Type"), exchange.getResponseHeaders().get("Access-Control-Allow-Headers"));
        assertEquals("*", exchange.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("", exchange.responseText());
        assertTrue(exchange.responseClosed);
    }

    @Test
    void unknownActivityRouteReturnsNotFoundWithoutCallingServices() throws Exception {
        ActivityRequestHandler handler = new ActivityRequestHandler(driver("unused", null));
        TestExchange exchange = new TestExchange("GET", "/test/activities/unknown", "");

        handler.handle(exchange);

        assertEquals(404, exchange.getResponseCode());
        assertEquals(-1L, exchange.responseLength);
        assertEquals("", exchange.responseText());
        assertTrue(exchange.responseClosed);
    }

    private static ParameterData parameter(int id, String path, String route, Validity validity) {
        Instant time = Instant.parse("2026-01-01T00:00:30Z");
        return new ParameterData(new LongUniqueId(100), time, id, "TEMP", SystemEntityPath.fromString(path),
                12.5, 125L, route, validity, AlarmState.NOMINAL, null, time, null);
    }

    private static ActivityPlainArgumentDescriptor argument(String name, ValueTypeEnum raw, ValueTypeEnum engineering) {
        return new ActivityPlainArgumentDescriptor(name, name, raw, engineering, null,
                false, false, null, null, false, false, null, null);
    }

    private static HttpServerDriver driver(String factoryMethod, Object providedService) {
        ActivityDescriptor descriptor = new ActivityDescriptor(SystemEntityPath.fromString("ROOT.DEVICE.SET"), 42,
                "Set device", "route-1", "TEST", List.of(
                argument("count", ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.ENUMERATED),
                argument("level", ValueTypeEnum.ENUMERATED, ValueTypeEnum.UNSIGNED_INTEGER),
                argument("mode", ValueTypeEnum.ENUMERATED, ValueTypeEnum.ENUMERATED),
                new ActivityArrayArgumentDescriptor("records", "Records", "count", List.of(
                        argument("small", ValueTypeEnum.SIGNED_INTEGER, ValueTypeEnum.SIGNED_INTEGER),
                        argument("large", ValueTypeEnum.UNSIGNED_INTEGER, ValueTypeEnum.UNSIGNED_INTEGER)))),
                List.of(), Duration.ofSeconds(1));
        IReatmetricSystem factory = service(IReatmetricSystem.class, factoryMethod, (proxy, method, args) -> providedService);
        IServiceCoreContext context = service(IServiceCoreContext.class, "getServiceFactory", (proxy, method, args) -> factory);
        return new HttpServerDriver() {
            @Override
            public String getSystemName() {
                return "test";
            }

            @Override
            public IServiceCoreContext getContext() {
                return context;
            }

            @Override
            public Map<String, ActivityDescriptor> getActivityMap() {
                return Map.of(descriptor.getPath().asString(), descriptor);
            }
        };
    }

    private static <T> T service(Class<T> type, String expectedMethod, InvocationHandler implementation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            assertEquals(expectedMethod, method.getName(), "Unexpected service call");
            return implementation.invoke(proxy, method, args);
        }));
    }

    /** An exchange entirely in memory: tests exercise handlers without binding a server port. */
    private static final class TestExchange extends HttpExchange {
        private final String method;
        private final URI uri;
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final InputStream requestBody;
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream() {
            @Override
            public void close() {
                responseClosed = true;
            }
        };
        private int responseCode = -1;
        private long responseLength;
        private boolean responseClosed;

        private TestExchange(String method, String path, String body) {
            this.method = method;
            this.uri = URI.create(path);
            this.requestBody = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        private String responseText() {
            return responseBody.toString(StandardCharsets.UTF_8);
        }

        @Override public Headers getRequestHeaders() { return requestHeaders; }
        @Override public Headers getResponseHeaders() { return responseHeaders; }
        @Override public URI getRequestURI() { return uri; }
        @Override public String getRequestMethod() { return method; }
        @Override public InputStream getRequestBody() { return requestBody; }
        @Override public OutputStream getResponseBody() { return responseBody; }
        @Override public int getResponseCode() { return responseCode; }
        @Override public void sendResponseHeaders(int status, long length) {
            responseCode = status;
            responseLength = length;
        }
        @Override public void close() { responseClosed = true; }
        @Override public HttpContext getHttpContext() { throw new UnsupportedOperationException(); }
        @Override public InetSocketAddress getRemoteAddress() { throw new UnsupportedOperationException(); }
        @Override public InetSocketAddress getLocalAddress() { throw new UnsupportedOperationException(); }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public Object getAttribute(String name) { throw new UnsupportedOperationException(); }
        @Override public void setAttribute(String name, Object value) { throw new UnsupportedOperationException(); }
        @Override public void setStreams(InputStream input, OutputStream output) { throw new UnsupportedOperationException(); }
        @Override public HttpPrincipal getPrincipal() { throw new UnsupportedOperationException(); }
    }
}
