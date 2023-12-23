package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import eu.dariolucia.reatmetric.api.activity.*;
import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.ContainerDescriptor;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.processing.input.*;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.api.scheduler.*;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import jakarta.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * All the returned data is encoded in UTF-8
 *
 */
public class JsonParseUtil {

    private JsonParseUtil() {
        throw new IllegalAccessError("Not supposed to be invoked");
    }

    // Credits: StackOverflow - https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
    public static Map<String, String> splitQuery(URI url) {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&", -1);
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }
        return queryPairs;
    }

    public static <T> T parseInput(InputStream requestBody, Class<T> inputClass) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String resultAccessor = "$['input']";
        return parsed.read(resultAccessor);
    }

    public static Map<String, Object> parseMapInput(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        List<Map<String, Object>> list = parsed.read("$[*]");
        Map<String, Object> toReturn = new LinkedHashMap<>();
        for(Map<String, Object> e : list) {
            toReturn.putAll(e);
        }
        return toReturn;
    }

    public static ParameterDataFilter parseParameterDataFilter(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String parentPathAccessor = "$['parentPath']";
        String parameterPathListAccessor = "$['parameterPathList']";
        String routeListAccessor = "$['routeList']";
        String validityListAccessor = "$['validityList']";
        String alarmStateListAccessor = "$['alarmStateList']";
        String externalIdListAccessor = "$['externalIdList']";

        String parentPath = parsed.read(parentPathAccessor);
        List<String> parameterPathList = parsed.read(parameterPathListAccessor);
        List<String> routes = parsed.read(routeListAccessor);
        List<String> validityList = parsed.read(validityListAccessor);
        List<String> alarmStateList = parsed.read(alarmStateListAccessor);
        List<Integer> externalIds = parsed.read(externalIdListAccessor);

        SystemEntityPath path = parentPath == null ? null : SystemEntityPath.fromString(parentPath);
        List<SystemEntityPath> parameterPaths = parameterPathList == null ? null : parameterPathList.stream().map(SystemEntityPath::fromString).collect(Collectors.toList());
        List<Validity> validities = validityList == null ? null : validityList.stream().map(Validity::valueOf).collect(Collectors.toList());
        List<AlarmState> alarmStates = alarmStateList == null ? null : alarmStateList.stream().map(AlarmState::valueOf).collect(Collectors.toList());
        return new ParameterDataFilter(
                path,
                parameterPaths,
                routes,
                validities,
                alarmStates,
                externalIds
        );
    }

    public static EventDataFilter parseEventDataFilter(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String parentPathAccessor = "$['parentPath']";
        String eventPathListAccessor = "$['eventPathList']";
        String routeListAccessor = "$['routeList']";
        String typeListAccessor = "$['typeList']";
        String sourceListAccessor = "$['sourceList']";
        String severityListAccessor = "$['severityList']";
        String externalIdListAccessor = "$['externalIdList']";

        String parentPath = parsed.read(parentPathAccessor);
        List<String> eventPathList = parsed.read(eventPathListAccessor);
        List<String> routes = parsed.read(routeListAccessor);
        List<String> types = parsed.read(typeListAccessor);
        List<String> sources = parsed.read(sourceListAccessor);
        List<String> severityList = parsed.read(severityListAccessor);
        List<Integer> externalIds = parsed.read(externalIdListAccessor);

        SystemEntityPath path = parentPath == null ? null : SystemEntityPath.fromString(parentPath);
        List<SystemEntityPath> eventPaths = eventPathList == null ? null : eventPathList.stream().map(SystemEntityPath::fromString).collect(Collectors.toList());
        List<Severity> severities = severityList == null ? null : severityList.stream().map(Severity::valueOf).collect(Collectors.toList());
        return new EventDataFilter(
                path,
                eventPaths,
                routes,
                types,
                sources,
                severities,
                externalIds
        );
    }

    public static RawDataFilter parseRawDataFilter(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String nameContainsAccessor = "$['nameContains']";
        String withDataAccessor = "$['contentSet']";
        String routeListAccessor = "$['routeList']";
        String typeListAccessor = "$['typeList']";
        String sourceListAccessor = "$['sourceList']";
        String qualityListAccessor = "$['qualityList']";

        String nameContains = parsed.read(nameContainsAccessor);
        boolean withData = parsed.read(withDataAccessor);
        List<String> routes = parsed.read(routeListAccessor);
        List<String> types = parsed.read(typeListAccessor);
        List<String> sources = parsed.read(sourceListAccessor);
        List<String> qualityList = parsed.read(qualityListAccessor);

        List<Quality> qualities = qualityList == null ? null : qualityList.stream().map(Quality::valueOf).collect(Collectors.toList());
        return new RawDataFilter(
                withData,
                nameContains,
                routes,
                types,
                sources,
                qualities
        );
    }

    public static ActivityOccurrenceDataFilter parseActivityOccurrenceDataFilter(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String parentPathAccessor = "$['parentPath']";
        String activityPathListAccessor = "$['activityPathList']";
        String routeListAccessor = "$['routeList']";
        String typeListAccessor = "$['typeList']";
        String sourceListAccessor = "$['sourceList']";
        String stateListAccessor = "$['stateList']";
        String externalIdListAccessor = "$['externalIdList']";

        String parentPath = parsed.read(parentPathAccessor);
        List<String> activityPathList = parsed.read(activityPathListAccessor);
        List<String> routes = parsed.read(routeListAccessor);
        List<String> types = parsed.read(typeListAccessor);
        List<String> sources = parsed.read(sourceListAccessor);
        List<String> stateList = parsed.read(stateListAccessor);
        List<Integer> externalIds = parsed.read(externalIdListAccessor);

        SystemEntityPath path = parentPath == null ? null : SystemEntityPath.fromString(parentPath);
        List<SystemEntityPath> activityPaths = activityPathList == null ? null : activityPathList.stream().map(SystemEntityPath::fromString).collect(Collectors.toList());
        List<ActivityOccurrenceState> stateLists = stateList == null ? null : stateList.stream().map(ActivityOccurrenceState::valueOf).collect(Collectors.toList());
        return new ActivityOccurrenceDataFilter(
                path,
                activityPaths,
                routes,
                types,
                stateLists,
                sources,
                externalIds
        );
    }

    public static ActivityRequest parseActivityRequest(InputStream requestBody, Map<String, ActivityDescriptor> descriptors) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String idAccessor = "$['id']";
        String activityPathAccessor = "$['path']";
        String routeAccessor = "$['route']";
        String argumentListAccessor = "$['arguments']";
        String sourceAccessor = "$['source']";
        String propertyMapAccessor = "$['properties']";

        int id = parsed.read(idAccessor);
        String activityPath = parsed.read(activityPathAccessor);
        String route = parsed.read(routeAccessor);
        String source = parsed.read(sourceAccessor);
        // Parse arguments
        ActivityDescriptor ad = descriptors.get(activityPath);
        List<Map<String, Object>> argumentsObject = parsed.read(argumentListAccessor);
        List<AbstractActivityArgument> arguments = mapToElements(argumentsObject, ad);
        // Parse properties
        Map<String, String> properties = parsed.read(propertyMapAccessor);
        // Build object
        return new ActivityRequest(
                id,
                SystemEntityPath.fromString(activityPath),
                arguments,
                properties,
                route,
                source
        );
    }

    public static SchedulingRequest parseSchedulingRequest(InputStream requestBody, Map<String, ActivityDescriptor> descriptors) throws IOException {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String requestAccessor = "$['request']";
        String resourcesListAccessor = "$['resources']";
        String sourceAccessor = "$['source']";
        String externalIdAccessor = "$['externalId']";
        String triggerAccessor = "$['trigger']";
        String latestInvocationTimeAccessor = "$['latest']"; // number of ms since 1st Jan 1970, 00:00:00
        String conflictAccessor = "$['conflict']";
        String durationAccessor = "$['duration']"; // in milliseconds

        List<String> resources = parsed.read(resourcesListAccessor);
        String source = parsed.read(sourceAccessor);
        String externalId = parsed.read(externalIdAccessor);
        long latestInvocationTimeNb = parsed.read(latestInvocationTimeAccessor);
        Instant latestInvocationTime = Instant.ofEpochMilli(latestInvocationTimeNb);
        ConflictStrategy conflictStrategy = ConflictStrategy.valueOf(parsed.read(conflictAccessor));
        long duration = parsed.read(durationAccessor);
        AbstractSchedulingTrigger trigger = parseTrigger(parsed.read(triggerAccessor));
        // Parse the request
        Map<String, Object> requestMap = parsed.read(requestAccessor);
        ActivityRequest request = parseActivityRequestFromMap(descriptors, requestMap);
        return new SchedulingRequest(request, new LinkedHashSet<>(resources), source, externalId, trigger, latestInvocationTime, conflictStrategy, Duration.ofMillis(duration));
    }

    private static ActivityRequest parseActivityRequestFromMap(Map<String, ActivityDescriptor> descriptors, Map<String, Object> requestMap) {
        int activityId = (Integer) requestMap.get("id");
        String activityPath = (String) requestMap.get("path");
        String route = (String) requestMap.get("route");
        String activitySource = (String) requestMap.get("source");
        // Parse arguments
        ActivityDescriptor ad = descriptors.get(activityPath);
        List<Map<String, Object>> argumentsObject = (List<Map<String, Object>>) requestMap.get("arguments");
        List<AbstractActivityArgument> arguments = mapToElements(argumentsObject, ad);
        // Parse properties
        Map<String, String> properties = (Map<String, String>) requestMap.get("properties");
        // Build object
        ActivityRequest request = new ActivityRequest(
                activityId,
                SystemEntityPath.fromString(activityPath),
                arguments,
                properties,
                route,
                activitySource
        );
        return request;
    }

    public static List<SchedulingRequest> parseSchedulingRequestList(InputStream requestBody, Map<String, ActivityDescriptor> descriptors) throws IOException {
        DocumentContext parsed = JsonPath.parse(requestBody);
        List<Map<String, Object>> list = parsed.read("$[*]");
        List<SchedulingRequest> schedulingRequestList = new LinkedList<>();
        for(Map<String, Object> item : list) {
            List<String> resources = (List<String>) item.get("resources");
            String source = (String) item.get("source");
            String externalId = (String) item.get("externalId");
            long latestInvocationTimeNb = ((Number) item.get("latest")).longValue();
            Instant latestInvocationTime = Instant.ofEpochMilli(latestInvocationTimeNb);
            ConflictStrategy conflictStrategy = ConflictStrategy.valueOf((String) item.get("conflict"));
            long duration = ((Number) item.get("duration")).longValue();
            AbstractSchedulingTrigger trigger = parseTrigger((Map<String, Object>) item.get("trigger"));
            // Parse the request
            Map<String, Object> requestMap = (Map<String, Object>) item.get("request");
            ActivityRequest request = parseActivityRequestFromMap(descriptors, requestMap);
            SchedulingRequest sr = new SchedulingRequest(request, new LinkedHashSet<>(resources), source, externalId, trigger, latestInvocationTime, conflictStrategy, Duration.ofMillis(duration));
            schedulingRequestList.add(sr);
        }
        return schedulingRequestList;
    }

    private static AbstractSchedulingTrigger parseTrigger(Map<String, Object> read) throws IOException {
        String type = (String) read.get("type");
        switch (type) {
            case "now":
                return new NowSchedulingTrigger();
            case "absolute":
                long timeNb = ((Number) read.get("time")).longValue();
                Instant releaseTime = Instant.ofEpochMilli(timeNb);
                return new AbsoluteTimeSchedulingTrigger(releaseTime);
            case "relative":
                List<String> predecessors = (List<String>) read.get("predecessors");
                int delay = ((Number) read.get("delay")).intValue(); // in milliseconds
                return new RelativeTimeSchedulingTrigger(new LinkedHashSet<>(predecessors), delay);
            case "event":
                String evPath = (String) read.get("path");
                int protectionTime = ((Number) read.get("protection")).intValue(); // in seconds
                boolean enabled = (Boolean) read.get("enabled");
                return new EventBasedSchedulingTrigger(SystemEntityPath.fromString(evPath), protectionTime, enabled);
            default:
                throw new IOException("Cannot derive trigger type: " + read);
        }
    }

    private static List<AbstractActivityArgument> mapToElements(Iterable<Map<String, Object>> elementsIterator, ActivityDescriptor ad) {
        List<AbstractActivityArgument> arguments = new LinkedList<>();
        for(Iterator<Map<String, Object>> it = elementsIterator.iterator(); it.hasNext();) {
            Map<String, Object> arg = it.next(); // record, you need to fetch elements
            String name = (String) arg.get("name");
            if(Objects.equals(arg.get("type"), "plain")) {
                boolean isEngineering = (boolean) arg.get("engineering");
                Object value = arg.get("value");
                value = sanitizeArgumentValue(value, name, isEngineering, ad);
                PlainActivityArgument plainArgument = isEngineering ?
                        PlainActivityArgument.ofEngineering(name, value) :
                        PlainActivityArgument.ofSource(name, value);
                arguments.add(plainArgument);
            } else {
                // assume array, must be parsed
                List<ArrayActivityArgumentRecord> records = new LinkedList<>();
                // Iterable on records
                Iterable<Map<String, Object>> recordsIterator = (Iterable<Map<String, Object>>) arg.get("records");
                for(Iterator<Map<String, Object>> it2 = recordsIterator.iterator(); it2.hasNext();) {
                    Map<String, Object> recordItem = it2.next(); // record, you need to fetch elements
                    List<AbstractActivityArgument> elems = mapToElements((Iterable<Map<String, Object>>) recordItem.get("elements"), ad);
                    records.add(new ArrayActivityArgumentRecord(elems));
                }
                arguments.add(new ArrayActivityArgument(name, records));
            }
        }
        return arguments;
    }

    private static Object sanitizeArgumentValue(Object value, String argName, boolean isEngineering, ActivityDescriptor ad) {
        if(ad == null) {
            return value;
        }
        // Look for argument descriptor
        Queue<AbstractActivityArgumentDescriptor> descQueue = new LinkedList<>(ad.getArgumentDescriptors());
        while(!descQueue.isEmpty()) {
            AbstractActivityArgumentDescriptor abstractDescriptor = descQueue.poll();
            if(abstractDescriptor instanceof ActivityPlainArgumentDescriptor) {
                ActivityPlainArgumentDescriptor plainArg = (ActivityPlainArgumentDescriptor) abstractDescriptor;
                if(plainArg.getName().equals(argName)) {
                    // Sanitize what you can: integer to long if type is UNSIGNED/SIGNED_INTEGER
                    if(value instanceof Integer) {
                        if(isEngineering &&
                            (plainArg.getEngineeringDataType() == ValueTypeEnum.UNSIGNED_INTEGER || plainArg.getEngineeringDataType() == ValueTypeEnum.SIGNED_INTEGER)) {
                            // Map to Long
                            return ((Integer) value).longValue();
                        }
                        if(!isEngineering &&
                                (plainArg.getRawDataType() == ValueTypeEnum.UNSIGNED_INTEGER || plainArg.getRawDataType() == ValueTypeEnum.SIGNED_INTEGER)) {
                            // Map to Long
                            return ((Integer) value).longValue();
                        }
                    }
                    // There is no point to go ahead with other descriptors, just return the value
                    return value;
                }
            } else if(abstractDescriptor instanceof ActivityArrayArgumentDescriptor) {
                // Get the inside items and add to queue
                descQueue.addAll(((ActivityArrayArgumentDescriptor) abstractDescriptor).getElements());
            }
        }
        // If you are here, just return the value
        return value;
    }

    public static OperationalMessageFilter parseOperationalMessageFilter(InputStream requestBody) {
        DocumentContext parsed = JsonPath.parse(requestBody);
        String messageTextContainsAccessor = "$['messageTextContains']";
        String idListAccessor = "$['idList']";
        String sourceListAccessor = "$['sourceList']";
        String severityListAccessor = "$['severityList']";

        String messageTextContains = parsed.read(messageTextContainsAccessor);
        List<String> idList = parsed.read(idListAccessor);
        List<String> sources = parsed.read(sourceListAccessor);
        List<String> severityList = parsed.read(severityListAccessor);

        List<Severity> severities = severityList == null ? null : severityList.stream().map(Severity::valueOf).collect(Collectors.toList());
        return new OperationalMessageFilter(
                messageTextContains,
                idList,
                sources,
                severities
        );
    }

    public static byte[] formatParameter(ParameterData update) {
        StringBuilder sb = new StringBuilder();
        format(sb, update);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatParameters(List<ParameterData> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < updates.size(); ++i) {
            sb.append("  ");
            ParameterData pd = updates.get(i);
            format(sb, pd);
            if (i < updates.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatEvents(List<EventData> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < updates.size(); ++i) {
            sb.append("  ");
            EventData pd = updates.get(i);
            format(sb, pd);
            if (i < updates.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatRawDatas(List<RawData> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < updates.size(); ++i) {
            sb.append("  ");
            RawData pd = updates.get(i);
            format(sb, pd);
            if (i < updates.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatActivities(List<ActivityOccurrenceData> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < updates.size(); ++i) {
            sb.append("  ");
            ActivityOccurrenceData pd = updates.get(i);
            format(sb, pd);
            if (i < updates.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatMessages(List<OperationalMessage> updates) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < updates.size(); ++i) {
            sb.append("  ");
            OperationalMessage pd = updates.get(i);
            format(sb, pd);
            if (i < updates.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatParameterDescriptors(List<ParameterDescriptor> descriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < descriptors.size(); ++i) {
            sb.append("  ");
            ParameterDescriptor pd = descriptors.get(i);
            format(sb, pd);
            if (i < descriptors.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatEventDescriptors(List<EventDescriptor> descriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < descriptors.size(); ++i) {
            sb.append("  ");
            EventDescriptor pd = descriptors.get(i);
            format(sb, pd);
            if (i < descriptors.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatActivityDescriptors(List<ActivityDescriptor> descriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < descriptors.size(); ++i) {
            sb.append("  ");
            ActivityDescriptor pd = descriptors.get(i);
            format(sb, pd);
            if (i < descriptors.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void format(StringBuilder sb, OperationalMessage obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"id\" : %s, ", valueToString(obj.getId())));
        sb.append(String.format("\"message\" : \"%s\", ", escapeMessage(obj.getMessage())));
        sb.append(String.format("\"source\" : %s, ", valueToString(obj.getSource())));
        sb.append(String.format("\"severity\" : \"%s\"", obj.getSeverity().name()));
        sb.append(" }");
    }

    private static String escapeMessage(String message) {
        return message.replace("\\", "\\\\");
    }

    private static void format(StringBuilder sb, EventData obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"qualifier\" : %s, ", valueToString(obj.getQualifier())));
        sb.append(String.format("\"rcptime\" : %s, ", valueToString(obj.getReceptionTime())));
        sb.append(String.format("\"type\" : %s, ", valueToString(obj.getType())));
        sb.append(String.format("\"route\" : %s, ", valueToString(obj.getRoute())));
        sb.append(String.format("\"source\" : %s, ", valueToString(obj.getSource())));
        sb.append(String.format("\"severity\" : \"%s\"", obj.getSeverity().name()));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, RawData obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"name\" : \"%s\", ", obj.getName()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"rcptime\" : %s, ", valueToString(obj.getReceptionTime())));
        sb.append(String.format("\"type\" : %s, ", valueToString(obj.getType())));
        sb.append(String.format("\"route\" : %s, ", valueToString(obj.getRoute())));
        sb.append(String.format("\"source\" : %s, ", valueToString(obj.getSource())));
        sb.append(String.format("\"quality\" : \"%s\", ", obj.getQuality().name()));
        if(obj.isContentsSet()) {
            sb.append(String.format("\"data\" : \"%s\"", DatatypeConverter.printBase64Binary(obj.getContents())));
        } else {
            sb.append("\"data\" : null");
        }

        sb.append(" }");
    }

    private static void format(StringBuilder sb, ActivityOccurrenceData obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"name\" : %s, ", valueToString(obj.getName())));
        sb.append(String.format("\"exectime\" : %s, ", valueToString(obj.getExecutionTime())));
        sb.append(String.format("\"type\" : %s, ", valueToString(obj.getType())));
        sb.append(String.format("\"route\" : %s, ", valueToString(obj.getRoute())));
        sb.append(String.format("\"source\" : %s, ", valueToString(obj.getSource())));
        sb.append(String.format("\"currentState\" : \"%s\", ", obj.getCurrentState().name()));
        sb.append(String.format("\"result\" : %s, ", valueToString(obj.getResult())));
        sb.append(String.format("\"arguments\" : %s, ", formatMap(obj.getArguments())));
        sb.append(String.format("\"properties\" : %s, ", formatMap(obj.getProperties())));
        sb.append(String.format("\"reports\" : %s ", formatActivityOccurrenceReports(obj.getProgressReports())));
        sb.append(" }");
    }

    private static String formatActivityOccurrenceReports(List<ActivityOccurrenceReport> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        int i = 0;
        for(ActivityOccurrenceReport add : reports) {
            format(sb, add);
            if(i < reports.size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            }
            ++i;
        }
        sb.append(" ]");
        return sb.toString();
    }

    private static void format(StringBuilder sb, ActivityOccurrenceReport obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"name\" : %s, ", valueToString(obj.getName())));
        sb.append(String.format("\"exectime\" : %s, ", valueToString(obj.getExecutionTime())));
        sb.append(String.format("\"state\" : \"%s\", ", obj.getState().name()));
        sb.append(String.format("\"transition\" : \"%s\", ", obj.getStateTransition().name()));
        sb.append(String.format("\"status\" : \"%s\", ", obj.getStatus().name()));
        sb.append(String.format("\"result\" : %s ", valueToString(obj.getResult())));
        sb.append(" }");
    }

    private static String formatMap(Map<String, ? extends Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        int i = 0;
        for(Map.Entry<String, ? extends Object> add : map.entrySet()) {
            sb.append(String.format("\"%s\" : %s", add.getKey(), valueToString(add.getValue())));
            if(i < map.size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            }
            ++i;
        }
        sb.append(" }");
        return sb.toString();
    }

    public static void format(StringBuilder sb, ParameterData obj) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", obj.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(obj.getGenerationTime())));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"eng\" : %s, ", valueToString(obj.getEngValue())));
        sb.append(String.format("\"raw\" : %s, ", valueToString(obj.getSourceValue())));
        sb.append(String.format("\"rcptime\" : %s, ", valueToString(obj.getReceptionTime())));
        sb.append(String.format("\"route\" : %s, ", valueToString(obj.getRoute())));
        sb.append(String.format("\"validity\" : \"%s\", ", obj.getValidity().name()));
        sb.append(String.format("\"alarm\" : \"%s\"", obj.getAlarmState().name()));
        sb.append(" }");
    }

    public static byte[] format(String key, String value) {
        return ("{ \"" + key + "\" : \"" + value + "\" }").getBytes(StandardCharsets.UTF_8);
    }

    private static String valueToString(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof Number) {
            return obj.toString();
        } else if (obj instanceof Instant) {
            return "\"" + obj + "\"";
        } else {
            return "\"" + obj + "\"";
        }
    }

    private static void format(StringBuilder sb, EventDescriptor obj) {
        sb.append("{ ");
        sb.append(String.format("\"type\" : \"%s\", ", obj.getType().name()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"description\" : %s, ", valueToString(obj.getDescription())));
        sb.append(String.format("\"severity\" : \"%s\", ", obj.getSeverity().name()));
        sb.append(String.format("\"eventType\" : %s", valueToString(obj.getEventType())));
        sb.append(" }");
    }


    private static void format(StringBuilder sb, ParameterDescriptor obj) {
        sb.append("{ ");
        sb.append(String.format("\"type\" : \"%s\", ", obj.getType().name()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"description\" : %s, ", valueToString(obj.getDescription())));
        sb.append(String.format("\"rawDataType\" : \"%s\", ", obj.getRawDataType().name()));
        sb.append(String.format("\"engDataType\" : \"%s\", ", obj.getEngineeringDataType().name()));
        sb.append(String.format("\"unit\" : %s, ", valueToString(obj.getUnit())));
        sb.append(String.format("\"synthetic\" : %s, ", obj.isSynthetic()));
        sb.append(String.format("\"settable\" : %s, ", obj.isSettable()));
        sb.append(String.format("\"user\" : %s", obj.isUserParameter()));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, ActivityDescriptor obj) {
        sb.append("{ ");
        sb.append(String.format("\"type\" : \"%s\", ", obj.getType().name()));
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"description\" : %s, ", valueToString(obj.getDescription())));
        sb.append(String.format("\"activityType\" : \"%s\", ", obj.getActivityType()));
        sb.append(String.format("\"defaultRoute\" : %s, ", valueToString(obj.getDefaultRoute())));
        sb.append(String.format("\"arguments\" : %s, ", formatArguments(obj.getArgumentDescriptors())));
        sb.append(String.format("\"properties\" : %s", formatProperties(obj.getProperties())));
        sb.append(" }");
    }

    private static String formatProperties(List<Pair<String, String>> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        int i = 0;
        for(Pair<String, String> add : properties) {
            sb.append(String.format("\"%s\" : %s", add.getFirst(), valueToString(add.getSecond())));
            if(i < properties.size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            }
            ++i;
        }
        sb.append(" }");
        return sb.toString();
    }

    private static String formatArguments(List<AbstractActivityArgumentDescriptor> argumentDescriptors) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        int i = 0;
        for(AbstractActivityArgumentDescriptor add : argumentDescriptors) {
            if(add instanceof ActivityPlainArgumentDescriptor) {
                ActivityPlainArgumentDescriptor ap = (ActivityPlainArgumentDescriptor) add;
                format(sb, ap);
            } else if(add instanceof ActivityArrayArgumentDescriptor) {
                ActivityArrayArgumentDescriptor ap = (ActivityArrayArgumentDescriptor) add;
                format(sb, ap);
            }
            if(i < argumentDescriptors.size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            }
            ++i;
        }
        sb.append(" ]");
        return sb.toString();
    }

    private static void format(StringBuilder sb, ActivityArrayArgumentDescriptor ap) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", ap.getName()));
        sb.append(String.format("\"description\" : %s, ", valueToString(ap.getDescription())));
        sb.append("\"type\" : \"array\", ");
        sb.append(String.format("\"expansionArgument\" : %s, ", valueToString(ap.getExpansionArgument())));
        sb.append(String.format("\"elements\" : %s, ", formatArguments(ap.getElements())));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, ActivityPlainArgumentDescriptor ap) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", ap.getName()));
        sb.append(String.format("\"description\" : %s, ", valueToString(ap.getDescription())));
        sb.append("\"type\" : \"plain\", ");
        sb.append(String.format("\"rawDataType\" : \"%s\", ", ap.getRawDataType().name()));
        sb.append(String.format("\"engDataType\" : \"%s\", ", ap.getEngineeringDataType().name()));
        sb.append(String.format("\"unit\" : %s, ", valueToString(ap.getUnit())));
        sb.append(String.format("\"fixed\" : %s, ", ap.isFixed()));
        sb.append(String.format("\"decalibrationPresent\" : %s", ap.isDecalibrationSet()));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, ContainerDescriptor obj) {
        sb.append("{ ");
        sb.append(String.format("\"type\" : \"%s\", ", obj.getType().name()));
        sb.append(String.format("\"path\" : \"%s\" ", obj.getPath().asString()));
        sb.append(" }");
    }

    public static byte[] formatModelElementResponse(AbstractSystemEntityDescriptor descriptor, List<AbstractSystemEntityDescriptor> children) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\"element\" : ");
        if(descriptor != null) {
            formatDescriptor(sb, descriptor);
        } else {
            sb.append("null");
        }
        sb.append(",\n");
        sb.append("\"children\" : [\n");
        for(int i = 0; i < children.size(); ++i) {
            formatDescriptor(sb, children.get(i));
            if(i != children.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        sb.append("}");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void formatDescriptor(StringBuilder sb, AbstractSystemEntityDescriptor descriptor) {
        if(descriptor instanceof ParameterDescriptor) {
            format(sb, (ParameterDescriptor) descriptor);
        } else if(descriptor instanceof EventDescriptor) {
            format(sb, (EventDescriptor) descriptor);
        } else if(descriptor instanceof ActivityDescriptor) {
            format(sb, (ActivityDescriptor) descriptor);
        } else if(descriptor instanceof ContainerDescriptor) {
            format(sb, (ContainerDescriptor) descriptor);
        } else {
            throw new IllegalArgumentException("Object " + descriptor + " not supported");
        }
    }

    public static byte[] formatConnectorList(List<Pair<TransportStatus, String>> cntList) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < cntList.size(); ++i) {
            sb.append("  ");
            format(sb, cntList.get(i));
            if (i < cntList.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void format(StringBuilder sb, Pair<TransportStatus, String> obj) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", obj.getFirst().getName()));
        sb.append(String.format("\"description\" : \"%s\", ", obj.getSecond()));
        sb.append(String.format("\"alarmState\" : \"%s\", ", obj.getFirst().getAlarmState().name()));
        sb.append(String.format("\"status\" : \"%s\", ", obj.getFirst().getStatus().name()));
        sb.append(String.format("\"rx\" : %d, ", obj.getFirst().getRxRate()));
        sb.append(String.format("\"tx\" : %d, ", obj.getFirst().getTxRate()));
        sb.append(String.format("\"autoreconnect\" : %s", obj.getFirst().isAutoReconnect()));
        sb.append(" }");
    }

    public static byte[] formatConnector(Pair<TransportStatus, String> theConnector) {
        StringBuilder sb = new StringBuilder();
        format(sb, theConnector);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatConnectorProperties(Map<String, Object[]> string2descTypeValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean atLeastOne = false;
        for(Map.Entry<String, Object[]> e : string2descTypeValue.entrySet()) {
            sb.append("\n{ ");
            sb.append(String.format("\"name\" : \"%s\", ", e.getKey()));
            sb.append(String.format("\"description\" : \"%s\", ", e.getValue()[0]));
            sb.append(String.format("\"type\" : \"%s\", ", ((ValueTypeEnum) (e.getValue()[1])).name()));
            sb.append(String.format("\"value\" : %s ", valueToString(e.getValue()[2])));
            sb.append(" },");
            atLeastOne = true;
        }
        if(atLeastOne) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("\n]");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] formatScheduledActivityData(ScheduledActivityData item) {
        StringBuilder sb = new StringBuilder();
        format(sb, item);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void format(StringBuilder sb, ScheduledActivityData item) {
        sb.append("{ ");
        sb.append(String.format("\"internalId\" : %d, ", item.getInternalId().asLong()));
        sb.append(String.format("\"gentime\" : %s, ", valueToString(item.getGenerationTime())));
        sb.append(String.format("\"request\" : "));
        format(sb, item.getRequest());
        sb.append(", ");
        sb.append(String.format("\"activity\" : %s, ", item.getActivityOccurrence() != null ? item.getActivityOccurrence().asLong() : "null" ));
        sb.append(String.format("\"resources\" : %s, ", listArray(item.getResources())));
        sb.append(String.format("\"source\" : \"%s\", ", item.getSource()));
        sb.append(String.format("\"externalId\" : %s, ", valueToString(item.getExternalId())));
        sb.append(String.format("\"trigger\" : "));
        format(sb, item.getTrigger());
        sb.append(", ");
        sb.append(String.format("\"latest\" : %s, ", valueToString(item.getLatestInvocationTime())));
        sb.append(String.format("\"startTime\" : %s, ", valueToString(item.getStartTime())));
        sb.append(String.format("\"duration\" : %d, ", item.getDuration() == null ? -1 : item.getDuration().toMillis()));
        sb.append(String.format("\"conflict\" : \"%s\", ", item.getConflictStrategy().name()));
        sb.append(String.format("\"state\" : \"%s\"", item.getState().name()));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, ActivityRequest request) {
        sb.append("{ ");
        sb.append(String.format("\"id\" : %d, ", request.getId()));
        sb.append(String.format("\"path\" : \"%s\", ", request.getPath().asString()));
        sb.append(String.format("\"route\" : %s, ", valueToString(request.getRoute())));
        sb.append(String.format("\"arguments\" : %s, ", formatActivityArguments(request.getArguments())));
        sb.append(String.format("\"source\" : %s, ", valueToString(request.getSource())));
        sb.append(String.format("\"properties\" : %s ", formatMap(request.getProperties())));
        sb.append(" }");
    }

    private static String formatActivityArguments(List<AbstractActivityArgument> arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        int i = 0;
        for(AbstractActivityArgument add : arguments) {
            if(add instanceof PlainActivityArgument) {
                PlainActivityArgument ap = (PlainActivityArgument) add;
                format(sb, ap);
            } else if(add instanceof ArrayActivityArgument) {
                ArrayActivityArgument ap = (ArrayActivityArgument) add;
                format(sb, ap);
            }
            if(i < arguments.size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            }
            ++i;
        }
        sb.append(" ]");
        return sb.toString();
    }

    private static void format(StringBuilder sb, ArrayActivityArgument ap) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", ap.getName()));
        sb.append("\"type\" : \"array\", ");
        sb.append("\"records\" : ");
        sb.append("[ ");
        for(int i = 0; i < ap.getRecords().size(); ++i) {
            ArrayActivityArgumentRecord record = ap.getRecords().get(i);
            sb.append("{ ");
            sb.append(String.format("\"elements\" : %s ", formatActivityArguments(record.getElements())));
            sb.append(" }");
            if(i < ap.getRecords().size() - 1) {
                // Not the last, add comma
                sb.append(", ");
            } else {
                sb.append(" ");
            }
        }
        sb.append("]");

        sb.append(" }");
    }

    private static void format(StringBuilder sb, PlainActivityArgument ap) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", ap.getName()));
        sb.append("\"type\" : \"plain\", ");
        sb.append(String.format("\"value\" : %s, ", valueToString(ap.isEngineering() ? ap.getEngValue() : ap.getRawValue())));
        sb.append(String.format("\"engineering\" : %s ", ap.isEngineering()));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, AbstractSchedulingTrigger trigger) {
        if(trigger instanceof NowSchedulingTrigger) {
            sb.append("{ \"type\" : \"now\" }");
        } else if(trigger instanceof AbsoluteTimeSchedulingTrigger) {
            sb.append("{ \"type\" : \"absolute\", ");
            sb.append(String.format("\"startTime\" : %s }", valueToString(((AbsoluteTimeSchedulingTrigger) trigger).getReleaseTime())));
        } else if(trigger instanceof RelativeTimeSchedulingTrigger) {
            sb.append("{ \"type\" : \"relative\", ");
            sb.append(String.format("\"predecessors\" : %s, ", listArray(((RelativeTimeSchedulingTrigger) trigger).getPredecessors())));
            sb.append(String.format("\"delay\" : %s }", valueToString(((RelativeTimeSchedulingTrigger) trigger).getDelayTime())));
        } else if(trigger instanceof EventBasedSchedulingTrigger) {
            sb.append("{ \"type\" : \"event\", ");
            sb.append(String.format("\"path\" : %s, ", valueToString(((EventBasedSchedulingTrigger) trigger).getEvent().asString())));
            sb.append(String.format("\"protection\" : %s, ", valueToString(((EventBasedSchedulingTrigger) trigger).getProtectionTime())));
            sb.append(String.format("\"enabled\" : %s }", ((EventBasedSchedulingTrigger) trigger).isEnabled() ? "true" : "false"));
        } else {
            throw new RuntimeException("Software bug, trigger type not supported: " + trigger);
        }
    }

    private static String listArray(Collection<String> resources) {
        if(resources == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for(String s : resources) {
            sb.append(s + ", ");
        }
        if(resources.size() > 0) {
            sb.delete(sb.length() - 2, sb.length()).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static byte[] formatSchedulerState(boolean schedulerStatus, List<ScheduledActivityData> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(String.format("\"enabled\" : %s, ", schedulerStatus));
        sb.append("\"items\" : [ ");
        for(int i = 0; i < items.size(); ++i) {
            format(sb, items.get(i));
            if(i != items.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(" ] }");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}