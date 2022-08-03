package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class HttpRequestHandler implements HttpHandler {

    // private static final String TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=UTF-8";
    private static final String TEXT_PLAIN_CHARSET_UTF_8 = "application/json";
    private static final String SUBSCRIPTION_KEY_PROPERTY = "key";

    private static final int HTTP_CODE_OK = 200;
    private static final int HTTP_CODE_NOT_FOUND = 404;

    private final HttpServerDriver driver;

    private final Map<String, HttpParameterStateSubscription> id2parameterStateSubscription = new ConcurrentHashMap<>();
    private final Map<String, HttpParameterStreamSubscription> id2parameterStreamSubscription = new ConcurrentHashMap<>();
    private final Map<String, HttpEventSubscription> id2eventSubscription = new ConcurrentHashMap<>();
    private final Map<String, HttpMessageSubscription> id2messageSubscription = new ConcurrentHashMap<>();

    public HttpRequestHandler(HttpServerDriver driver) {
        this.driver = driver;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        boolean handled = false;
        // First of all, check if it is an OPTIONS. If so, return the CORS headers
        if(exchange.getRequestMethod().equals("OPTIONS")) {
            sendPositiveResponse(exchange, new byte[0], true);
            return;
        }
        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring(("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/").length());
            // Parameter request
            if(path.startsWith(LIST_URL) && exchange.getRequestMethod().equals("GET")) {
                // Return all defined parameters
                handled = handleParameterListGetRequest(exchange);
            } else if(path.startsWith(PARAMETER_CURRENT_STATE_PATH)) {
                // Current state operation
                if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals("POST")) {
                    // Request to register a new update page
                    handled = handleParameterStateRegistrationRequest(exchange);
                } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals("GET")) {
                    handled = handleParameterStateGetRequest(exchange);
                } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals("DELETE")) {
                    handled = handleParameterStateDeregistrationRequest(exchange);
                }
            } else if(path.startsWith(PARAMETER_STREAM_PATH)) {
                // Current state operation
                if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals("POST")) {
                    // Request to register a new update page
                    handled = handleParameterStreamRegistrationRequest(exchange);
                } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals("GET")) {
                    handled = handleParameterStreamGetRequest(exchange);
                } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals("DELETE")) {
                    handled = handleParameterStreamDeregistrationRequest(exchange);
                }
            }
        } else if(path.startsWith("/" + this.driver.getSystemName() + "/" + EVENTS_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring(("/" + this.driver.getSystemName() + "/" + EVENTS_PATH + "/").length());
            // Event request
            if(path.startsWith(LIST_URL) && exchange.getRequestMethod().equals("GET")) {
                // Return all defined events
                handled = handleEventListGetRequest(exchange);
            } else if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals("POST")) {
                // Request to register a new update page
                handled = handleEventRegistrationRequest(exchange);
            } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals("GET")) {
                handled = handleEventGetRequest(exchange);
            } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals("DELETE")) {
                handled = handleEventDeregistrationRequest(exchange);
            }
        } else if(path.startsWith("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring(("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH + "/").length());
            // Event request
            if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals("POST")) {
                // Request to register a new update page
                handled = handleMessageRegistrationRequest(exchange);
            } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals("GET")) {
                handled = handleMessageGetRequest(exchange);
            } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals("DELETE")) {
                handled = handleMessageDeregistrationRequest(exchange);
            }
        }

        if(!handled) {
            sendNegativeResponse(exchange, HTTP_CODE_NOT_FOUND);
        }
    }

    private boolean handleEventListGetRequest(HttpExchange exchange) throws IOException {
        // Fetch the descriptors
        List<EventDescriptor> descriptors = this.driver.getEventList();
        // Format the updates
        byte[] body = JsonParseUtil.formatEventDescriptors(descriptors);
        // Send the response
        sendPositiveResponse(exchange, body);
        return true;
    }

    private boolean handleParameterListGetRequest(HttpExchange exchange) throws IOException {
        // Fetch the descriptors
        List<ParameterDescriptor> descriptors = this.driver.getParameterList();
        // Format the updates
        byte[] body = JsonParseUtil.formatParameterDescriptors(descriptors);
        // Send the response
        sendPositiveResponse(exchange, body);
        return true;
    }

    private boolean handleMessageDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpMessageSubscription s = this.id2messageSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH + "/" + GET_URL + "/" + uuid);
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH + "/" + DEREGISTRATION_URL + "/" + uuid);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return true;
        }
        return false;
    }

    private boolean handleMessageGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpMessageSubscription s = this.id2messageSubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<OperationalMessage> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatMessages(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return true;
        }
        return false;
    }

    private boolean handleMessageRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        OperationalMessageFilter filter = JsonParseUtil.parseOperationalMessageFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpMessageSubscription s = new HttpMessageSubscription(filter, this.driver);
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2messageSubscription.put(key.toString(), s);
            // Register the new key to the server
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH + "/" + GET_URL + "/" + key.toString(), this);
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + MESSAGES_PATH + "/" + DEREGISTRATION_URL + "/" + key.toString(), this);

            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
        }
        return inited;
    }

    private boolean handleEventDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpEventSubscription s = this.id2eventSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + EVENTS_PATH + "/" + GET_URL + "/" + uuid);
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + EVENTS_PATH + "/" + DEREGISTRATION_URL + "/" + uuid);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return true;
        }
        return false;
    }

    private boolean handleEventGetRequest(HttpExchange exchange) throws IOException {
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
            return true;
        }
        return false;
    }

    private boolean handleEventRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        EventDataFilter filter = JsonParseUtil.parseEventDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpEventSubscription s = new HttpEventSubscription(filter, this.driver);
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2eventSubscription.put(key.toString(), s);
            // Register the new key to the server
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + EVENTS_PATH + "/" + GET_URL + "/" + key.toString(), this);
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + EVENTS_PATH + "/" + DEREGISTRATION_URL + "/" + key.toString(), this);

            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
        }
        return inited;
    }

    private boolean handleParameterStateDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStateSubscription s = this.id2parameterStateSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + GET_URL + "/" + uuid);
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + DEREGISTRATION_URL + "/" + uuid);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return true;
        }
        return false;
    }

    private boolean handleParameterStateGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStateSubscription s = this.id2parameterStateSubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<ParameterData> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatParameters(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return true;
        }
        return false;
    }

    private boolean handleParameterStateRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ParameterDataFilter filter = JsonParseUtil.parseParameterDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpParameterStateSubscription s = new HttpParameterStateSubscription(filter, this.driver);
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2parameterStateSubscription.put(key.toString(), s);
            // Register the new key to the server
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + GET_URL + "/" + key.toString(), this);
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_CURRENT_STATE_PATH + "/" + DEREGISTRATION_URL + "/" + key.toString(), this);
            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
        }
        return inited;
    }


    private boolean handleParameterStreamDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStreamSubscription s = this.id2parameterStreamSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + GET_URL + "/" + uuid);
            this.driver.getServer().removeContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + DEREGISTRATION_URL + "/" + uuid);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return true;
        }
        return false;
    }

    private boolean handleParameterStreamGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStreamSubscription s = this.id2parameterStreamSubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<ParameterData> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatParameters(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return true;
        }
        return false;
    }

    private boolean handleParameterStreamRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ParameterDataFilter filter = JsonParseUtil.parseParameterDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpParameterStreamSubscription s = new HttpParameterStreamSubscription(filter, this.driver);
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2parameterStreamSubscription.put(key.toString(), s);
            // Register the new key to the server
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + GET_URL + "/" + key.toString(), this);
            this.driver.getServer().createContext("/" + this.driver.getSystemName() + "/" + PARAMETERS_PATH + "/" + PARAMETER_STREAM_PATH + "/" + DEREGISTRATION_URL + "/" + key.toString(), this);
            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
        }
        return inited;
    }

    private void sendPositiveResponse(HttpExchange exchange, byte[] body) throws IOException {
        sendPositiveResponse(exchange, body, false);
    }

    private void sendPositiveResponse(HttpExchange exchange, byte[] body, boolean isOptions) throws IOException {
        addCORSHeaderProperties(exchange, isOptions);
        if(!isOptions) {
            exchange.getResponseHeaders().put("Content-Type", List.of(TEXT_PLAIN_CHARSET_UTF_8));
        }
        exchange.sendResponseHeaders(HTTP_CODE_OK, body.length);
        exchange.getResponseBody().write(body);
        exchange.getResponseBody().close();
    }

    private void sendNegativeResponse(HttpExchange exchange, int errorCode) throws IOException {
        addCORSHeaderProperties(exchange, false);
        exchange.sendResponseHeaders(errorCode, -1);
        exchange.getResponseBody().close();
    }

    private void addCORSHeaderProperties(HttpExchange exchange, boolean isOptions) {
        exchange.getResponseHeaders().put("Access-Control-Allow-Origin", List.of("*"));
        exchange.getResponseHeaders().put("Access-Control-Allow-Credentials", List.of("true"));
        exchange.getResponseHeaders().put("Access-Control-Allow-Methods", List.of("POST", "GET", "DELETE", "OPTIONS"));
        if(isOptions) {
            exchange.getResponseHeaders().put("Allow", List.of("POST", "GET", "DELETE", "OPTIONS"));
        }
        exchange.getResponseHeaders().put("Access-Control-Allow-Headers", List.of("Origin", "Content-Type"));
    }

    public void dispose() {
        this.id2parameterStateSubscription.values().forEach(AbstractSubscriptionHandler::dispose);
        this.id2parameterStateSubscription.clear();
        this.id2eventSubscription.values().forEach(AbstractSubscriptionHandler::dispose);
        this.id2eventSubscription.clear();
        this.id2messageSubscription.values().forEach(AbstractSubscriptionHandler::dispose);
        this.id2messageSubscription.clear();
        this.id2parameterStreamSubscription.values().forEach(AbstractSubscriptionHandler::dispose);
        this.id2parameterStreamSubscription.clear();
    }
}
