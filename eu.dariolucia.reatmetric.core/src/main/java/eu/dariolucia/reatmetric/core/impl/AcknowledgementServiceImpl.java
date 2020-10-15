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

package eu.dariolucia.reatmetric.core.impl;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.messages.AcknowledgedMessage;
import eu.dariolucia.reatmetric.api.messages.IAcknowledgementService;

import java.rmi.RemoteException;
import java.util.Collection;

/**
 * This class exists to avoid remoting problems with object activation.
 */
public class AcknowledgementServiceImpl implements IAcknowledgementService {

    private final AcknowledgedMessageBrokerImpl broker;

    public AcknowledgementServiceImpl(AcknowledgedMessageBrokerImpl acknowledgedMessageBroker) {
        this.broker = acknowledgedMessageBroker;
    }

    @Override
    public void acknowledgeMessage(AcknowledgedMessage message, String user) throws ReatmetricException {
        broker.internalAcknowledgeMessage(message, user);
    }

    @Override
    public void acknowledgeMessages(Collection<AcknowledgedMessage> messages, String user) throws ReatmetricException {
        broker.internalAcknowledgeMessages(messages, user);
    }

    @Override
    public void acknowledgeAllMessages(String user) throws ReatmetricException, RemoteException {
        broker.internalAcknowledgeAllMessages(user);
    }
}
