package eu.dariolucia.reatmetric.api.scheduler.input;

import eu.dariolucia.reatmetric.api.processing.input.AbstractInputDataItem;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.scheduler.AbstractSchedulingTrigger;
import eu.dariolucia.reatmetric.api.scheduler.ConflictStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public final class SchedulingRequest extends AbstractInputDataItem {

    private final ActivityRequest request;

    private final Set<String> resources;

    private final String source;

    private final long externalId;

    private final AbstractSchedulingTrigger trigger;

    private final Instant latestInvocationTime;

    private final ConflictStrategy conflictStrategy;

    private final Duration expectedDuration;

    public SchedulingRequest(ActivityRequest request, Set<String> resources, String source, long externalId, AbstractSchedulingTrigger trigger, Instant latestInvocationTime, ConflictStrategy conflictStrategy, Duration expectedDuration) {
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

    public Duration getExpectedDuration() {
        return expectedDuration;
    }
}

