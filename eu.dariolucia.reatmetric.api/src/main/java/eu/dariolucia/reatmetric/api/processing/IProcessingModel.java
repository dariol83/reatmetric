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

public interface IProcessingModel extends IDebugInfoProvider {

    void injectParameters(List<ParameterSample> sampleList);

    void raiseEvent(EventOccurrence event);

    IUniqueId startActivity(ActivityRequest request) throws ProcessingModelException;

    IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ProcessingModelException;

    void reportActivityProgress(ActivityProgress progress);

    void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ProcessingModelException;

    void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ProcessingModelException;

    List<ActivityOccurrenceData> getActiveActivityOccurrences();

    IUniqueId setParameterValue(SetParameterRequest request) throws ProcessingModelException;
    // The model looks for the parameter processor, and depending on whether the parameter is settable or not,
    // the activity related to the setting is found (part of the parameter definition), and it is invoked with the
    // correct arguments. The following properties shall be allowed to be set: parameter external id as unsigned int,
    // provided value as argument source or eng, parameter path as string. Route must be provided in the invocation.

    void visit(IProcessingModelVisitor visitor);

    List<AbstractDataItem> get(AbstractDataItemFilter<?> filter);

    List<AbstractDataItem> getByPath(List<SystemEntityPath> paths) throws ProcessingModelException;

    List<AbstractDataItem> getById(List<Integer> ids) throws ProcessingModelException;

    void enable(SystemEntityPath path) throws ProcessingModelException;

    void disable(SystemEntityPath path) throws ProcessingModelException;

    void ignore(SystemEntityPath path) throws ProcessingModelException;

    SystemEntity getRoot() throws ProcessingModelException;

    List<SystemEntity> getContainedEntities(SystemEntityPath path) throws ProcessingModelException;

    SystemEntity getSystemEntityAt(SystemEntityPath path) throws ProcessingModelException;

    SystemEntity getSystemEntityOf(int id) throws ProcessingModelException;

    int getExternalIdOf(SystemEntityPath path) throws ProcessingModelException;

    SystemEntityPath getPathOf(int id) throws ProcessingModelException;

    AbstractSystemEntityDescriptor getDescriptorOf(int id) throws ProcessingModelException;

    AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ProcessingModelException;

    void registerActivityHandler(IActivityHandler handler) throws ProcessingModelException;

    void deregisterActivityHandler(IActivityHandler handler) throws ProcessingModelException;

    List<ActivityRouteState> getRouteAvailability() throws ProcessingModelException;

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
