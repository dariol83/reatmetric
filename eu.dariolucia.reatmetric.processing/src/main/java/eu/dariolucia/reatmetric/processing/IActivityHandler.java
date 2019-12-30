/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.util.List;
import java.util.Map;

public interface IActivityHandler {

    void registerModel(IProcessingModel model);

    void deregisterModel(IProcessingModel model);

    List<String> getSupportedRoutes();

    List<String> getSupportedActivityTypes();

    void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException;

    class ActivityInvocation {
        private final IUniqueId activityOccurrenceId;
        private final int activityId;
        private final SystemEntityPath path;
        private final String type;
        private final Map<String, Object> arguments;
        private final Map<String, String> properties;
        private final String route;

        public ActivityInvocation(IUniqueId activityOccurrenceId, int activityId, SystemEntityPath path, String type, Map<String, Object> arguments, Map<String, String> properties, String route) {
            this.activityOccurrenceId = activityOccurrenceId;
            this.activityId = activityId;
            this.path = path;
            this.type = type;
            this.arguments = arguments;
            this.properties = properties;
            this.route = route;
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

        @Override
        public String toString() {
            return "ActivityInvocation{" +
                    "activityOccurrenceId=" + activityOccurrenceId +
                    ", activityId=" + activityId +
                    ", path=" + path +
                    ", type='" + type + '\'' +
                    ", arguments=" + arguments +
                    ", properties=" + properties +
                    ", route='" + route + '\'' +
                    '}';
        }
    }
}
