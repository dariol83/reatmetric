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

import java.util.List;

public class ActivityDescriptor extends AbstractSystemEntityDescriptor {

    private final String description;
    private final String defaultRoute;
    private final String activityType;
    private final List<ActivityArgumentDescriptor> argumentDescriptors;
    private final List<Pair<String, String>> properties;

    public ActivityDescriptor(SystemEntityPath path, int externalId, String description, String defaultRoute, String activityType, List<ActivityArgumentDescriptor> argumentDescriptors, List<Pair<String, String>> properties) {
        super(path, externalId, SystemEntityType.ACTIVITY);
        this.description = description;
        this.defaultRoute = defaultRoute;
        this.activityType = activityType;
        this.argumentDescriptors = List.copyOf(argumentDescriptors);
        this.properties = List.copyOf(properties);
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultRoute() {
        return defaultRoute;
    }

    public String getActivityType() {
        return activityType;
    }

    public List<ActivityArgumentDescriptor> getArgumentDescriptors() {
        return argumentDescriptors;
    }

    public List<Pair<String, String>> getProperties() {
        return properties;
    }
}
