package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.transport.ITransportConnector;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.CONNECTORS_PATH;

public class ConnectorRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(ConnectorRequestHandler.class.getName());

    private static final String PROPERTIES_PATH = "properties";
    private static final String CONNECT_PATH = "connect";
    private static final String DISCONNECT_PATH = "disconnect";
    private static final String ABORT_PATH = "abort";
    private static final String INITIALISE_PATH = "initialise";
    private static final String RECONNECT_PATH = "reconnect";

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
                // If the path includes also /properties, then return the properties of the connector
                handled = handleConnectorGetRequest(path, exchange);
            } else if(!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Operation on connector: path contains the name of the connector + / + connect, disconnect, reconnect, initialise, abort
                handled = handleConnectorActionRequest(path, exchange);
            }
        }
        return handled;
    }

    private int handleConnectorActionRequest(String path, HttpExchange exchange) throws IOException {
        String connector = path.substring(0, path.lastIndexOf(HTTP_PATH_SEPARATOR));
        String action = path.substring(path.lastIndexOf(HTTP_PATH_SEPARATOR) + 1);

        try {
            ITransportConnector theConnector = null;
            List<ITransportConnector> connectors = getDriver().getContext().getServiceFactory().getTransportConnectors();
            for (ITransportConnector c : connectors) {
                if (c.getName().equals(connector)) {
                    theConnector = c;
                    break;
                }
            }

            if(theConnector != null) {
                if(action.equals(CONNECT_PATH)) {
                    theConnector.connect();
                } else if(action.equals(DISCONNECT_PATH)) {
                    theConnector.disconnect();
                } else if(action.equals(ABORT_PATH)) {
                    theConnector.abort();
                } else if(action.equals(RECONNECT_PATH)) {
                    boolean activate = JsonParseUtil.parseInput(exchange.getRequestBody(), Boolean.class);
                    theConnector.setReconnect(activate);
                } else if(action.equals(INITIALISE_PATH)) {
                    Map<String, Object> properties = JsonParseUtil.parseMapInput(exchange.getRequestBody());
                    theConnector.initialise(properties);
                } else {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Connector POST request for path " + path + " issue: action not recognised");
                    }
                    return HTTP_CODE_NOT_FOUND;
                }

                // Send the response
                sendPositiveResponse(exchange, new byte[0]);
                return HTTP_CODE_OK;
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Connector POST request for path " + path + " issue: connector not found");
                }
                return HTTP_CODE_NOT_FOUND;
            }
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Connector POST request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleConnectorGetRequest(String path, HttpExchange exchange) throws IOException {
        if(path.endsWith(HTTP_PATH_SEPARATOR + PROPERTIES_PATH)) {
            String connector = path.substring(0, path.lastIndexOf(HTTP_PATH_SEPARATOR));
            try {
                List<ITransportConnector> connectors = getDriver().getContext().getServiceFactory().getTransportConnectors();
                Map<String, Object[]> string2descTypeValue = new LinkedHashMap<>();
                for (ITransportConnector c : connectors) {
                    if (c.getName().equals(connector)) {
                        Map<String, Pair<String, ValueTypeEnum>> supportedProps = c.getSupportedProperties();
                        Map<String, Object> currentValues = c.getCurrentProperties();
                        merge(string2descTypeValue, supportedProps, currentValues);
                        break;
                    }
                }
                // Format the response
                byte[] body = JsonParseUtil.formatConnectorProperties(string2descTypeValue);
                // Send the response
                sendPositiveResponse(exchange, body);
                return HTTP_CODE_OK;
            } catch (RemoteException | ReatmetricException e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Connector GET request for path " + path + " exception: " + e.getMessage(), e);
                }
                return HTTP_CODE_NOT_FOUND;
            }
        } else {
            try {
                List<ITransportConnector> connectors = getDriver().getContext().getServiceFactory().getTransportConnectors();
                Pair<TransportStatus, String> theConnector = null;
                for (ITransportConnector c : connectors) {
                    if (c.getName().equals(path)) {
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
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Connector GET request for path " + path + " exception: " + e.getMessage(), e);
                }
                return HTTP_CODE_NOT_FOUND;
            }
        }
    }

    private void merge(Map<String, Object[]> string2descTypeValue, Map<String, Pair<String, ValueTypeEnum>> supportedProps, Map<String, Object> currentValues) {
        for(String p : supportedProps.keySet()) {
            Pair<String, ValueTypeEnum> pdef = supportedProps.get(p);
            Object val = currentValues.get(p);
            string2descTypeValue.put(p, new Object[] { pdef.getFirst(), pdef.getSecond(), val});
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
