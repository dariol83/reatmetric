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
import eu.dariolucia.reatmetric.api.transport.TransportStatus;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.nio.charset.StandardCharsets;
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

    public static ActivityRequest parseActivityRequest(InputStream requestBody) {
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
        List<Map<String, Object>> argumentsObject = parsed.read(argumentListAccessor);
        List<AbstractActivityArgument> arguments = mapToElements(argumentsObject);
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

    private static List<AbstractActivityArgument> mapToElements(Iterable<Map<String, Object>> elementsIterator) {
        List<AbstractActivityArgument> arguments = new LinkedList<>();
        for(Iterator<Map<String, Object>> it = elementsIterator.iterator(); it.hasNext();) {
            Map<String, Object> arg = it.next(); // record, you need to fetch elements
            String name = (String) arg.get("name");
            if(Objects.equals(arg.get("type"), "plain")) {
                boolean isEngineering = (boolean) arg.get("engineering");
                PlainActivityArgument plainArgument = isEngineering ?
                        PlainActivityArgument.ofEngineering(name, arg.get("value")) :
                        PlainActivityArgument.ofSource(name, arg.get("value"));
                arguments.add(plainArgument);
            } else {
                // assume array, must be parsed
                List<ArrayActivityArgumentRecord> records = new LinkedList<>();
                // Iterable on records
                Iterable<Map<String, Object>> recordsIterator = (Iterable<Map<String, Object>>) arg.get("records");
                for(Iterator<Map<String, Object>> it2 = recordsIterator.iterator(); it2.hasNext();) {
                    Map<String, Object> recordItem = it2.next(); // record, you need to fetch elements
                    List<AbstractActivityArgument> elems = mapToElements((Iterable<Map<String, Object>>) recordItem.get("elements"));
                    records.add(new ArrayActivityArgumentRecord(elems));
                }
                arguments.add(new ArrayActivityArgument(name, records));
            }
        }
        return arguments;
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
        sb.append(String.format("\"message\" : \"%s\", ", obj.getMessage()));
        sb.append(String.format("\"source\" : %s, ", valueToString(obj.getSource())));
        sb.append(String.format("\"severity\" : \"%s\"", obj.getSeverity().name()));
        sb.append(" }");
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
            return "\"" + obj.toString() + "\"";
        } else {
            return "\"" + obj.toString() + "\"";
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
        sb.append(String.format("\"settable\" : %s", obj.isSettable()));
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
        sb.append(String.format("\"type\" : \"array\", "));
        sb.append(String.format("\"expansionArgument\" : %s, ", valueToString(ap.getExpansionArgument())));
        sb.append(String.format("\"elements\" : %s, ", formatArguments(ap.getElements())));
        sb.append(" }");
    }

    private static void format(StringBuilder sb, ActivityPlainArgumentDescriptor ap) {
        sb.append("{ ");
        sb.append(String.format("\"name\" : \"%s\", ", ap.getName()));
        sb.append(String.format("\"description\" : %s, ", valueToString(ap.getDescription())));
        sb.append(String.format("\"type\" : \"plain\", "));
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
}