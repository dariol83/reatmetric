/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;

import java.rmi.RemoteException;
import java.util.List;

/**
 * This class can be used to retrieve cached system entities from whatever view has them.
 *
 * Calls to the {@link ISystemEntityResolver} interface must be executed from the UI thread.
 */
public class SystemEntityResolver {

    private static ISystemEntityResolver RESOLVER;

    public static void setResolver(ISystemEntityResolver r) {
        synchronized (SystemEntityResolver.class) {
            RESOLVER = r;
        }
    }

    public static ISystemEntityResolver getResolver() {
        synchronized (SystemEntityResolver.class) {
            if(RESOLVER == null) {
                throw new IllegalStateException("Resolver not set");
            }
            return RESOLVER;
        }
    }

    public interface ISystemEntityResolver {

        SystemEntity getSystemEntity(String path);

        SystemEntity getSystemEntity(int id);

        List<SystemEntity> getFromFilter(String partialPath, SystemEntityType type);

        AbstractSystemEntityDescriptor getDescriptorOf(int externalId) throws ReatmetricException, RemoteException;

        AbstractSystemEntityDescriptor getDescriptorOf(String path) throws ReatmetricException, RemoteException;

        AbstractSystemEntityDescriptor getDescriptorOf(SystemEntityPath path) throws ReatmetricException, RemoteException;
    }
}
