package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;

import java.time.Instant;
import java.util.Set;

public final class ScheduledActivityData extends AbstractDataItem {

    private final ActivityRequest request;

    private final IUniqueId activityOccurrence;

    private final Set<String> resources;

    private final String source;

    private final long externalId;

    private final AbstractSchedulingTrigger trigger;

    private final Instant latestInvocationTime;

    private final ConflictStrategy conflictStrategy;

    private final SchedulingState state;

    public ScheduledActivityData(IUniqueId internalId, Instant generationTime, ActivityRequest request, IUniqueId activityOccurrence, Set<String> resources, String source, long externalId, AbstractSchedulingTrigger trigger, Instant latestInvocationTime, ConflictStrategy conflictStrategy, SchedulingState state, Object extension) {
        super(internalId, generationTime, extension);
        this.request = request;
        this.activityOccurrence = activityOccurrence;
        this.resources = resources;
        this.source = source;
        this.externalId = externalId;
        this.trigger = trigger;
        this.latestInvocationTime = latestInvocationTime;
        this.conflictStrategy = conflictStrategy;
        this.state = state;
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
}
