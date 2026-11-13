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

    /**
     * Initializes the scheduler.
     *
     * @throws SchedulingException in case of initialization errors
     * @throws RemoteException in case of issues on the remote side
     */
    void initialise() throws SchedulingException, RemoteException;

    /**
     * Subscribe the provided subscriber to the scheduler, to know the current scheduler state.
     *
     * @param subscriber the subscriber
     */
    /**
     * Subscribe the provided subscriber to the scheduler, to know the current scheduler state.
     *
     * @param subscriber the subscriber
     * @throws RemoteException in case of issues on the remote side
     */
    void subscribe(ISchedulerSubscriber subscriber) throws RemoteException;

    /**
     * Unsubscribe the provided subscriber.
     *
     * @param subscriber the subscriber
     * @throws RemoteException in case of issues on the remote side
     */
    void unsubscribe(ISchedulerSubscriber subscriber) throws RemoteException;

    /**
     * Enables the scheduler.
     *
     * @throws SchedulingException in case of errors enabling the scheduler
     * @throws RemoteException in case of issues on the remote side
     */
    void enable() throws SchedulingException, RemoteException;

    /**
     * Disables the scheduler.
     *
     * @throws SchedulingException in case of errors disabling the scheduler
     * @throws RemoteException in case of issues on the remote side
     */
    void disable() throws SchedulingException, RemoteException;

    /**
     * Returns whether the scheduler is enabled.
     *
     * @return true if enabled, false otherwise
     * @throws SchedulingException in case of errors checking the scheduler state
     * @throws RemoteException in case of issues on the remote side
     */
    boolean isEnabled() throws SchedulingException, RemoteException;

    /**
     * Schedules a single activity request.
     *
     * @param request the scheduling request
     * @param conflictStrategy the strategy to handle conflicts
     * @return the scheduled activity data
     * @throws SchedulingException in case of scheduling errors
     * @throws RemoteException in case of issues on the remote side
     */
    ScheduledActivityData schedule(SchedulingRequest request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Schedules multiple activity requests.
     *
     * @param request the list of scheduling requests
     * @param conflictStrategy the strategy to handle conflicts
     * @return the list of scheduled activity data
     * @throws SchedulingException in case of scheduling errors
     * @throws RemoteException in case of issues on the remote side
     */
    List<ScheduledActivityData> schedule(List<SchedulingRequest> request, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}.
     *
     * @return the list of the activities currently in the state {@link SchedulingState#SCHEDULED}
     * @throws SchedulingException in case of issues when executing the method
     * @throws RemoteException in case of issues on the remote side
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
     * @throws RemoteException in case of issues on the remote side
     */
    ScheduledActivityData update(IUniqueId originalId, SchedulingRequest newRequest, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Removes a scheduled activity by its ID.
     *
     * @param scheduledId the ID of the scheduled activity to remove
     * @throws SchedulingException in case of errors removing the scheduled activity
     * @throws RemoteException in case of issues on the remote side
     */
    void remove(IUniqueId scheduledId) throws SchedulingException, RemoteException;

    /**
     * Removes scheduled activities matching the filter.
     *
     * @param filter the filter to select activities to remove
     * @throws SchedulingException in case of errors removing the scheduled activities
     * @throws RemoteException in case of issues on the remote side
     */
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
     * @throws RemoteException in case of issues on the remote side
     */
    List<ScheduledActivityData> load(Instant startTime, Instant endTime, List<SchedulingRequest> requests, String source, CreationConflictStrategy conflictStrategy) throws SchedulingException, RemoteException;

    /**
     * Enable the bot identified by the provided name. Once re-enabled, the bot will move to the uninitialised state and
     * it will compute the first valid state.
     *
     * @param name the name of the bot to enable
     * @throws SchedulingException in case of errors in the scheduler
     * @throws RemoteException in case of issues on the remote side
     */
    void enableBot(String name) throws SchedulingException, RemoteException;

    /**
     * Disable the bot identified by the provided name.
     *
     * @param name the name of the bot to disable
     * @throws SchedulingException in case of errors in the scheduler
     * @throws RemoteException in case of issues on the remote side
     */
    void disableBot(String name) throws SchedulingException, RemoteException;
    
    /**
     * Dispose the scheduler. Pending tasks will not be modified, to allow resume (if the archiving is enabled).
     *
     * @throws RemoteException in case of issues on the remote side
     */
    void dispose() throws RemoteException;
}
