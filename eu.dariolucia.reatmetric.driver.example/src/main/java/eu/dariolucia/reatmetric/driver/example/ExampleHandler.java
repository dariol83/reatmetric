/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.example;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.exceptions.ActivityHandlingException;

import java.util.Collections;
import java.util.List;

public class ExampleHandler implements IActivityHandler {

    private final ExampleDriver driver;

    public ExampleHandler(ExampleDriver driver) {
        this.driver = driver;
    }

    @Override
    public void registerModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public void deregisterModel(IProcessingModel model) {
        // Not needed
    }

    @Override
    public List<String> getSupportedRoutes() {
        return Collections.singletonList(ExampleDriver.ROUTE_NAME);
    }

    @Override
    public List<String> getSupportedActivityTypes() {
        return Collections.singletonList(ExampleDriver.ACTIVITY_TYPE);
    }

    @Override
    public void executeActivity(ActivityInvocation activityInvocation) throws ActivityHandlingException {
        // Check if the connector is active
        if(!driver.isConnectorStarted()) {
            throw new ActivityHandlingException("Connector not started");
        }
        // Check if the route is OK
        if(!activityInvocation.getRoute().equals(ExampleDriver.ROUTE_NAME)) {
            throw new ActivityHandlingException("Route mismatch");
        }
        // Check if the activity is the one you expect (ID and path are matching)
        if(!driver.isActivitySupported(activityInvocation.getPath(), activityInvocation.getActivityId())) {
            throw new ActivityHandlingException("ID/Path mismatch");
        }
        // If so, inform that the RELEASE is done and invoke the request asynchronously to the connector
        // (a service executor would help, but this is an example)
        new Thread(() -> {
            driver.executeCounterReset(activityInvocation);
        }).start();
    }

    @Override
    public boolean getRouteAvailability(String route) throws ActivityHandlingException {
        return route.equals(ExampleDriver.ROUTE_NAME) && driver.isConnectorStarted();
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ActivityHandlingException {
        // Not supported for this driver
        throw new ActivityHandlingException("Operation not supported");
    }
}
