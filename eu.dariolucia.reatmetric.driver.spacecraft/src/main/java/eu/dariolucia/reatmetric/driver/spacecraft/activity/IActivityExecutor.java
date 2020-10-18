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

package eu.dariolucia.reatmetric.driver.spacecraft.activity;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;

import java.util.List;

public interface IActivityExecutor {

    /**
     * Execute the provided activity occurrence.
     *
     * @param activityInvocation the activity occurrence to execute
     * @throws ActivityHandlingException in case of errors during the activity handling process
     */
    void executeActivity(IActivityHandler.ActivityInvocation activityInvocation) throws ActivityHandlingException;

    /**
     * Return the list of the supported activity types.
     *
     * @return the list of supported activity types
     */
    List<String> getSupportedActivityTypes();

    /**
     * Return the list of the supported routes.
     *
     * @return the list of supported routes
     */
    List<String> getSupportedRoutes();

    /**
     * Abort the specified activity occurrence, if present. Implementors of this method shall not report any exception,
     * if the occurrence is not handled by them, but silently return.
     *
     * @param activityId the activity ID
     * @param activityOccurrenceId the activity occurrence ID to abort
     */
    void abort(int activityId, IUniqueId activityOccurrenceId);

}
