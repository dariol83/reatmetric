package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public abstract class AbstractHttpRequestHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(AbstractHttpRequestHandler.class.getName());

    public static final String TEXT_PLAIN_CHARSET_UTF_8 = "application/json";
    public static final String SUBSCRIPTION_KEY_PROPERTY = "key";

    public static final int HTTP_CODE_OK = 200;
    public static final int HTTP_CODE_NOT_FOUND = 404;
    public static final int HTTP_CODE_BAD_REQUEST = 400;
    public static final int HTTP_CODE_NOT_ACCEPTABLE = 406;
    public static final int HTTP_CODE_INTERNAL_ERROR = 500;

    public static final String HTTP_METHOD_OPTIONS = "OPTIONS";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_PATH_SEPARATOR = "/";

    public static final int SUBSCRIPTION_EXPIRATION_TIME = 60000; // 60 seconds

    private final HttpServerDriver driver;

    public AbstractHttpRequestHandler(HttpServerDriver driver) {
        this.driver = driver;
    }

    public abstract void cleanup();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // First of all, check if it is an OPTIONS. If so, return the CORS headers
        if(exchange.getRequestMethod().equals(HTTP_METHOD_OPTIONS)) {
            sendPositiveResponse(exchange, new byte[0], true);
            return;
        }
        int handled = doHandle(exchange);

        if(handled != HTTP_CODE_OK) {
            sendNegativeResponse(exchange, handled);
        }
    }

    protected abstract int doHandle(HttpExchange exchange) throws IOException;

    protected final void sendPositiveResponse(HttpExchange exchange, byte[] body) throws IOException {
        sendPositiveResponse(exchange, body, false);
    }

    protected final void sendPositiveResponse(HttpExchange exchange, byte[] body, boolean isOptions) throws IOException {
        addCORSHeaderProperties(exchange, isOptions);
        if(!isOptions) {
            exchange.getResponseHeaders().put("Content-Type", List.of(TEXT_PLAIN_CHARSET_UTF_8));
        }
        exchange.sendResponseHeaders(HTTP_CODE_OK, body.length);
        exchange.getResponseBody().write(body);
        exchange.getResponseBody().close();
    }

    protected final void sendNegativeResponse(HttpExchange exchange, int errorCode) throws IOException {
        addCORSHeaderProperties(exchange, false);
        exchange.sendResponseHeaders(errorCode, -1);
        exchange.getResponseBody().close();
    }

    protected final void addCORSHeaderProperties(HttpExchange exchange, boolean isOptions) {
        exchange.getResponseHeaders().put("Access-Control-Allow-Origin", List.of("*"));
        exchange.getResponseHeaders().put("Access-Control-Allow-Credentials", List.of("true"));
        exchange.getResponseHeaders().put("Access-Control-Allow-Methods",
                List.of(HTTP_METHOD_POST, HTTP_METHOD_GET, HTTP_METHOD_DELETE, HTTP_METHOD_OPTIONS));
        if(isOptions) {
            exchange.getResponseHeaders().put("Allow",
                    List.of(HTTP_METHOD_POST, HTTP_METHOD_GET, HTTP_METHOD_DELETE, HTTP_METHOD_OPTIONS));
        }
        exchange.getResponseHeaders().put("Access-Control-Allow-Headers", List.of("Origin", "Content-Type"));
    }

    protected final HttpServerDriver getDriver() {
        return driver;
    }

    public abstract void dispose();

    protected final void cleanSubscriptions(Map<String, ? extends AbstractHttpSubscription> map) {
        // Iterate on all subscriptions, and check if the last access time is more than SUBSCRIPTION_EXPIRATION_TIME older
        Set<String> keys = new TreeSet<>(map.keySet());
        Instant limit = Instant.now().minus(SUBSCRIPTION_EXPIRATION_TIME, ChronoUnit.MILLIS);
        for(String k : keys) {
            AbstractHttpSubscription s = map.get(k);
            if(s.getLastAccess().isBefore(limit)) {
                map.remove(k);
                s.dispose();
            }
        }
    }
}
