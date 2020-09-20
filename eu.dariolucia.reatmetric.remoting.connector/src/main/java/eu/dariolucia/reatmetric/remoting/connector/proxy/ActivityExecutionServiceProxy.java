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

package eu.dariolucia.reatmetric.remoting.connector.proxy;

import eu.dariolucia.reatmetric.api.activity.ActivityRouteState;
import eu.dariolucia.reatmetric.api.activity.IActivityExecutionService;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.processing.input.ActivityProgress;
import eu.dariolucia.reatmetric.api.processing.input.ActivityRequest;
import eu.dariolucia.reatmetric.api.processing.input.SetParameterRequest;

import java.rmi.RemoteException;
import java.util.List;

public class ActivityExecutionServiceProxy implements IActivityExecutionService {

    protected final IActivityExecutionService delegate;

    public ActivityExecutionServiceProxy(IActivityExecutionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public IUniqueId startActivity(ActivityRequest request) throws ReatmetricException, RemoteException {
        return delegate.startActivity(request);
    }

    @Override
    public IUniqueId createActivity(ActivityRequest request, ActivityProgress currentProgress) throws ReatmetricException, RemoteException {
        return delegate.createActivity(request, currentProgress);
    }

    @Override
    public void purgeActivities(List<Pair<Integer, IUniqueId>> activityOccurrenceIds) throws ReatmetricException, RemoteException {
        delegate.purgeActivities(activityOccurrenceIds);
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability() throws ReatmetricException, RemoteException {
        return delegate.getRouteAvailability();
    }

    @Override
    public List<ActivityRouteState> getRouteAvailability(String type) throws ReatmetricException, RemoteException {
        return delegate.getRouteAvailability(type);
    }

    @Override
    public IUniqueId setParameterValue(SetParameterRequest request) throws ReatmetricException, RemoteException {
        return delegate.setParameterValue(request);
    }

    @Override
    public void abortActivity(int activityId, IUniqueId activityOccurrenceId) throws ReatmetricException, RemoteException {
        delegate.abortActivity(activityId, activityOccurrenceId);
    }
}
