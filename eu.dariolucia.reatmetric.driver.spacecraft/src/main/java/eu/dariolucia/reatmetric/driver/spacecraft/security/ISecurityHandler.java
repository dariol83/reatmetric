/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.security;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

public interface ISecurityHandler {

    void initialise(IServiceCoreContext context, SpacecraftConfiguration configuration) throws ReatmetricException;

    int getSecurityHeaderLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type);

    int getSecurityTrailerLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type);

    AbstractTransferFrame encrypt(AbstractTransferFrame frame) throws ReatmetricException;

    AbstractTransferFrame decrypt(AbstractTransferFrame frame) throws ReatmetricException;

    void dispose();
}
