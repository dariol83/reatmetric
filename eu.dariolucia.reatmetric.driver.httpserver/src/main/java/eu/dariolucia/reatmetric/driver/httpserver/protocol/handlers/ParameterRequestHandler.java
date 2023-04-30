package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class ParameterRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(ParameterRequestHandler.class.getName());

    private final Map<String, HttpParameterStateSubscription> id2parameterStateSubscription = new ConcurrentHashMap<>();
    private final Map<String, HttpParameterStreamSubscription> id2parameterStreamSubscription = new ConcurrentHashMap<>();

    public ParameterRequestHandler(HttpServerDriver driver) {
        super(driver);
    }

    @Override
    public void cleanup() {
        cleanSubscriptions(this.id2parameterStateSubscription);
        cleanSubscriptions(this.id2parameterStreamSubscription);
    }

    @Override
    public int doHandle(HttpExchange exchange) throws IOException {
        int handled = HTTP_CODE_NOT_FOUND;

        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR).length());
            // Parameter request
            if(path.startsWith(LIST_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Return all defined parameters
                handled = handleParameterListGetRequest(exchange);
            } else if(path.startsWith(PARAMETER_CURRENT_STATE_PATH)) {
                // Current state operation
                if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                    // Request to register a new update page
                    handled = handleParameterStateRegistrationRequest(exchange);
                } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                    handled = handleParameterStateGetRequest(exchange);
                } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                    handled = handleParameterStateDeregistrationRequest(exchange);
                }
            } else if(path.startsWith(PARAMETER_STREAM_PATH)) {
                // Current state operation
                if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                    // Request to register a new update page
                    handled = handleParameterStreamRegistrationRequest(exchange);
                } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                    handled = handleParameterStreamGetRequest(exchange);
                } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                    handled = handleParameterStreamDeregistrationRequest(exchange);
                }
            } else if(path.startsWith(RETRIEVE_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Retrieve requested parameters from archive
                handled = handleParameterRetrieveRequest(exchange);
            }
        }
        return handled;
    }

    private int handleParameterRetrieveRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ParameterDataFilter filter = JsonParseUtil.parseParameterDataFilter(exchange.getRequestBody());
        // Retrieve the retrieval properties from the request
        Map<String, String> requestParams = JsonParseUtil.splitQuery(exchange.getRequestURI());
        // Perform the retrieval
        try {
            Instant starttime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(START_TIME_ARG)));
            Instant endtime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(END_TIME_ARG)));
            List<ParameterData> data = getDriver().getContext().getServiceFactory().getParameterDataMonitorService().retrieve(starttime, endtime, filter);
            // Format the updates
            byte[] body = JsonParseUtil.formatParameters(data);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Error while processing request handleParameterRetrieveRequest(): " + e.getMessage(), e);
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleParameterListGetRequest(HttpExchange exchange) throws IOException {
        // Fetch the descriptors
        List<ParameterDescriptor> descriptors = getDriver().getParameterList();
        // Format the updates
        byte[] body = JsonParseUtil.formatParameterDescriptors(descriptors);
        // Send the response
        sendPositiveResponse(exchange, body);
        return HTTP_CODE_OK;
    }

    private int handleParameterStateDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStateSubscription s = this.id2parameterStateSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_CURRENT_STATE_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + uuid);
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_CURRENT_STATE_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + uuid);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleParameterStateGetRequest(HttpExchange exchange) throws IOException {
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
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleParameterStateRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ParameterDataFilter filter = JsonParseUtil.parseParameterDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpParameterStateSubscription s = new HttpParameterStateSubscription(filter, getDriver());
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2parameterStateSubscription.put(key.toString(), s);
            // Register the new key to the server
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_CURRENT_STATE_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_CURRENT_STATE_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(SUBSCRIPTION_KEY_PROPERTY, key.toString());
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }


    private int handleParameterStreamDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpParameterStreamSubscription s = this.id2parameterStreamSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_STREAM_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + uuid);
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_STREAM_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + uuid);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleParameterStreamGetRequest(HttpExchange exchange) throws IOException {
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
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleParameterStreamRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ParameterDataFilter filter = JsonParseUtil.parseParameterDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpParameterStreamSubscription s = new HttpParameterStreamSubscription(filter, getDriver());
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2parameterStreamSubscription.put(key.toString(), s);
            // Register the new key to the server
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_STREAM_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + PARAMETERS_PATH + HTTP_PATH_SEPARATOR + PARAMETER_STREAM_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
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
        this.id2parameterStateSubscription.values().forEach(AbstractHttpSubscription::dispose);
        this.id2parameterStateSubscription.clear();
        this.id2parameterStreamSubscription.values().forEach(AbstractHttpSubscription::dispose);
        this.id2parameterStreamSubscription.clear();
    }
}
