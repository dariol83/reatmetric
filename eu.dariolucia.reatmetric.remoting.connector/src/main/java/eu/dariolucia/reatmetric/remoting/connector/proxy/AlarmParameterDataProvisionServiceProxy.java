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

import eu.dariolucia.reatmetric.api.alarms.AlarmParameterData;
import eu.dariolucia.reatmetric.api.alarms.AlarmParameterDataFilter;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataProvisionService;
import eu.dariolucia.reatmetric.api.alarms.IAlarmParameterDataSubscriber;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.events.*;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;

import java.rmi.RemoteException;

public class AlarmParameterDataProvisionServiceProxy extends AbstractProvisionServiceProxy<AlarmParameterData, AlarmParameterDataFilter, IAlarmParameterDataSubscriber, IAlarmParameterDataProvisionService> implements IAlarmParameterDataProvisionService {

    public AlarmParameterDataProvisionServiceProxy(IAlarmParameterDataProvisionService delegate, String localAddress) {
        super(delegate, localAddress);
    }

}
