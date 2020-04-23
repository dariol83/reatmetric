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

/**
 * This interface is the service interface representing the persistence service in a ReatMetric system. It contains
 * a single factory method that can be used to instantiate an implementation of the {@link IArchive} interface.
 */
public interface IArchiveFactory {

    /**
     * Return an implementation of the {@link IArchive} interface supplied by the provider of the service. The
     * archiveLocation parameter type and format depends on the archive specific implementation, i.e. can be a
     * JDBC string, a file path containing a configuration, an IP address. From this string, the specific implementation
     * shall be able to construct and return an object.
     *
     * The returned {@link IArchive} object is not required to be a different new object: {@link IArchiveFactory}
     * implementations are allowed to cache objects or use a singleton-based design.
     *
     * @param archiveLocation the 'location' of the archive
     * @return an implementation of {@link IArchive} interface
     * @throws ArchiveException in case of problems arising from the construction of the specific {@link IArchive} object
     */
    IArchive buildArchive(String archiveLocation) throws ArchiveException;

}
