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
import eu.dariolucia.reatmetric.driver.spacecraft.services.ISecurityHandler;
import eu.dariolucia.reatmetric.driver.spacecraft.services.IServiceBroker;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataLinkSecurityManager {

    private static final Logger LOG = Logger.getLogger(DataLinkSecurityManager.class.getName());
    private final ISecurityHandler securityHandler;

    public DataLinkSecurityManager(IServiceCoreContext context, SpacecraftConfiguration configuration, IServiceBroker serviceBroker) {
        ISecurityHandler handler = serviceBroker.locate(ISecurityHandler.class);
        if(handler != null) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, String.format("Security handler %s for spacecraft %s initialised", handler.getName(), configuration.getName()));
            }
            securityHandler = handler;
        } else {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Security handler not found");
            }
            securityHandler = null;
        }
    }

    public int getSecurityHeaderLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type) {
        return securityHandler == null ? 0 : securityHandler.getSecurityHeaderLength(spacecraftId, virtualChannelId, type);
    }

    public int getSecurityTrailerLength(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type) {
        return securityHandler == null ? 0 : securityHandler.getSecurityTrailerLength(spacecraftId, virtualChannelId, type);
    }

    public AbstractTransferFrame encrypt(AbstractTransferFrame frame) throws ReatmetricException {
        return securityHandler == null ? frame : withAnnotations(frame, securityHandler.encrypt(frame));
    }

    public AbstractTransferFrame decrypt(AbstractTransferFrame frame) throws ReatmetricException{
        return securityHandler == null ? frame : withAnnotations(frame, securityHandler.decrypt(frame));
    }

    private AbstractTransferFrame withAnnotations(AbstractTransferFrame originalFrame, AbstractTransferFrame newFrame) {
        if (originalFrame != newFrame) {
            for (Object annotationKey : originalFrame.getAnnotationKeys()) {
                if (!newFrame.isAnnotationPresent(annotationKey)) {
                    newFrame.setAnnotationValue(annotationKey, originalFrame.getAnnotationValue(annotationKey));
                }
            }
        }
        return newFrame;
    }

    public Supplier<byte[]> getSecurityHeaderSupplier(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type) {
        if(securityHandler == null) {
            return null;
        } else {
            int len = getSecurityHeaderLength(spacecraftId, virtualChannelId, type);
            return len > 0 ? () -> new byte[len] : null;
        }
    }

    public Supplier<byte[]> getSecurityTrailerSupplier(int spacecraftId, int virtualChannelId, Class<? extends AbstractTransferFrame> type) {
        if(securityHandler == null) {
            return null;
        } else {
            int len = getSecurityTrailerLength(spacecraftId, virtualChannelId, type);
            return len > 0 ? () -> new byte[len] : null;
        }
    }

    public void dispose() {
        if(securityHandler != null) {
            securityHandler.dispose();
        }
    }
}
