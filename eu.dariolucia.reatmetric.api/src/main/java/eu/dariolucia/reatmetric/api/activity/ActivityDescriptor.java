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

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;

import java.time.Duration;
import java.util.List;

/**
 * The descriptor of an activity system entity.
 *
 * Objects of this class are immutable.
 */
public class ActivityDescriptor extends AbstractSystemEntityDescriptor {

    private final String description;
    private final String defaultRoute;
    private final String activityType;
    private final List<AbstractActivityArgumentDescriptor> argumentDescriptors;
    private final List<Pair<String, String>> properties;
    private final Duration expectedDuration;

    /**
     * Constructor of the class.
     *
     * @param path the activity path
     * @param externalId the activity ID
     * @param description the activity description
     * @param defaultRoute the default route of the activity
     * @param activityType the activity type
     * @param argumentDescriptors the list of argument descriptors
     * @param properties the list of properties
     * @param expectedDuration the expected duration of the activity
     */
    public ActivityDescriptor(SystemEntityPath path, int externalId, String description, String defaultRoute, String activityType, List<AbstractActivityArgumentDescriptor> argumentDescriptors, List<Pair<String, String>> properties, Duration expectedDuration) {
        super(path, externalId, SystemEntityType.ACTIVITY);
        this.description = description;
        this.defaultRoute = defaultRoute;
        this.activityType = activityType;
        this.argumentDescriptors = List.copyOf(argumentDescriptors);
        this.properties = List.copyOf(properties);
        this.expectedDuration = expectedDuration;
    }

    /**
     * Return the activity description.
     *
     * @return the activity description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return the default route defined for the activity.
     *
     * @return the default route
     */
    public String getDefaultRoute() {
        return defaultRoute;
    }

    /**
     * Return the activity type.
     *
     * @return the activity type
     */
    public String getActivityType() {
        return activityType;
    }

    /**
     * Return the list of argument descriptors.
     *
     * @return the list of argument descriptors
     */
    public List<AbstractActivityArgumentDescriptor> getArgumentDescriptors() {
        return argumentDescriptors;
    }

    /**
     * Return the list of properties as defined for the activity.
     *
     * @return the list of properties
     */
    public List<Pair<String, String>> getProperties() {
        return properties;
    }

    /**
     * Return the expected duration of the activity, as specified in the definition.
     *
     * @return the expected activity duration
     */
    public Duration getExpectedDuration() {
        return expectedDuration;
    }
}
