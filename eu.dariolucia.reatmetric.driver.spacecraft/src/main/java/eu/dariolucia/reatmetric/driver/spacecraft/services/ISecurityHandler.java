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

package eu.dariolucia.reatmetric.driver.spacecraft.services;

import eu.dariolucia.ccsds.tmtc.datalink.pdu.AbstractTransferFrame;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.spacecraft.definition.SpacecraftConfiguration;

/**
 * This interface specifies the methods that an external class must implement, to provide encryption/decryption capabilities
 * to ReatMetric's spacecraft driver.
 */
public interface ISecurityHandler extends IService {

    /**
     * The length of the security header field, as defined by the CCSDS SDLP.
     *
     * @param spacecraftId the ID of the spacecraft for which the request is performed
     * @param virtualChannelId the virtual channel ID for which the request is performed
     * @param type the specific class for which the request is performed
     * @return the length of the security header field
     */
    int getSecurityHeaderLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type);

    /**
     * The length of the security trailer field, as defined by the CCSDS SDLP.
     *
     * @param spacecraftId the ID of the spacecraft for which the request is performed
     * @param virtualChannelId the virtual channel ID for which the request is performed
     * @param type the specific class for which the request is performed
     * @return the length of the security header field
     */
    int getSecurityTrailerLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type);

    /**
     * This method is called to request the encryption of the provided frame.
     *
     * @param frame the original, unencrypted frame
     * @return the encrypted frame (or the same original frame, if no encryption was deemed necessary)
     * @throws ReatmetricException in case of issues during the encryption process
     */
    AbstractTransferFrame encrypt(AbstractTransferFrame frame) throws ReatmetricException;

    /**
     * This method is called to request the decryption of the provided frame.
     *
     * @param frame the original, encrypted frame
     * @return the decrypted frame (or the same original frame, if no decryption was deemed necessary)
     * @throws ReatmetricException in case of issues during the decryption process
     */
    AbstractTransferFrame decrypt(AbstractTransferFrame frame) throws ReatmetricException;

    /**
     * This method is called at the disposal of the driver: all acquired resources must be released.
     */
    void dispose();
}
