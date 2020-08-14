/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.api.processing.input;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.util.*;

public final class ActivityRequest extends AbstractInputDataItem {

    public static ActivityRequest.Builder newRequest(int id, SystemEntityPath path) {
        return new Builder(id, path);
    }

    /**
     * The ID (System Entity ID) of the activity to be requested
     */
    private final int id;
    /**
     * The path of the activity to be requested
     */
    private final SystemEntityPath path;
    /**
     * The list of arguments.
     */
    private final List<AbstractActivityArgument> arguments;
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

    public ActivityRequest(int id, SystemEntityPath path, List<AbstractActivityArgument> arguments, Map<String, String> properties, String route, String source) {
        this.id = id;
        this.arguments = List.copyOf(arguments);
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        this.route = route;
        this.source = source;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public SystemEntityPath getPath() {
        return path;
    }

    public List<AbstractActivityArgument> getArguments() {
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
                Objects.equals(getPath(), that.getPath()) &&
                Objects.equals(getArguments(), that.getArguments()) &&
                Objects.equals(getProperties(), that.getProperties()) &&
                Objects.equals(getRoute(), that.getRoute()) &&
                Objects.equals(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPath(), getArguments(), getProperties(), getRoute(), getSource());
    }

    @Override
    public String toString() {
        return "ActivityRequest{" +
                "id=" + id +
                ", path=" + path +
                ", arguments=" + arguments +
                ", properties=" + properties +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                "}";
    }

    public static class Builder {
        private final int id;
        private final SystemEntityPath path;
        private List<AbstractActivityArgument> arguments = new LinkedList<>();
        private Map<String, String> properties = new TreeMap<>();
        private String route;
        private String source;

        public Builder(int id, SystemEntityPath path) {
            this.id = id;
            this.path = path;
        }

        public Builder withArgument(AbstractActivityArgument argument) {
            this.arguments.add(argument);
            return this;
        }

        public Builder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder withArguments(List<AbstractActivityArgument> arguments) {
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
            return new ActivityRequest(id, path, arguments, properties, route, source);
        }
    }
}
