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

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;

import java.util.List;

/**
 * This interface is used to request the execution of activities to the processing model.
 */
public interface IActivityExecutionService {

    /**
     * Request the execution of the specified activity. An activity occurrence created by this method is forwarded
     * to the identified {@link eu.dariolucia.reatmetric.api.processing.IActivityHandler} for transmission and
     * execution.
     *
     * @param request the activity request
     * @return the ID of the activity occurrence created by this request
     * @throws ReatmetricException in case of problems during the creation of the activity occurrence
     */
    IUniqueId startActivity(ActivityRequest request) throws ReatmetricException;

    /**
     * Register an existing activity occurrence in the processing model. This method is used to report the existence
     * of activity occurrences created outside the ReatMetric systems, e.g. autonomously by the controlled system.
     * An activity occurrence created by this method is not forwarded to any activity handler.
     *
     * @param request the activity occurrence characteristics in the form of request
     * @param currentProgress the current status of the occurrence
     * @return the ID of the activity occurrence as created in the processing model
     * @throws ReatmetricException in case of problems during the creation of the activity occurrence
     */
    IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ReatmetricException;

    /**
     * Purge the provided list of activity occurrences. The list is composed as a list of pairs: first value is the
     * activity ID (as per {@link ActivityOccurrenceData#getExternalId()}, second value is the activity occurrence ID
     * (as per {@link ActivityOccurrenceData#getInternalId()}.
     *
     * @param activityOccurrenceIds the list of {@link Pair} identifying the activity occurrences to purge
     * @throws ReatmetricException in case of problems purging the provided list of activity occurrences
     */
    void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ReatmetricException;

    /**
     * Retrieve the list of defined activity routes in the system and return them together with the current availability state.
     *
     * @return the availability state
     */
    List<ActivityRouteState> getRouteAvailability() throws ReatmetricException;

    // TODO add a way to set parameter value
    // IUniqueId setParameter(SetParameterRequest request) throws ReatmetricException


}
