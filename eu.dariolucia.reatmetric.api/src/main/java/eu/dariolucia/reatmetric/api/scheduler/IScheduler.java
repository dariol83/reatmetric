package eu.dariolucia.reatmetric.api.scheduler;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.scheduler.exceptions.SchedulingException;
import eu.dariolucia.reatmetric.api.scheduler.input.SchedulingRequest;

import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;

/**
 * Interface implemented by a ReatMetric scheduler, allowing the change in the scheduler state as well as
 * the capability to create, update, remove scheduled activities and enable/disable the configured bots.
 */
public interface IScheduler extends IScheduledActivityDataProvisionService {

    void initialise() throws SchedulingException, RemoteException;

    /**
     * Subscribe the provided subscriber to the scheduler, to know the current scheduler state.
     *
     * @param subscriber the subscriber
     */
    void subscribe(ISchedulerSubscriber subscriber) throws RemoteException;

    /**
     * Unsubscribe the provided subscriber.
     *
     * @param subscriber the subscriber
     */
    void unsubscribe(ISchedulerSubscriber subscriber) throws RemoteException;

    void enable() throws SchedulingException, RemoteException;

    void disable() throws SchedulingException, RemoteException;

    boolean isEnabled() throws SchedulingException, RemoteException;

    ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}.
     *
     * @return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}
     * @throws SchedulingException in case of issues when executing the method
     */
    List<ScheduledActivityData> getCurrentScheduledActivities() throws SchedulingException, RemoteException;

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
    ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    void remove(IUniqueId scheduledId) throws SchedulingException, RemoteException;

    void remove(ScheduledActivityDataFilter filter) throws SchedulingException, RemoteException;

    /**
     * This operation removes all the scheduled activities between startTime and endTime belonging to the provided scheduling source,
     * and adds the new scheduling requests. This is effectively the operation needed to replace schedule increments belonging to
     * a given source.
     *
     * If there are activities already under execution in the provided time period from the requesting source, the load operation will
     * fail with an exception and the schedule will not be modified.
     *
     * If there is a conflict (resource-wise) between the new scheduling requests and the scheduled activities, then this is handled
     * according to the specified conflictStrategy.
     *
     * If a merge with existing activities from the same source is instead requested, the method {@link IScheduler#schedule(List, CreationConflictStrategy)}
     * should be used instead.
     *
     * @param startTime the start of the schedule increment
     * @param endTime the end of the schedule increments
     * @param requests the scheduling requests to add
     * @param source the source requesting the loading
     * @param conflictStrategy the conflict strategy to use with respect to resources
     * @return the list of scheduled operations
     * @throws SchedulingException in case of already running activities in the defined time interval for the requesting source
     */
    List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Enable the bot identified by the provided name. Once re-enabled, the bot will move to the uninitialised state and
     * it will compute the first valid state.
     *
     * @param name the name of the bot to enable
     * @throws SchedulingException in case of errors in the scheduler
     */
    void enableBot(String name) throws SchedulingException, RemoteException;

    /**
     * Disable the bot identified by the provided name.
     *
     * @param name the name of the bot to disable
     * @throws SchedulingException in case of errors in the scheduler
     */
    void disableBot(String name) throws SchedulingException, RemoteException;
    /**
     * Dispose the scheduler. Pending tasks will not be modified, to allow resume (if the archiving is enabled).
     */
    void dispose() throws RemoteException;
}
