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

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Interface for handling activities in the ReatMetric system.
 * Implementations of this interface are responsible for executing activities
 * and managing their lifecycle.
 */
public interface IActivityHandler {

    /**
     * Registers a processing model with this handler.
     *
     * @param model the processing model to register
     */
    void registerModel(IProcessingModel model);

    /**
     * Deregisters a processing model from this handler.
     *
     * @param model the processing model to deregister
     */
    void deregisterModel(IProcessingModel model);

    /**
     * Returns the list of supported routes.
     *
     * @return the list of supported routes
     */
    List<String> getSupportedRoutes();

    /**
     * Returns the list of supported activity types.
     *
     * @return the list of supported activity types
     */
    List<String> getSupportedActivityTypes();

    /**
     * Executes an activity.
     *
     * @param activityInvocation the activity invocation to execute
     * @throws ActivityHandlingException if an error occurs during activity execution
     */
    void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException;

    /**
     * Checks if a route is available.
     *
     * @param route the route to check
     * @return true if the route is available, false otherwise
     * @throws ActivityHandlingException if an error occurs while checking route availability
     */
    boolean getRouteAvailability(String route) throws ActivityHandlingException;

    /**
     * Aborts an activity.
     *
     * @param activityId the ID of the activity to abort
     * @param activityOccurrenceId the occurrence ID of the activity to abort
     * @throws ActivityHandlingException if an error occurs during activity abort
     */
    void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException;

    /**
     * Represents an invocation of an activity.
     * Contains all the information needed to execute an activity.
     */
    class ActivityInvocation implements Serializable {
        /** The unique ID of the activity occurrence. */
        private final IUniqueId activityOccurrenceId;
        /** The generation time of the activity invocation. */
        private final Instant generationTime;
        /** The ID of the activity. */
        private final int activityId;
        /** The path to the system entity. */
        private final SystemEntityPath path;
        /** The type of the activity. */
        private final String type;
        /** The map of arguments for the activity. */
        private final Map<String, Object> arguments;
        /** The map of properties for the activity. */
        private final Map<String, String> properties;
        /** The route for the activity. */
        private final String route;
        /** The source that originated the activity. */
        private final String source;

        /**
         * Constructs a new ActivityInvocation.
         *
         * @param activityOccurrenceId the unique ID of the activity occurrence
         * @param activityId the ID of the activity
         * @param generationTime the generation time
         * @param path the system entity path
         * @param type the activity type
         * @param arguments the map of arguments
         * @param properties the map of properties
         * @param route the route
         * @param source the source
         */
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

        /**
         * Returns the generation time.
         *
         * @return the generation time
         */
        public Instant getGenerationTime() {
            return generationTime;
        }

        /**
         * Returns the activity occurrence ID.
         *
         * @return the activity occurrence ID
         */
        public IUniqueId getActivityOccurrenceId() {
            return activityOccurrenceId;
        }

        /**
         * Returns the activity ID.
         *
         * @return the activity ID
         */
        public int getActivityId() {
            return activityId;
        }

        /**
         * Returns the system entity path.
         *
         * @return the system entity path
         */
        public SystemEntityPath getPath() {
            return path;
        }

        /**
         * Returns the activity type.
         *
         * @return the activity type
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the arguments map.
         *
         * @return the arguments map
         */
        public Map<String, Object> getArguments() {
            return arguments;
        }

        /**
         * Returns the properties map.
         *
         * @return the properties map
         */
        public Map<String, String> getProperties() {
            return properties;
        }

        /**
         * Returns the route.
         *
         * @return the route
         */
        public String getRoute() {
            return route;
        }

        /**
         * Returns the source.
         *
         * @return the source
         */
        public String getSource() {
            return source;
        }

        @Override
        /**
         * Returns a string representation of this ActivityInvocation.
         *
         * @return the string representation
         */
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
