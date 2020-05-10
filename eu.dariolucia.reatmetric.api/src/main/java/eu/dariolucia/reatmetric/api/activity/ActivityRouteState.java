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

package eu.dariolucia.reatmetric.api.activity;

import java.util.Objects;

/**
 * This object is used to deliver the availability state of an activity route.
 *
 * This object is immutable.
 */
public class ActivityRouteState {

    private final String route;
    private final ActivityRouteAvailability availability;

    /**
     * Constructor of the object.
     *
     * @param route the activity route
     * @param availability the state of the route
     */
    public ActivityRouteState(String route, ActivityRouteAvailability availability) {
        this.route = route;
        this.availability = availability;
    }

    /**
     * Return the route name.
     *
     * @return the route name, cannot be null
     */
    public String getRoute() {
        return route;
    }

    /**
     * Return the route availability.
     *
     * @return true if available, otherwise false
     */
    public ActivityRouteAvailability getAvailability() {
        return availability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityRouteState that = (ActivityRouteState) o;
        return getRoute().equals(that.getRoute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRoute());
    }

    @Override
    public String toString() {
        return route + " [" + availability + "]";
    }
}
