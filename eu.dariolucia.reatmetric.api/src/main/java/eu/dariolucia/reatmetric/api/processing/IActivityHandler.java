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

package eu.dariolucia.reatmetric.api.processing;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface IActivityHandler {

    void registerModel(IProcessingModel model);

    void deregisterModel(IProcessingModel model);

    List<String> getSupportedRoutes();

    List<String> getSupportedActivityTypes();

    void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException;

    boolean getRouteAvailability(String route) throws ActivityHandlingException;

    void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException;

    class ActivityInvocation {
        private final IUniqueId activityOccurrenceId;
        private final Instant generationTime;
        private final int activityId;
        private final SystemEntityPath path;
        private final String type;
        private final Map<String, Object> arguments;
        private final Map<String, String> properties;
        private final String route;
        private final String source;

        public ActivityInvocation(IUniqueId activityOccurrenceId, int activityId, Instant generationTime, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, String route, String source) {
            this.activityOccurrenceId = activityOccurrenceId;
            this.generationTime = generationTime;
            this.activityId = activityId;
            this.path = path;
            this.type = type;
            this.arguments = arguments;
            this.properties = properties;
            this.route = route;
            this.source = source;
        }

        public Instant getGenerationTime() {
            return generationTime;
        }

        public IUniqueId getActivityOccurrenceId() {
            return activityOccurrenceId;
        }

        public int getActivityId() {
            return activityId;
        }

        public SystemEntityPath getPath() {
            return path;
        }

        public String getType() {
            return type;
        }

        public Map<String, Object> getArguments() {
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
        public String toString() {
            return "ActivityInvocation{" +
                    "activityOccurrenceId=" + activityOccurrenceId +
                    ", activityId=" + activityId +
                    ", generationTime=" + generationTime +
                    ", path=" + path +
                    ", type='" + type + '\'' +
                    ", arguments=" + arguments +
                    ", properties=" + properties +
                    ", route='" + route + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
}
