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

    private final long externalId;

    private final AbstractSchedulingTrigger trigger;

    private final Instant latestInvocationTime;

    private final Instant startTime;
    private final Duration duration;

    private final ConflictStrategy conflictStrategy;

    private final SchedulingState state;

    public ScheduledActivityData(IUniqueId internalId, Instant generationTime, ActivityRequest request, IUniqueId activityOccurrence, Collection<String> resources, String source, long externalId, AbstractSchedulingTrigger trigger, Instant latestInvocationTime, Instant startTime, Duration duration, ConflictStrategy conflictStrategy, SchedulingState state, Object extension) {
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

    public long getExternalId() {
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
}
