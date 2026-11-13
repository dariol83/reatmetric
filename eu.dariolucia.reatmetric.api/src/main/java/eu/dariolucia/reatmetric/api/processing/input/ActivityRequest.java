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

/**
 * A request to execute an activity.
 * This class contains all the information needed to execute an activity, including the activity identifier,
 * arguments, properties, route, and source.
 */
public final class ActivityRequest extends AbstractInputDataItem {

    /**
     * Creates a new builder for an activity request.
     *
     * @param id the ID (System Entity ID) of the activity to be requested
     * @param path the path of the activity to be requested, cannot be null
     * @return a new builder instance
     */
    public static ActivityRequest.Builder newRequest(int id, SystemEntityPath path) {
        return new Builder(id, path);
    }

    /**
     * The ID (System Entity ID) of the activity to be requested.
     */
    private final int id;
    
    /**
     * The path of the activity to be requested.
     */
    private final SystemEntityPath path;
    
    /**
     * The list of arguments for the activity.
     */
    private final List<AbstractActivityArgument> arguments;
    
    /**
     * The map of properties for the activity request.
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

    /**
     * Constructor for an activity request.
     *
     * @param id the ID (System Entity ID) of the activity to be requested
     * @param path the path of the activity to be requested, cannot be null
     * @param arguments the list of arguments for the activity (can be null or empty)
     * @param properties the map of properties for the activity request (can be null or empty)
     * @param route the route that the request must be forwarded to (can be null)
     * @param source the source that originated the request (can be null)
     */
    public ActivityRequest(int id, SystemEntityPath path, List<AbstractActivityArgument> arguments, Map<String, String> properties, String route, String source) {
        this.id = id;
        this.arguments = List.copyOf(arguments);
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        this.route = route;
        this.source = source;
        this.path = path;
    }

    /**
     * Returns the ID of the activity to be requested.
     *
     * @return the activity ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the path of the activity to be requested.
     *
     * @return the activity path (cannot be null)
     */
    public SystemEntityPath getPath() {
        return path;
    }

    /**
     * Returns the list of arguments for the activity.
     * The returned list is unmodifiable.
     *
     * @return the list of arguments (can be empty, cannot be null)
     */
    public List<AbstractActivityArgument> getArguments() {
        return arguments;
    }

    /**
     * Returns the map of properties for the activity request.
     * The returned map is unmodifiable.
     *
     * @return the map of properties (can be empty, cannot be null)
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the route that the request must be forwarded to.
     *
     * @return the route (can be null)
     */
    public String getRoute() {
        return route;
    }

    /**
     * Returns the source that originated the request.
     *
     * @return the source (can be null)
     */
    public String getSource() {
        return source;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two activity requests are equal if they have the same ID, path, arguments, properties, route, and source.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
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

    /**
     * Returns a hash code value for this activity request.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getPath(), getArguments(), getProperties(), getRoute(), getSource());
    }

    /**
     * Returns a string representation of this activity request.
     *
     * @return a string representation of this object
     */
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

    /**
     * Builder class for creating ActivityRequest instances.
     */
    public static class Builder {
        private final int id;
        private final SystemEntityPath path;
        private List<AbstractActivityArgument> arguments = new LinkedList<>();
        private Map<String, String> properties = new TreeMap<>();
        private String route;
        private String source;

        /**
         * Constructor for the builder.
         *
         * @param id the ID (System Entity ID) of the activity to be requested
         * @param path the path of the activity to be requested, cannot be null
         */
        public Builder(int id, SystemEntityPath path) {
            this.id = id;
            this.path = path;
        }

        /**
         * Adds a single argument to the activity request.
         *
         * @param argument the argument to add, cannot be null
         * @return this builder for method chaining
         */
        public Builder withArgument(AbstractActivityArgument argument) {
            this.arguments.add(argument);
            return this;
        }

        /**
         * Adds a single property to the activity request.
         *
         * @param key the property key, cannot be null
         * @param value the property value, can be null
         * @return this builder for method chaining
         */
        public Builder withProperty(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        /**
         * Adds multiple arguments to the activity request.
         *
         * @param arguments the list of arguments to add, cannot be null
         * @return this builder for method chaining
         */
        public Builder withArguments(List<AbstractActivityArgument> arguments) {
            this.arguments.addAll(arguments);
            return this;
        }

        /**
         * Adds multiple properties to the activity request.
         *
         * @param properties the map of properties to add, cannot be null
         * @return this builder for method chaining
         */
        public Builder withProperties(Map<String, String> properties) {
            this.properties.putAll(properties);
            return this;
        }

        /**
         * Sets the route for the activity request.
         *
         * @param route the route that the request must be forwarded to, can be null
         * @return this builder for method chaining
         */
        public Builder withRoute(String route) {
            this.route = route;
            return this;
        }

        /**
         * Sets the source for the activity request.
         *
         * @param source the source that originated the request, can be null
         * @return this builder for method chaining
         */
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        /**
         * Builds the ActivityRequest instance.
         *
         * @return a new ActivityRequest instance with the configured properties
         */
        public ActivityRequest build() {
            return new ActivityRequest(id, path, arguments, properties, route, source);
        }
    }
}
