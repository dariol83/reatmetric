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
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.subscriptions.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class ModelRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(ModelRequestHandler.class.getName());

    public ModelRequestHandler(HttpServerDriver driver) {
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
        if(path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + MODEL_PATH)) {
            // Shorten the path
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + MODEL_PATH).length());
            if(path.startsWith(HTTP_PATH_SEPARATOR)) {
                path = path.substring(HTTP_PATH_SEPARATOR.length());
            }
            if(exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Fetch the descriptors
                // Return the system element descriptor and the children descriptors of the provided path
                handled = handleModelElementGetRequest(path, exchange);
            } else if(exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Operation on element
                handled = handleModelElementPostRequest(path, exchange);
            }
        }
        return handled;
    }

    private int handleModelElementPostRequest(String path, HttpExchange exchange) throws IOException {
        // Get the last part of the path
        String operation = path.substring(path.lastIndexOf(HTTP_PATH_SEPARATOR) + 1);
        String effectivePath = path.substring(0, path.lastIndexOf(HTTP_PATH_SEPARATOR));
        if(operation.equals("enable")) {
            try {
                setSystemElementEnablement(effectivePath.replace(HTTP_PATH_SEPARATOR, "."), true);
                sendPositiveResponse(exchange, new byte[0]);
                return HTTP_CODE_OK;
            } catch (ReatmetricException | RemoteException e) {
                return HTTP_CODE_INTERNAL_ERROR;
            }
        } else if(operation.equals("disable")) {
            try {
                setSystemElementEnablement(effectivePath.replace(HTTP_PATH_SEPARATOR, "."), false);
                sendPositiveResponse(exchange, new byte[0]);
                return HTTP_CODE_OK;
            } catch (ReatmetricException | RemoteException e) {
                return HTTP_CODE_INTERNAL_ERROR;
            }
        } else {
            return HTTP_CODE_NOT_FOUND;
        }
    }

    private int handleModelElementGetRequest(String path, HttpExchange exchange) throws IOException {
        path = path.replace(HTTP_PATH_SEPARATOR, ".");
        try {
            AbstractSystemEntityDescriptor descriptor = getDescriptorOf(path);
            List<AbstractSystemEntityDescriptor> children = getChildrenDescriptorOf(path);
            // Format the response
            byte[] body = JsonParseUtil.formatModelElementResponse(descriptor, children);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Model element GET request exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_NOT_FOUND;
        }
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }

    private AbstractSystemEntityDescriptor getDescriptorOf(String path) throws ReatmetricException, RemoteException {
        if(path.isBlank()) {
            // Return null descriptor
            return null;
        } else {
            SystemEntityPath thePath = SystemEntityPath.fromString(path);
            return getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getDescriptorOf(thePath);
        }
    }

    private List<AbstractSystemEntityDescriptor> getChildrenDescriptorOf(String path) throws ReatmetricException, RemoteException {
        if(path.isBlank()) {
            // Return the root descriptor
            return Collections.singletonList(getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getDescriptorOf(getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getRoot().getPath()));
        } else {
            SystemEntityPath thePath = SystemEntityPath.fromString(path);
            List<AbstractSystemEntityDescriptor> toReturn = new LinkedList<>();
            // Check if the element is a container first
            AbstractSystemEntityDescriptor elemDesc = getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getDescriptorOf(thePath);
            if(elemDesc.getType() == SystemEntityType.CONTAINER) {
                List<SystemEntity> children = getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getContainedEntities(thePath);
                for (SystemEntity se : children) {
                    toReturn.add(getDriver().getContext().getServiceFactory().getSystemModelMonitorService().getDescriptorOf(se.getPath()));
                }
            }
            return toReturn;
        }
    }

    private void setSystemElementEnablement(String path, boolean enable) throws ReatmetricException, RemoteException {
        SystemEntityPath thePath = SystemEntityPath.fromString(path);
        if(enable) {
            getDriver().getContext().getServiceFactory().getSystemModelMonitorService().enable(thePath);
        } else {
            getDriver().getContext().getServiceFactory().getSystemModelMonitorService().disable(thePath);
        }
    }
}
