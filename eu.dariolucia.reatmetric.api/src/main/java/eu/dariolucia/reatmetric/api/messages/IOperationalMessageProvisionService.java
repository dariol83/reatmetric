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

import eu.dariolucia.reatmetric.api.common.IDataItemProvisionService;

/**
 * This interface is a specialisation of the {@link IDataItemProvisionService}, for operational messages.
 */
public interface IOperationalMessageProvisionService extends IDataItemProvisionService<IOperationalMessageSubscriber, OperationalMessageFilter, OperationalMessage> {

    // TODO add possibility to raise a message with a message request object: IOperationalMessageSender and input OperationalMessageRequest
}
