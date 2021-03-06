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

package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * This interface is used to request the acknowledgement of messages.
 */
public interface IAcknowledgementService extends Remote {

    /**
     * Acknowledge the provided message by the specified user.
     *
     * @param message the message to acknowledge
     * @param user the user that acknowledged the message
     */
    void acknowledgeMessage(AcknowledgedMessage message, String user) throws ReatmetricException, RemoteException;

    /**
     * Acknowledge the provided messages by the specified user.
     *
     * @param messages the messages to acknowledge
     * @param user the user that acknowledged the message
     */
    void acknowledgeMessages(Collection<AcknowledgedMessage> messages, String user) throws ReatmetricException, RemoteException;

    /**
     * Acknowledge all pending messages by the specified user.
     *
     * @param user the user that acknowledged the message
     */
    void acknowledgeAllMessages(String user) throws ReatmetricException, RemoteException;
}
