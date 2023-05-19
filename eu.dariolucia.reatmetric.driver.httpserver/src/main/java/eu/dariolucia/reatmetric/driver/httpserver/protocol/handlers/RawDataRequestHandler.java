package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.AbstractHttpSubscription;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.HttpRawDataSubscription;

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

public class RawDataRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(RawDataRequestHandler.class.getName());

    private final Map<String, HttpRawDataSubscription> id2rawDataSubscription = new ConcurrentHashMap<>();

    public RawDataRequestHandler(HttpServerDriver driver) {
        super(driver);
    }

    @Override
    public void cleanup() {
        cleanSubscriptions(this.id2rawDataSubscription);
    }

    @Override
    public int doHandle(HttpExchange exchange) throws IOException {
        int handled = HTTP_CODE_NOT_FOUND;

        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH)) {
            // Shorten the path to avoid potential matches with the system name
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH + HTTP_PATH_SEPARATOR).length());
            // Raw data request
            if(path.endsWith(REGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Request to register a new update page
                handled = handleRawDataRegistrationRequest(exchange);
            } else if(path.contains(HttpServerDriver.GET_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                handled = handleRawDataGetRequest(exchange);
            } else if(path.contains(HttpServerDriver.DEREGISTRATION_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                handled = handleRawDataDeregistrationRequest(exchange);
            } else if(path.startsWith(RETRIEVE_URL) && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Retrieve requested events from archive
                handled = handleRawDataRetrieveRequest(exchange);
            }
        }
        return handled;
    }

    private int handleRawDataRetrieveRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        RawDataFilter filter = JsonParseUtil.parseRawDataFilter(exchange.getRequestBody());
        // Retrieve the retrieval properties from the request
        Map<String, String> requestParams = JsonParseUtil.splitQuery(exchange.getRequestURI());
        // Perform the retrieval
        try {
            Instant starttime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(START_TIME_ARG)));
            Instant endtime = Instant.ofEpochMilli(Long.parseLong(requestParams.get(END_TIME_ARG)));
            List<RawData> data = getDriver().getContext().getServiceFactory().getRawDataMonitorService().retrieve(starttime, endtime, filter);
            // Format the updates
            byte[] body = JsonParseUtil.formatRawDatas(data);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (ReatmetricException | RemoteException e) {
            LOG.log(Level.SEVERE, "Error while processing request handleRawDataRetrieveRequest(): " + e.getMessage(), e);
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleRawDataDeregistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpRawDataSubscription s = this.id2rawDataSubscription.remove(uuid);
        if(s != null) {
            // Dispose the subscription and remove it from the map
            s.dispose();
            // Deregister the new key to the server
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + uuid);
            getDriver().getServer().removeContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + uuid);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleRawDataGetRequest(HttpExchange exchange) throws IOException {
        // Retrieve the UUID from the request path and look for the subscription object
        String path = exchange.getRequestURI().getPath();
        String uuid = path.substring(path.lastIndexOf('/') + 1);
        HttpRawDataSubscription s = this.id2rawDataSubscription.get(uuid);
        if(s != null) {
            // Fetch the updates since the last time
            List<RawData> updates = s.getUpdates();
            // Format the updates
            byte[] body = JsonParseUtil.formatRawDatas(updates);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleRawDataRegistrationRequest(HttpExchange exchange) throws IOException {
        // Retrieve the filter from the body
        RawDataFilter filter = JsonParseUtil.parseRawDataFilter(exchange.getRequestBody());
        // Create a parameter state subscription manager
        HttpRawDataSubscription s = new HttpRawDataSubscription(filter, getDriver());
        boolean inited = s.initialise();
        if(inited) {
            // Register the parameter state subscription manager
            UUID key = s.getKey();
            this.id2rawDataSubscription.put(key.toString(), s);
            // Register the new key to the server
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH + HTTP_PATH_SEPARATOR + GET_URL + HTTP_PATH_SEPARATOR + key.toString(), this);
            getDriver().getServer().createContext(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + RAW_DATA_PATH + HTTP_PATH_SEPARATOR + DEREGISTRATION_URL + HTTP_PATH_SEPARATOR + key.toString(), this);

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
        this.id2rawDataSubscription.values().forEach(AbstractHttpSubscription::dispose);
        this.id2rawDataSubscription.clear();
    }
}
