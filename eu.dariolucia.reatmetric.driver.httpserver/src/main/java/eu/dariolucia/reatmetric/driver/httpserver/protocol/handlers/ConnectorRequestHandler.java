package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.CONNECTORS_PATH;
import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.MODEL_PATH;

public class ConnectorRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(ConnectorRequestHandler.class.getName());

    public ConnectorRequestHandler(HttpServerDriver driver) {
        super(driver);
    }

    @Override
    public void cleanup() {
        // Nothing to be done
    }

    @Override
    public int doHandle(HttpExchange exchange) throws IOException {
        int handled = HTTP_CODE_NOT_FOUND;

        // Get the requested URI and, on the basis of the URI, understand what has to be done
        String path = exchange.getRequestURI().getPath();
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + CONNECTORS_PATH)) {
            // Shorten the path
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + CONNECTORS_PATH).length());
            if(path.startsWith(HTTP_PATH_SEPARATOR)) {
                path = path.substring(HTTP_PATH_SEPARATOR.length());
            }
            if(path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Fetch all the connector descriptors
                handled = handleConnectorListGetRequest(exchange);
            } else if(!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Fetch the state of the specified connector: path contains the ID of the connector
                handled = handleConnectorGetRequest(path, exchange);
            } else if(!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Operation on connector: path contains the name of the connector + / + start, stop, reconnect, initialise, abort
                // TODO
            }
        }
        return handled;
    }

    private int handleConnectorGetRequest(String path, HttpExchange exchange) throws IOException {
        try {
            List<ITransportConnector> connectors = getDriver().getContext().getServiceFactory().getTransportConnectors();
            Pair<TransportStatus, String> theConnector = null;
            for(ITransportConnector c : connectors) {
                if(c.getName().equals(path)) {
                    theConnector = Pair.of(c.getLastTransportStatus(), c.getDescription());
                    break;
                }
            }
            // Format the response
            byte[] body = JsonParseUtil.formatConnector(theConnector);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Connector GET request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleConnectorListGetRequest(HttpExchange exchange) throws IOException {
        try {
            List<ITransportConnector> connectors = getDriver().getContext().getServiceFactory().getTransportConnectors();
            List<Pair<TransportStatus, String>> cntList = new ArrayList<>(connectors.size());
            for(ITransportConnector c : connectors) {
                cntList.add(Pair.of(c.getLastTransportStatus(), c.getDescription()));
            }
            // Format the response
            byte[] body = JsonParseUtil.formatConnectorList(cntList);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Connector list GET request exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_NOT_FOUND;
        }
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }
}
