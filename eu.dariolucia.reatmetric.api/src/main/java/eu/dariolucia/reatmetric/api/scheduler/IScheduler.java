package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.time.Instant;
import java.util.List;

/**
 * Interface implemented by a ReatMetric scheduler, allowing the change in the scheduler state as well as
 * the capability to create, update and remove scheduled activities.
 */
public interface IScheduler extends IScheduledActivityDataProvisionService {

    void initialise() throws SchedulingException;

    /**
     * Subscribe the provided subscriber to the scheduler, to know the current scheduler state.
     *
     * @param subscriber the subscriber
     */
    void subscribe(ISchedulerSubscriber subscriber);

    /**
     * Unsubscribe the provided subscriber.
     *
     * @param subscriber the subscriber
     */
    void unsubscribe(ISchedulerSubscriber subscriber);

    void enable() throws SchedulingException;

    void disable() throws SchedulingException;

    boolean isEnabled() throws SchedulingException;

    ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    /**
     * Return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}.
     *
     * @return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}
     * @throws SchedulingException in case of issues when executing the method
     */
    List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException;

    /**
     * Update the scheduled activity indicated by originalId with the new data coming from the provided {@link SchedulingRequest}.
     *
     * The scheduled activity can be updated only if it is in the {@link SchedulingState#SCHEDULED} state.
     *
     * @param originalId the original scheduled activity to update
     * @param newRequest the new request
     * @param conflictStrategy the creation conflict strategy to be used in case of resource conflict
     * @return the updated scheduled activity data
     * @throws SchedulingException in case the conflict strategy does not allow proper resolution or in case the scheduled activity is not
     * in the expected state
     */
    ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException;

    void remove(IUniqueId scheduledId) throws SchedulingException;

    void remove(ScheduledActivityDataFilter filter) throws SchedulingException;

    /**
     * This operation removes all the scheduled activities between startTime and endTime belonging to the provided scheduling source,
     * and adds the new scheduling requests. This is effectively the operation needed to replace schedule increments belonging to
     * a given source.
     *
     * If a merge is instead requested, method {@link IScheduler#schedule(List, CreationConflictStrategy)} should be used.
     *
     * @param startTime the start of the schedule increment
     * @param endTime the end of the schedule increments
     * @param requests the scheduling requests to add
     * @param source the source requesting the loading
     * @param conflictStrategy the conflict strategy to use with respect to resources
     * @return the list of scheduled operations
     */
    List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy);



}
