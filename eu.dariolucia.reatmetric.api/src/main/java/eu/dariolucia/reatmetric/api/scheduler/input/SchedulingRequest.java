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

package eu.dariolucia.reatmetric.api.scheduler.input;

import eu.dariolucia.reatmetric.api.processing.input.AbstractInputDataItem;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.AbsoluteTimeSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.AbstractSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.ConflictStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a request to schedule an activity.
 * Contains all the information needed to create a scheduled activity.
 */
public final class SchedulingRequest extends AbstractInputDataItem {

    /**
     * Creates a new Builder for a SchedulingRequest.
     *
     * @param activityRequest the activity request to schedule
     * @param source the source requesting the scheduling
     * @param externalId the external ID for the scheduled activity
     * @param expectedDuration the expected duration of the activity
     * @return a new Builder instance
     */
    public static SchedulingRequest.Builder newRequest(ActivityRequest activityRequest, String source, String externalId, Duration expectedDuration) {
        return new Builder(activityRequest, source, externalId, expectedDuration);
    }

    /** The activity request. */
    private final ActivityRequest request;

    /** The set of resource identifiers. */
    private final Set<String> resources;

    /** The source requesting the scheduling. */
    private final String source;

    /** The external identifier for the scheduled activity. */
    private final String externalId;

    /** The trigger that determines when the activity should be executed. */
    private final AbstractSchedulingTrigger trigger;

    /** The latest time at which the activity can be invoked. */
    private final Instant latestInvocationTime;

    /** The strategy to handle scheduling conflicts. */
    private final ConflictStrategy conflictStrategy;

    /** The expected duration of the activity. */
    private final Duration expectedDuration;

    /**
     * Constructs a SchedulingRequest.
     *
     * @param request the activity request
     * @param resources the set of resource identifiers
     * @param source the source requesting the scheduling
     * @param externalId the external identifier for the scheduled activity
     * @param trigger the trigger that determines when the activity should be executed
     * @param latestInvocationTime the latest time at which the activity can be invoked (may be null)
     * @param conflictStrategy the strategy to handle scheduling conflicts (may be null, defaults to WAIT)
     * @param expectedDuration the expected duration of the activity (must not be null)
     * @throws NullPointerException if expectedDuration is null
     */
    public SchedulingRequest(ActivityRequest request, Set<String> resources, String source, String externalId, AbstractSchedulingTrigger trigger, Instant latestInvocationTime, ConflictStrategy conflictStrategy, Duration expectedDuration) {
        if(expectedDuration == null) {
            throw new NullPointerException("Expected duration must be provided");
        }
        this.request = request;
        this.resources = Set.copyOf(resources);
        this.source = source;
        this.externalId = externalId;
        this.trigger = trigger;
        this.latestInvocationTime = latestInvocationTime;
        this.conflictStrategy = conflictStrategy;
        this.expectedDuration = expectedDuration;
    }

    /**
     * Returns the activity request.
     *
     * @return the activity request
     */
    public ActivityRequest getRequest() {
        return request;
    }

    /**
     * Returns the set of resource identifiers.
     *
     * @return the set of resource identifiers
     */
    public Set<String> getResources() {
        return resources;
    }

    /**
     * Returns the source requesting the scheduling.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the external identifier for the scheduled activity.
     *
     * @return the external identifier
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Returns the trigger that determines when the activity should be executed.
     *
     * @return the scheduling trigger
     */
    public AbstractSchedulingTrigger getTrigger() {
        return trigger;
    }

    /**
     * Returns the latest time at which the activity can be invoked.
     *
     * @return the latest invocation time, or null if not specified
     */
    public Instant getLatestInvocationTime() {
        return latestInvocationTime;
    }

    /**
     * Returns the strategy to handle scheduling conflicts.
     *
     * @return the conflict strategy, or null if not specified
     */
    public ConflictStrategy getConflictStrategy() {
        return conflictStrategy;
    }

    /**
     * Returns the expected duration of the activity.
     *
     * @return the expected duration
     */
    public Duration getExpectedDuration() {
        return expectedDuration;
    }

    @Override
    /**
     * Returns a string representation of this scheduling request.
     *
     * @return a string representation of this scheduling request
     */
    public String toString() {
        return "SchedulingRequest{" +
                "request=" + request +
                ", resources=" + resources +
                ", source='" + source + '\'' +
                ", externalId='" + externalId + '\'' +
                ", trigger=" + trigger +
                ", latestInvocationTime=" + latestInvocationTime +
                ", conflictStrategy=" + conflictStrategy +
                ", expectedDuration=" + expectedDuration +
                '}';
    }

    /**
     * Builder for creating SchedulingRequest instances.
     */
    public static class Builder {
        /** The activity request. */
        private final ActivityRequest activityRequest;
        /** The source requesting the scheduling. */
        private final String source;
        /** The external identifier for the scheduled activity. */
        private final String externalId;
        /** The expected duration of the activity. */
        private final Duration expectedDuration;
        /** The set of resource identifiers. */
        private final Set<String> resources = new LinkedHashSet<>();
        /** The latest time at which the activity can be invoked. */
        private Instant latestInvocationTime;
        /** The strategy to handle scheduling conflicts, defaults to WAIT. */
        private ConflictStrategy conflictStrategy = ConflictStrategy.WAIT;

        /**
         * Constructs a Builder.
         *
         * @param activityRequest the activity request to schedule
         * @param source the source requesting the scheduling
         * @param externalId the external ID for the scheduled activity
         * @param expectedDuration the expected duration of the activity
         */
        public Builder(ActivityRequest activityRequest, String source, String externalId, Duration expectedDuration) {
            this.activityRequest = activityRequest;
            this.source = source;
            this.externalId = externalId;
            this.expectedDuration = expectedDuration;
        }

        /**
         * Adds a single resource to the request.
         *
         * @param resource the resource identifier to add
         * @return this Builder for chaining
         */
        public Builder withResource(String resource) {
            this.resources.add(resource);
            return this;
        }

        /**
         * Adds multiple resources to the request.
         *
         * @param resources the resource identifiers to add
         * @return this Builder for chaining
         */
        public Builder withResources(String... resources) {
            this.resources.addAll(Arrays.asList(resources));
            return this;
        }

        /**
         * Adds a collection of resources to the request.
         *
         * @param resources the collection of resource identifiers to add
         * @return this Builder for chaining
         */
        public Builder withResources(Collection<String> resources) {
            this.resources.addAll(resources);
            return this;
        }

        /**
         * Sets the latest invocation time for the activity.
         *
         * @param time the latest time at which the activity can be invoked
         * @return this Builder for chaining
         */
        public Builder withLatestInvocationTime(Instant time) {
            this.latestInvocationTime = time;
            return this;
        }

        /**
         * Sets the conflict strategy for the activity.
         *
         * @param conflictStrategy the strategy to handle scheduling conflicts
         * @return this Builder for chaining
         */
        public Builder withConflictStrategy(ConflictStrategy conflictStrategy) {
            this.conflictStrategy = conflictStrategy;
            return this;
        }

        /**
         * Builds the SchedulingRequest with the specified trigger.
         *
         * @param trigger the trigger that determines when the activity should be executed
         * @return the constructed SchedulingRequest
         */
        public SchedulingRequest build(AbstractSchedulingTrigger trigger) {
            return new SchedulingRequest(activityRequest,resources,source,externalId,trigger,latestInvocationTime,conflictStrategy,expectedDuration);
        }
    }
}

