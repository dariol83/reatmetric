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

/**
 * Represents a scheduled activity in the ReatMetric system.
 * Contains all the information about a scheduled activity including its request, resources, timing, and state.
 */
public final class ScheduledActivityData extends AbstractDataItem {

    /** The activity request. */
    private final ActivityRequest request;

    /** The activity occurrence ID. */
    private final IUniqueId activityOccurrence;

    /** The set of resources required by this scheduled activity. */
    private final Set<String> resources;

    /** The source that created this scheduled activity. */
    private final String source;

    /** The external ID. */
    private final String externalId;

    /** The scheduling trigger. */
    private final AbstractSchedulingTrigger trigger;

    /** The latest invocation time. */
    private final Instant latestInvocationTime;

    /** The start time of the scheduled activity. */
    private final Instant startTime;
    /** The duration of the scheduled activity. */
    private final Duration duration;

    /** The conflict strategy. */
    private final ConflictStrategy conflictStrategy;

    /** The scheduling state. */
    private final SchedulingState state;

    /**
     * Constructs a new ScheduledActivityData.
     *
     * @param internalId the internal unique ID
     * @param generationTime the generation time
     * @param request the activity request
     * @param activityOccurrence the activity occurrence ID, can be null
     * @param resources the collection of resources required, cannot be null and cannot contain blank or whitespace strings
     * @param source the source, can be null
     * @param externalId the external ID, can be null
     * @param trigger the scheduling trigger, can be null
     * @param latestInvocationTime the latest invocation time, can be null
     * @param startTime the start time, can be null
     * @param duration the duration, can be null
     * @param conflictStrategy the conflict strategy, can be null
     * @param state the scheduling state, can be null
     * @param extension the extension object, can be null
     * @throws NullPointerException if resources is null
     * @throws IllegalArgumentException if any resource is blank or contains whitespace
     */
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

    /**
     * Returns the activity request.
     *
     * @return the activity request, may be null
     */
    public ActivityRequest getRequest() {
        return request;
    }

    /**
     * Returns the activity occurrence ID.
     *
     * @return the activity occurrence ID, may be null
     */
    public IUniqueId getActivityOccurrence() {
        return activityOccurrence;
    }

    /**
     * Returns the set of resources required by this scheduled activity.
     *
     * @return the set of resources, never null
     */
    public Set<String> getResources() {
        return resources;
    }

    /**
     * Returns the source that created this scheduled activity.
     *
     * @return the source, may be null
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the external ID.
     *
     * @return the external ID, may be null
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Returns the scheduling trigger.
     *
     * @return the scheduling trigger, may be null
     */
    public AbstractSchedulingTrigger getTrigger() {
        return trigger;
    }

    /**
     * Returns the latest invocation time.
     *
     * @return the latest invocation time, may be null
     */
    public Instant getLatestInvocationTime() {
        return latestInvocationTime;
    }

    /**
     * Returns the conflict strategy.
     *
     * @return the conflict strategy, may be null
     */
    public ConflictStrategy getConflictStrategy() {
        return conflictStrategy;
    }

    /**
     * Returns the scheduling state.
     *
     * @return the scheduling state, may be null
     */
    public SchedulingState getState() {
        return state;
    }

    /**
     * Returns the start time of the scheduled activity.
     *
     * @return the start time, may be null
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns the duration of the scheduled activity.
     *
     * @return the duration, may be null
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Checks if this scheduled activity overlaps with the specified time range.
     *
     * @param otherStartTime the start time of the other range
     * @param otherEndTime the end time of the other range
     * @return true if there is an overlap, false otherwise
     */
    public boolean overlapsWith(Instant otherStartTime, Instant otherEndTime) {
        return (startTime.isAfter(otherStartTime) && startTime.isBefore(otherEndTime)) ||
                (startTime.plus(duration).isAfter(otherStartTime) && startTime.plus(duration).isBefore(otherEndTime));
    }

    /**
     * Returns the end time of this scheduled activity.
     *
     * @return the end time (startTime + duration)
     */
    public Instant getEndTime() {
        return startTime.plus(duration);
    }

    @Override
    /**
     * Returns a string representation of this ScheduledActivityData.
     *
     * @return the string representation
     */
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
