package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;
import eu.dariolucia.reatmetric.api.events.EventDescriptor;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.api.messages.Severity;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.ParameterDescriptor;
import eu.dariolucia.reatmetric.api.parameters.Validity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * All the returned data is encoded in UTF-8
 */
public class JsonParseUtil {

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
        sb.append(String.format("\"path\" : \"%s\", ", obj.getPath().asString()));
        sb.append(String.format("\"externalId\" : %d, ", obj.getExternalId()));
        sb.append(String.format("\"description\" : %s, ", valueToString(obj.getDescription())));
        sb.append(String.format("\"severity\" : \"%s\", ", obj.getSeverity().name()));
        sb.append(String.format("\"eventType\" : %s", valueToString(obj.getEventType())));
        sb.append(" }");
    }


    private static void format(StringBuilder sb, ParameterDescriptor obj) {
        sb.append("{ ");
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
}