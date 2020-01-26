package eu.dariolucia.reatmetric.api.processing.input;

import java.util.*;

public final class ActivityRequest extends AbstractInputDataItem {

    public static ActivityRequest.Builder newRequest(int id) {
        return new Builder(id);
    }

    /**
     * The ID (System Entity ID) of the activity to be requested
     */
    private final int id;
    /**
     * The list of arguments.
     */
    private final List<ActivityArgument> arguments;
    /**
     * The map of properties.
     */
    private final Map<String, String> properties;
    /**
     * The route that the request must be forwarded to.
     */
    private final String route;
    /**
     * The source that originated the request.
     */
    private final String source;

    public ActivityRequest(int id, List<ActivityArgument> arguments, Map<String, String> properties, String route, String source) {
        this.id = id;
        this.arguments = List.copyOf(arguments);
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        this.route = route;
        this.source = source;
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

    public String getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityRequest that = (ActivityRequest) o;
        return getId() == that.getId() &&
                Objects.equals(getArguments(), that.getArguments()) &&
                Objects.equals(getProperties(), that.getProperties()) &&
                Objects.equals(getRoute(), that.getRoute()) &&
                Objects.equals(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getArguments(), getProperties(), getRoute(), getSource());
    }

    @Override
    public String toString() {
        return "ActivityRequest{" +
                "id=" + id +
                ", arguments=" + arguments +
                ", properties=" + properties +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                "}";
    }

    public static class Builder {
        private final int id;
        private List<ActivityArgument> arguments = new LinkedList<>();
        private Map<String, String> properties = new TreeMap<>();
        private String route;
        private String source;

        public Builder(int id) {
            this.id = id;
        }

        public Builder withArgument(ActivityArgument argument) {
            this.arguments.add(argument);
            return this;
        }

        public Builder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder withArguments(List<ActivityArgument> arguments) {
            this.arguments.addAll(arguments);
            return this;
        }

        public Builder withProperties(Map<String, String> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public Builder withRoute(String route) {
            this.route = route;
            return this;
        }

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        public ActivityRequest build() {
            return new ActivityRequest(id, arguments, properties, route, source);
        }
    }
}
