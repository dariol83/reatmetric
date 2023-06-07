package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.activity.ActivityDescriptor;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceDataFilter;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.AbstractHttpSubscription;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.HttpActivitySubscription;

import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class ActivityRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(ActivityRequestHandler.class.getName());

    private static final String ACTIVITY_OCCURRENCE_KEY_PROPERTY = "id";

    private final Map<String, HttpActivitySubscription> id2activitySubscription = new ConcurrentHashMap<>();

    public ActivityRequestHandler(HttpServerDriver driver) {
        super(driver);
    }

    @Override
    public void cleanup() {
        cleanSubscriptions(this.id2activitySubscription);
    }

    @Override
    public int doHandle(HttpExchange exchange) throws IOException {
        int handled = HTTP_CODE_NOT_FOUND;

        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH + HTTP_PATH_SEPARATOR).length());
            // Activity request
            if(path.startsWith(LIST_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Return all defined activities
                handled = handleActivityListGetRequest(exchange);
            } else if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Request to register a new update page
                handled = handleActivityRegistrationRequest(exchange);
            } else if(path.endsWith(INVOKE_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Request to invoke an activity
                handled = handleActivityInvokeRequest(exchange);
            } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                handled = handleActivityGetRequest(exchange);
            } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                handled = handleActivityDeregistrationRequest(exchange);
            } else if(path.startsWith(RETRIEVE_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Retrieve requested activities from archive
                handled = handleActivityRetrieveRequest(exchange);
            }
        }
        return handled;
    }

    private int handleActivityRetrieveRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ActivityOccurrenceDataFilter filter = JsonParseUtil.parseActivityOccurrenceDataFilter(exchange.getRequestBody());
        // Retrieve the retrieval properties from the request
        Map<String, String> requestParams = JsonParseUtil.splitQuery(exchange.getRequestURI());
        // Perform the retrieval
        try {
            Instant starttime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(START_TIME_ARG)));
            Instant endtime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(END_TIME_ARG)));
            List<ActivityOccurrenceData> data = getDriver().getContext().getServiceFactory().getActivityOccurrenceDataMonitorService().retrieve(starttime, endtime, filter);
            // Format the updates
            byte[] body = JsonParseUtil.formatActivities(data);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Error while processing request handleActivityRetrieveRequest(): " + e.getMessage(), e);
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleActivityInvokeRequest(HttpExchange exchange) throws IOException {
        // Retrieve the request from the body
        // The integer arguments must be "converted" from JSON arguments to what ReatMetric expects as type. This means
        // that the activity descriptor must be used to "sanitize" the constructed ActivityRequest
        Map<String, ActivityDescriptor> path2descriptor = getDriver().getActivityMap();
        ActivityRequest request = JsonParseUtil.parseActivityRequest(exchange.getRequestBody(), path2descriptor);
        // Start activity
        try {
            IUniqueId activityId = getDriver().getContext().getServiceFactory().getActivityExecutionService().startActivity(request);
            // Return a response with the UUID linked to the manager
            byte[] body = JsonParseUtil.format(ACTIVITY_OCCURRENCE_KEY_PROPERTY, activityId.toString());
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (ReatmetricException | RemoteException e) {
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleActivityListGetRequest(HttpExchange exchange) throws IOException {
        // Fetch the descriptors
        List<ActivityDescriptor> descriptors = getDriver().getActivityList();
        // Format the updates
        byte[] body = JsonParseUtil.formatActivityDescriptors(descriptors);
        // Send the response
        sendPositiveResponse(exchange, body);
        return HTTP_CODE_OK;
    }

    private int handleActivityDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpActivitySubscription s = this.id2activitySubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + uuid);
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + uuid);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleActivityGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpActivitySubscription s = this.id2activitySubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<ActivityOccurrenceData> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatActivities(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleActivityRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        ActivityOccurrenceDataFilter filter = JsonParseUtil.parseActivityOccurrenceDataFilter(exchange.getRequestBody());
        // Create an activity state subscription manager
        HttpActivitySubscription s = new HttpActivitySubscription(filter, getDriver());
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2activitySubscription.put(key.toString(), s);
            // Register the new key to the server
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + ACTIVITIES_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + key.toString(), this);

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
        this.id2activitySubscription.values().forEach(AbstractHttpSubscription::dispose);
        this.id2activitySubscription.clear();
    }
}
