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

public class SetParameterRequest extends AbstractInputDataItem {

    public static SetParameterRequest ofRawValue(int id, Object value, String route, String source) {
        return new SetParameterRequest(id, false, value, route, source);
    }

    public static SetParameterRequest ofEngineeringValue(int id, Object value, String route, String source) {
        return new SetParameterRequest(id, true, value, route, source);
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
     * The route that the request must be forwarded to.
     */
    private final String route;
    /**
     * The source that originated the request.
     */
    private final String source;

    public SetParameterRequest(int id, boolean engineeringUsed, Object value, String route, String source) {
        this.id = id;
        this.engineeringUsed = engineeringUsed;
        this.value = value;
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
}
