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

package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public final class ScheduledActivityData extends AbstractDataItem {

    private final ActivityRequest request;

    private final IUniqueId activityOccurrence;

    private final Set<String> resources;

    private final String source;

    private final String externalId;

    private final AbstractSchedulingTrigger trigger;

    private final Instant latestInvocationTime;

    private final Instant startTime;
    private final Duration duration;

    private final ConflictStrategy conflictStrategy;

    private final SchedulingState state;

    public ScheduledActivityData(IUniqueId internalId, Instant generationTime, ActivityRequest request, IUniqueId activityOccurrence, Collection<String> resources, String source, String externalId, AbstractSchedulingTrigger trigger, Instant latestInvocationTime, Instant startTime, Duration duration, ConflictStrategy conflictStrategy, SchedulingState state, Object extension) {
        super(internalId, generationTime, extension);
        if(resources == null) {
            throw new NullPointerException("Resources cannot be null");
        }
        for(String res : resources) {
            if(res.isBlank()) {
                throw new IllegalArgumentException("Resource '" + res + "' is blank");
            } else if(res.indexOf(' ') != -1) {
                throw new IllegalArgumentException("Resource '" + res + "' contains whitespaces (forbidden)");
            }
        }
        this.request = request;
        this.activityOccurrence = activityOccurrence;
        this.resources = Set.copyOf(resources);
        this.source = source;
        this.externalId = externalId;
        this.trigger = trigger;
        this.latestInvocationTime = latestInvocationTime;
        this.conflictStrategy = conflictStrategy;
        this.state = state;
        this.startTime = startTime;
        this.duration = duration;
    }

    public ActivityRequest getRequest() {
        return request;
    }

    public IUniqueId getActivityOccurrence() {
        return activityOccurrence;
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

    public SchedulingState getState() {
        return state;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public boolean overlapsWith(Instant otherStartTime, Instant otherEndTime) {
        return (startTime.isAfter(otherStartTime) && startTime.isBefore(otherEndTime)) ||
                (startTime.plus(duration).isAfter(otherStartTime) && startTime.plus(duration).isBefore(otherEndTime));
    }

    public Instant getEndTime() {
        return startTime.plus(duration);
    }

    @Override
    public String toString() {
        return "ScheduledActivityData{" +
                "request=" + request +
                ", activityOccurrence=" + activityOccurrence +
                ", resources=" + resources +
                ", source='" + source + '\'' +
                ", externalId=" + externalId +
                ", trigger=" + trigger +
                ", latestInvocationTime=" + latestInvocationTime +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", conflictStrategy=" + conflictStrategy +
                ", state=" + state +
                ", endTime=" + getEndTime() +
                "} " + super.toString();
    }
}
