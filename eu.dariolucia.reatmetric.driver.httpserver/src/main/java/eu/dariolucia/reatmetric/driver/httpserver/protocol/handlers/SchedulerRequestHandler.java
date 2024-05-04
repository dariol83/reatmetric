package eu.dariolucia.reatmetric.driver.httpserver.protocol.handlers;

import com.sun.net.httpserver.HttpExchange;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.scheduler.CreationConflictStrategy;
import eu.dariolucia.reatmetric.api.scheduler.ScheduledActivityData;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver;
import eu.dariolucia.reatmetric.driver.httpserver.protocol.JsonParseUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.dariolucia.reatmetric.driver.httpserver.HttpServerDriver.*;

public class SchedulerRequestHandler extends AbstractHttpRequestHandler {

    private static final Logger LOG = Logger.getLogger(SchedulerRequestHandler.class.getName());

    private static final String ENABLE_PATH = "enable";
    private static final String DISABLE_PATH = "disable";
    private static final String LOAD_PATH = "load";
    private static final String SCHEDULE_PATH = "schedule";
    private static final String CREATION_CONFLICT_ARG = "conflict";
    private static final String SOURCE_ARG = "source";

    public SchedulerRequestHandler(HttpServerDriver driver) {
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
        if (path.startsWith(HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + SCHEDULER_PATH)) {
            // Shorten the path
            path = path.substring((HTTP_PATH_SEPARATOR + getDriver().getSystemName() + HTTP_PATH_SEPARATOR + SCHEDULER_PATH).length());
            if (path.startsWith(HTTP_PATH_SEPARATOR)) {
                path = path.substring(HTTP_PATH_SEPARATOR.length());
            }
            if (path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Fetch full scheduler status
                handled = handleSchedulerStateGetRequest(exchange);
            } else if (!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_GET)) {
                // Fetch single scheduled item status
                handled = handleScheduledItemStateGetRequest(path, exchange);
            } else if (!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_DELETE)) {
                // Delete scheduled item
                handled = handleDeleteScheduledItemRequest(path, exchange);
            } else if (!path.isBlank() && exchange.getRequestMethod().equals(HTTP_METHOD_POST)) {
                // Check the path value:
                switch (path) {
                    case ENABLE_PATH:
                    case DISABLE_PATH:
                        handled = handleSchedulerEnableRequest(path, exchange);
                        break;
                    case LOAD_PATH:
                        handled = handleSchedulerLoadRequest(exchange);
                        break;
                    case SCHEDULE_PATH:
                        handled = handleSchedulerScheduleRequest(exchange);
                        break;
                    default:
                        // path is a number?conflict=... -> update scheduled item
                        handled = handleUpdateScheduledItemRequest(path, exchange);
                        break;
                }
            }
        }
        return handled;
    }

    private int handleSchedulerLoadRequest(HttpExchange exchange) throws IOException {
        Map<String, String> properties = JsonParseUtil.splitQuery(exchange.getRequestURI());
        try {
            List<SchedulingRequest> schedRequest = JsonParseUtil.parseSchedulingRequestList(exchange.getRequestBody(), getDriver().getActivityMap());
            CreationConflictStrategy conflictStrategy = CreationConflictStrategy.valueOf(properties.get(CREATION_CONFLICT_ARG));
            Instant starttime = Instant.ofEpochMilli(Long.parseLong(properties.get(START_TIME_ARG)));
            Instant endtime = Instant.ofEpochMilli(Long.parseLong(properties.get(END_TIME_ARG)));
            String source = properties.get(SOURCE_ARG);
            getDriver().getContext().getScheduler().load(starttime, endtime, schedRequest, source, conflictStrategy);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler POST schedule request exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleSchedulerScheduleRequest(HttpExchange exchange) throws IOException {
        Map<String, String> properties = JsonParseUtil.splitQuery(exchange.getRequestURI());
        try {
            SchedulingRequest schedRequest = JsonParseUtil.parseSchedulingRequest(exchange.getRequestBody(), getDriver().getActivityMap());
            CreationConflictStrategy conflictStrategy = CreationConflictStrategy.valueOf(properties.get(CREATION_CONFLICT_ARG));
            ScheduledActivityData schedData = getDriver().getContext().getScheduler().schedule(schedRequest, conflictStrategy);
            // Send the response
            sendPositiveResponse(exchange, JsonParseUtil.format("id", String.valueOf(schedData.getInternalId().asLong())));
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler POST schedule request exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleSchedulerEnableRequest(String path, HttpExchange exchange) throws IOException {
        try {
            switch (path) {
                case ENABLE_PATH:
                    getDriver().getContext().getScheduler().enable();
                    break;
                case DISABLE_PATH:
                    getDriver().getContext().getScheduler().disable();
                    break;
                default:
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Scheduler POST request for path " + path + " issue: operation not found");
                    }
                    return HTTP_CODE_NOT_FOUND;
            }

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler POST request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleDeleteScheduledItemRequest(String path, HttpExchange exchange) throws IOException {
        try {
            // Perform operation
            IUniqueId id = new LongUniqueId(Long.parseLong(path));
            getDriver().getContext().getScheduler().remove(id);
            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler DELETE request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleUpdateScheduledItemRequest(String path, HttpExchange exchange) throws IOException {
        IUniqueId id = new LongUniqueId(Long.parseLong(path.substring(0, path.indexOf(HTTP_QUERY_SEPARATOR))));
        Map<String, String> properties = JsonParseUtil.splitQuery(exchange.getRequestURI());
        try {
            // Perform request
            SchedulingRequest schedRequest = JsonParseUtil.parseSchedulingRequest(exchange.getRequestBody(), getDriver().getActivityMap());
            CreationConflictStrategy conflictStrategy = CreationConflictStrategy.valueOf(properties.get(CREATION_CONFLICT_ARG));
            getDriver().getContext().getScheduler().update(id, schedRequest, conflictStrategy);

            // Send the response
            sendPositiveResponse(exchange, new byte[0]);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler POST request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleScheduledItemStateGetRequest(String path, HttpExchange exchange) throws IOException {
        // Path is the ID of the scheduled item
        IUniqueId id = new LongUniqueId(Long.parseLong(path));
        try {
            List<ScheduledActivityData> items = getDriver().getContext().getScheduler().getCurrentScheduledActivities();
            Optional<ScheduledActivityData> item = items.stream().filter(o -> o.getInternalId().equals(id)).findFirst();
            if(item.isPresent()) {
                // Format the response
                byte[] body = JsonParseUtil.formatScheduledActivityData(item.get());
                // Send the response
                sendPositiveResponse(exchange, body);
                return HTTP_CODE_OK;
            } else {
                return HTTP_CODE_NOT_FOUND;
            }
        } catch (RemoteException | ReatmetricException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler GET request for path " + path + " exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    private int handleSchedulerStateGetRequest(HttpExchange exchange) throws IOException {
        try {
            boolean schedulerStatus = getDriver().getContext().getScheduler().isEnabled();
            List<ScheduledActivityData> items = getDriver().getContext().getScheduler().getCurrentScheduledActivities();
            // Format the response
            byte[] body = JsonParseUtil.formatSchedulerState(schedulerStatus, items);
            // Send the response
            sendPositiveResponse(exchange, body);
            return HTTP_CODE_OK;
        } catch (RemoteException | ReatmetricException e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Scheduler status GET request exception: " + e.getMessage(), e);
            }
            return HTTP_CODE_INTERNAL_ERROR;
        }
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }
}
