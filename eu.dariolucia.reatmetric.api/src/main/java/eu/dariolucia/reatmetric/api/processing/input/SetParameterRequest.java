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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SetParameterRequest extends AbstractInputDataItem {

    public static SetParameterRequest ofRawValue(int id, Object value) {
        return new SetParameterRequest(id, false, value, Collections.emptyMap(), null, null);
    }

    public static SetParameterRequest ofRawValue(int id, Object value, Map<String, String> properties, String route, String source) {
        return new SetParameterRequest(id, false, value, properties, route, source);
    }

    public static SetParameterRequest ofEngineeringValue(int id, Object value, Map<String, String> properties, String route, String source) {
        return new SetParameterRequest(id, true, value, properties, route, source);
    }

    /**
     * The ID (System Entity ID) of the activity to be requested
     */
    private final int id;
    /**
     * If the value is the source or the engineering
     */
    private final boolean engineeringUsed;
    /**
     * The value to set
     */
    private final Object value;
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

    /**
     * Constructor of the class.
     *
     * @param id the ID of the parameter to set
     * @param engineeringUsed true if the engineering value is provided, otherwise false
     * @param value the value to set
     * @param properties the map of properties to use, cannot be null
     * @param route the route to forward the activity to
     * @param source the activity source
     */
    public SetParameterRequest(int id, boolean engineeringUsed, Object value, Map<String, String> properties, String route, String source) {
        this.id = id;
        this.engineeringUsed = engineeringUsed;
        this.value = value;
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
        this.route = route;
        this.source = source;
    }

    public int getId() {
        return id;
    }

    public boolean isEngineeringUsed() {
        return engineeringUsed;
    }

    public Object getValue() {
        return value;
    }

    public String getRoute() {
        return route;
    }

    public String getSource() {
        return source;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetParameterRequest that = (SetParameterRequest) o;
        return getId() == that.getId() &&
                isEngineeringUsed() == that.isEngineeringUsed() &&
                Objects.equals(getValue(), that.getValue()) &&
                Objects.equals(getProperties(), that.getProperties()) &&
                Objects.equals(getRoute(), that.getRoute()) &&
                Objects.equals(getSource(), that.getSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), isEngineeringUsed(), getValue(), getProperties(), getRoute(), getSource());
    }

    @Override
    public String toString() {
        return "SetParameterRequest{" +
                "id=" + id +
                ", engineeringUsed=" + engineeringUsed +
                ", value=" + value +
                ", properties=" + properties +
                ", route='" + route + '\'' +
                ", source='" + source + '\'' +
                "} " + super.toString();
    }
}
