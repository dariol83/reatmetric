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

public final class SchedulingRequest extends AbstractInputDataItem {

    public static SchedulingRequest.Builder newRequest(ActivityRequest activityRequest, String source, String externalId, Duration expectedDuration) {
        return new Builder(activityRequest, source, externalId, expectedDuration);
    }

    private final ActivityRequest request;

    private final Set<String> resources;

    private final String source;

    private final String externalId;

    private final AbstractSchedulingTrigger trigger;

    private final Instant latestInvocationTime;

    private final ConflictStrategy conflictStrategy;

    private final Duration expectedDuration;

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

    public ActivityRequest getRequest() {
        return request;
    }

    public Set<String> getResources() {
        return resources;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public AbstractSchedulingTrigger getTrigger() {
        return trigger;
    }

    public Instant getLatestInvocationTime() {
        return latestInvocationTime;
    }

    public ConflictStrategy getConflictStrategy() {
        return conflictStrategy;
    }

    public Duration getExpectedDuration() {
        return expectedDuration;
    }

    @Override
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

    public static class Builder {
        private final ActivityRequest activityRequest;
        private final String source;
        private final String externalId;
        private final Duration expectedDuration;
        private final Set<String> resources = new LinkedHashSet<>();
        private Instant latestInvocationTime;
        private ConflictStrategy conflictStrategy = ConflictStrategy.WAIT;

        public Builder(ActivityRequest activityRequest, String source, String externalId, Duration expectedDuration) {
            this.activityRequest = activityRequest;
            this.source = source;
            this.externalId = externalId;
            this.expectedDuration = expectedDuration;
        }

        public Builder withResource(String resource) {
            this.resources.add(resource);
            return this;
        }

        public Builder withResources(String... resources) {
            this.resources.addAll(Arrays.asList(resources));
            return this;
        }

        public Builder withResources(Collection<String> resources) {
            this.resources.addAll(resources);
            return this;
        }

        public Builder withLatestInvocationTime(Instant time) {
            this.latestInvocationTime = time;
            return this;
        }

        public Builder withConflictStrategy(ConflictStrategy conflictStrategy) {
            this.conflictStrategy = conflictStrategy;
            return this;
        }

        public SchedulingRequest build(AbstractSchedulingTrigger trigger) {
            return new SchedulingRequest(activityRequest,resources,source,externalId,trigger,latestInvocationTime,conflictStrategy,expectedDuration);
        }
    }
}

