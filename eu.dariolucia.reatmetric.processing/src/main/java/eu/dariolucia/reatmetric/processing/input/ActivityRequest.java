package eu.dariolucia.reatmetric.processing.input;

import java.util.*;

public final class ActivityRequest extends AbstractInputDataItem {

    private final int id;
    private final List<ActivityArgument> arguments;
    private final Map<String, String> properties;
    private final String route;

    public ActivityRequest(int id, List<ActivityArgument> arguments, Map<String, String> properties, String route) {
        this.id = id;
        this.arguments = List.copyOf(arguments);
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        this.route = route;
    }

    public int getId() {
        return id;
    }

    public List<ActivityArgument> getArguments() {
        return arguments;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getRoute() {
        return route;
    }
}
