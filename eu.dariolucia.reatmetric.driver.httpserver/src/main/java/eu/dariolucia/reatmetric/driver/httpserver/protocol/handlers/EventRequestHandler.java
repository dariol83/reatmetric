package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.AbstractHttpSubscription;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.HttpEventSubscription;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class EventRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(EventRequestHandler.class.getName());

    private final Map<String, HttpEventSubscription> id2eventSubscription = new ConcurrentHashMap<>();

    public EventRequestHandler(HttpServerDriver driver) {
        super(driver);
    }

    @Override
    public void cleanup() {
        cleanSubscriptions(this.id2eventSubscription);
    }

    @Override
    public int doHandle(HttpExchange exchange) throws IOException {
        int handled = HTTP_CODE_NOT_FOUND;

        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH + HTTP_PATH_SEPARATOR).length());
            // Event request
            if(path.startsWith(LIST_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Return all defined events
                handled = handleEventListGetRequest(exchange);
            } else if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Request to register a new update page
                handled = handleEventRegistrationRequest(exchange);
            } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                handled = handleEventGetRequest(exchange);
            } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                handled = handleEventDeregistrationRequest(exchange);
            }
        }
        return handled;
    }

    private int handleEventListGetRequest(HttpExchange exchange) throws IOException {
        // Fetch the descriptors
        List<EventDescriptor> descriptors = getDriver().getEventList();
        // Format the updates
        byte[] body = JsonParseUtil.formatEventDescriptors(descriptors);
        // Send the response
        sendPositiveResponse(exchange, body);
        return HTTP_CODE_OK;
    }

    private int handleEventDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpEventSubscription s = this.id2eventSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + uuid);
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + uuid);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleEventGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpEventSubscription s = this.id2eventSubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<EventData> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatEvents(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleEventRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        EventDataFilter filter = JsonParseUtil.parseEventDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpEventSubscription s = new HttpEventSubscription(filter, getDriver());
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2eventSubscription.put(key.toString(), s);
            // Register the new key to the server
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + EVENTS_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + key.toString(), this);

            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    @Override
    public void dispose() {
        this.id2eventSubscription.values().forEach(AbstractHttpSubscription::dispose);
        this.id2eventSubscription.clear();
    }
}
