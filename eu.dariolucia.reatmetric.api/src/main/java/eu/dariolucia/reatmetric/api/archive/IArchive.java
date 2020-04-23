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

package eu.dariolucia.reatmetric.api.archive;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;

/**
 * This interface specifies the entry point contract for archive service providers.
 */
public interface IArchive {

    /**
     * Connect to the archive backend system. The archive can be used only after return from this invocation.
     *
     * @throws ArchiveException in case of problems connecting to the archive backend system
     */
    void connect() throws ArchiveException;

    /**
     * Close the connection to the archive backend system. After this call, the object should be no longer be used.
     *
     * @throws ArchiveException in case of problems disconnecting to the archive backend system
     */
    void dispose() throws ArchiveException;

    /**
     * Retrieve the {@link IDataItemArchive} implementation specified by parameter clazz. This method is allowed
     * to return null, if no implementation of the requested interface is available, so callers should check the return
     * value.
     *
     * @param clazz the type class of the {@link IDataItemArchive}
     * @param <U> the type class of clazz
     * @param <J> the {@link AbstractDataItem} type managed by the specified archive interface clazz
     * @param <K> the {@link AbstractDataItemFilter} type managed by the specified archive interface clazz
     * @return the implementation of the requested {@link IDataItemArchive} type or null if such implementation is not available
     */
    <U extends IDataItemArchive<J,K>,J extends AbstractDataItem,K extends AbstractDataItemFilter<J>> U getArchive(Class<U> clazz);
}
