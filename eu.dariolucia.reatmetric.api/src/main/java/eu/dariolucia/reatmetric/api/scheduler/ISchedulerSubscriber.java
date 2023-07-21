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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface subscriber for the scheduler state.
 */
public interface ISchedulerSubscriber extends Remote {

    /**
     * Report the enablement status of the scheduler when it changes.
     *
     * @param enabled true if the scheduler is enabled
     * @throws RemoteException in case of remote exception
     */
    void schedulerEnablementChanged(boolean enabled) throws RemoteException;

    /**
     * Report updates in the state of the bots. Upon subscription, this method is called with the current state
     * of all the bots.
     *
     * @param botStates the updated bot states
     * @throws RemoteException in case of remote exception
     */
    void botStateUpdated(List<BotStateData> botStates) throws RemoteException;
}
