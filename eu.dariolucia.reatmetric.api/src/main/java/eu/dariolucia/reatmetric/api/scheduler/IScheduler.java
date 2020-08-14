package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;

import java.time.Instant;
import java.util.List;

public interface IScheduler {

    void subscribe(ISchedulerSubscriber subscriber);

    void unsubscribe(ISchedulerSubscriber subscriber);

    void enable() throws SchedulingException;

    void disable() throws SchedulingException;

    boolean isEnabled() throws SchedulingException;

    ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    boolean remove(IUniqueId scheduledId) throws SchedulingException;

    boolean remove(ScheduledActivityDataFilter filter) throws SchedulingException;

    /**
     * This operation removes all the scheduled activities between startTime and endTime belonging to the provided scheduling source,
     * and adds the new scheduling requests. This is effectively the operation needed to replace schedule increments belonging to
     * a given source.
     *
     * If a merge is requested, method {@link IScheduler#schedule(List, CreationConflictStrategy)} should be used.
     *
     * @param startTime the start of the schedule increment
     * @param endTime the end of the schedule increments
     * @param requests the scheduling requests to add
     * @param source the source requesting the loading
     * @param conflictStrategy the conflict strategy to use with respect to resources
     * @return the list of scheduled operations
     */
    List<ScheduledActivityData> reload(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy);



}
