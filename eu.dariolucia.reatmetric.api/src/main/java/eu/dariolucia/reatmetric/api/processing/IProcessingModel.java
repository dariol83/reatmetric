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

package eu.dariolucia.reatmetric.api.processing;

import eu.dariolucia.reatmetric.api.activity.ActivityOccurrenceData;
import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.common.*;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.api.processing.input.*;

import java.util.List;

/**
 * Interface representing a processing model in the ReatMetric system.
 * A processing model manages system entities, parameters, events, and activities,
 * and provides operations for querying and manipulating the model state.
 */
public interface IProcessingModel extends IDebugInfoProvider {

    /**
     * Injects parameter samples into the processing model.
     *
     * @param sampleList the list of parameter samples to inject
     */
    void injectParameters(List<ParameterSample> sampleList);

    /**
     * Raises an event occurrence in the processing model.
     *
     * @param event the event occurrence to raise
     */
    void raiseEvent(EventOccurrence event);

    /**
     * Starts an activity based on the request.
     *
     * @param request the activity request
     * @return the unique ID of the started activity occurrence
     * @throws ProcessingModelException if the activity cannot be started
     */
    IUniqueId startActivity(ActivityRequest request) throws ProcessingModelException;

    /**
     * Creates an activity with the specified request and initial progress.
     *
     * @param request the activity request
     * @param currentProgress the initial progress of the activity
     * @return the unique ID of the created activity occurrence
     * @throws ProcessingModelException if the activity cannot be created
     */
    IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ProcessingModelException;

    /**
     * Reports progress for an activity.
     *
     * @param progress the activity progress to report
     */
    void reportActivityProgress(ActivityProgress progress);

    /**
     * Purges the specified activities from the model.
     *
     * @param activityOccurrenceIds the list of activity IDs and occurrence IDs to purge
     * @throws ProcessingModelException if the activities cannot be purged
     */
    void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ProcessingModelException;

    /**
     * Aborts an activity.
     *
     * @param activityId the ID of the activity to abort
     * @param activityOccurrenceId the occurrence ID of the activity to abort
     * @throws ProcessingModelException if the activity cannot be aborted
     */
    void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ProcessingModelException;

    /**
     * Returns the list of active activity occurrences.
     *
     * @return the list of active activity occurrences
     */
    List<ActivityOccurrenceData> getActiveActivityOccurrences();

    /**
     * The model looks for the parameter processor, and depending on whether the parameter is settable or not,
     * the activity related to the setting is found (part of the parameter definition), and it is invoked with the
     * correct arguments. The following properties shall be allowed to be set: parameter external id as unsigned int,
     * provided value as argument source or eng, parameter path as string. Route must be provided in the invocation.
     * If the targeted parameter is a user-parameter and no setter is defined, the request translates internally into a
     * parameter injection, and the return value is null.
     *
     * @param request the request to set a parameter to a given value
     * @return the {@link IUniqueId} of the generated activity, or null in case of user-parameter
     * @throws ProcessingModelException in case of issues to translate the request into an effective, workable task
     */
    IUniqueId setParameterValue(SetParameterRequest request) throws ProcessingModelException;

    /**
     * Accepts a visitor to traverse the processing model.
     *
     * @param visitor the visitor to accept
     */
    void visit(IProcessingModelVisitor visitor);

    /**
     * Gets data items matching the specified filter.
     *
     * @param filter the filter to apply
     * @return the list of matching data items
     */
    List<AbstractDataItem> get(AbstractDataItemFilter<?> filter);

    /**
     * Gets data items by their paths.
     *
     * @param paths the list of system entity paths
     * @return the list of data items at the specified paths
     * @throws ProcessingModelException if the items cannot be retrieved
     */
    List<AbstractDataItem> getByPath(List<SystemEntityPath> paths) throws ProcessingModelException;

    /**
     * Gets data items by their IDs.
     *
     * @param ids the list of IDs
     * @return the list of data items with the specified IDs
     * @throws ProcessingModelException if the items cannot be retrieved
     */
    List<AbstractDataItem> getById(List<Integer> ids) throws ProcessingModelException;

    /**
     * Enables the system entity at the specified path.
     *
     * @param path the path to the system entity to enable
     * @throws ProcessingModelException if the entity cannot be enabled
     */
    void enable(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Disables the system entity at the specified path.
     *
     * @param path the path to the system entity to disable
     * @throws ProcessingModelException if the entity cannot be disabled
     */
    void disable(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Ignores the system entity at the specified path.
     *
     * @param path the path to the system entity to ignore
     * @throws ProcessingModelException if the entity cannot be ignored
     */
    void ignore(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Returns the root system entity of the processing model.
     *
     * @return the root system entity
     * @throws ProcessingModelException if the root entity cannot be retrieved
     */
    SystemEntity getRoot() throws ProcessingModelException;

    /**
     * Returns the list of system entities contained at the specified path.
     *
     * @param path the path to query
     * @return the list of contained system entities
     * @throws ProcessingModelException if the entities cannot be retrieved
     */
    List<SystemEntity> getContainedEntities(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Returns the system entity at the specified path.
     *
     * @param path the path to the system entity
     * @return the system entity at the specified path
     * @throws ProcessingModelException if the entity cannot be retrieved
     */
    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Returns the system entity with the specified ID.
     *
     * @param id the ID of the system entity
     * @return the system entity with the specified ID
     * @throws ProcessingModelException if the entity cannot be retrieved
     */
    SystemEntity getSystemEntityOf(int id) throws ProcessingModelException;

    /**
     * Returns the external ID of the system entity at the specified path.
     *
     * @param path the path to the system entity
     * @return the external ID
     * @throws ProcessingModelException if the external ID cannot be retrieved
     */
    int getExternalIdOf(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Returns the path of the system entity with the specified ID.
     *
     * @param id the ID of the system entity
     * @return the path to the system entity
     * @throws ProcessingModelException if the path cannot be retrieved
     */
    SystemEntityPath getPathOf(int id) throws ProcessingModelException;

    /**
     * Returns the descriptor of the system entity with the specified ID.
     *
     * @param id the ID of the system entity
     * @return the system entity descriptor
     * @throws ProcessingModelException if the descriptor cannot be retrieved
     */
    AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ProcessingModelException;

    /**
     * Returns the descriptor of the system entity at the specified path.
     *
     * @param path the path to the system entity
     * @return the system entity descriptor
     * @throws ProcessingModelException if the descriptor cannot be retrieved
     */
    AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ProcessingModelException;

    /**
     * Registers an activity handler with this processing model.
     *
     * @param handler the activity handler to register
     * @throws ProcessingModelException if the handler cannot be registered
     */
    void registerActivityHandler(IActivityHandler handler) throws ProcessingModelException;

    /**
     * Deregisters an activity handler from this processing model.
     *
     * @param handler the activity handler to deregister
     * @throws ProcessingModelException if the handler cannot be deregistered
     */
    void deregisterActivityHandler(IActivityHandler handler) throws ProcessingModelException;

    /**
     * Returns the availability state of all routes.
     *
     * @return the list of route availability states
     * @throws ProcessingModelException if the route availability cannot be determined
     */
    List<ActivityRouteState> getRouteAvailability() throws ProcessingModelException;

    /**
     * Returns the availability state of routes for a specific type.
     *
     * @param type the activity type to filter by
     * @return the list of route availability states for the specified type
     * @throws ProcessingModelException if the route availability cannot be determined
     */
    List<ActivityRouteState> getRouteAvailability(String type) throws ProcessingModelException;

    /**
     * Apply the provided list of data items (of type {@link eu.dariolucia.reatmetric.api.parameters.ParameterData},
     * {@link eu.dariolucia.reatmetric.api.events.EventData}, {@link ActivityOccurrenceData}) to the items in this
     * processing model. All items reported here must map to processing model object marked as 'mirrored', or a warning
     * will be raised.
     *
     * The idea of this method is to allow distribution of the processing model onto several processes and to
     * enable a master-slave, or low-level vs high-level monitoring concept. The monitoring driver is responsible to map
     * the incoming data item to the corresponding ID/path.
     *
     * @param items the list of data item states that must be mirrored by the processing model
     * @throws ProcessingModelException in case of issues when applying the update
     */
    void mirror(List<AbstractDataItem> items) throws ProcessingModelException;
}
