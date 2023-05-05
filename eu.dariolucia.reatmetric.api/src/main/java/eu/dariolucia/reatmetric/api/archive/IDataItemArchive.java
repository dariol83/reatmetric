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
import eu.dariolucia.reatmetric.api.common.*;

import java.time.Instant;
import java.util.List;

/**
 * This interface is a generic archive service interface, providing operations to store and retrieve data items.
 *
 * @param <T> the {@link AbstractDataItem} that this service stores/retrieves
 * @param <K> the {@link AbstractDataItemFilter} that can be used with this service
 */
public interface IDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter<T>> extends IDebugInfoProvider {

    /**
     * Retrieve a maximum of numRecords data items, with a generation time greater/lower or equals than the provided startTime (if
     * direction is respectively set to TO_FUTURE/TO_PAST), and matching the provided filter.
     * The returned list is ordered according to generation time (ascending order if TO_FUTURE is used, descending order
     * if TO_PAST is used).
     *
     * @param startTime the time used as reference for the retrieval
     * @param numRecords the maximum number of records to be retrieved
     * @param direction the retrieval direction, which drives also the ordering of the returned data
     * @param filter the filter, it can be null
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException;

    /**
     * Retrieve a maximum of numRecords data items, starting from the provided startItem, in the future or in the past (if
     * direction is respectively set to TO_FUTURE/TO_PAST), and matching the provided filter.
     * The returned list is ordered according to generation time (ascending order if TO_FUTURE is used, descending order
     * if TO_PAST is used). The returned list shall not contain the startItem element.
     *
     * @param startItem the item used as reference for the retrieval
     * @param numRecords the maximum number of records to be retrieved
     * @param direction the retrieval direction, which drives also the ordering of the returned data
     * @param filter the filter, it can be null
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException;

    /**
     * Retrieve data items, between the provided startItem and the provided endTime (included), and matching the provided filter.
     * The returned list is ordered according to generation time (ascending order if startItem < endTime, descending order
     * if startItem > endTime).
     *
     * @param startTime the start time used as reference for the retrieval
     * @param endTime the end time used as reference for the retrieval
     * @param filter the filter, it can be null
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<T> retrieve(Instant startTime, Instant endTime, K filter) throws ArchiveException;

    /**
     * Retrieve the status of the data item matching the filter at the specified time. Not all data item archive services
     * support this operation. If the operation is not supported, the archive service is entitled to throw an {@link UnsupportedOperationException}.
     *
     * @param time the reference time for which the status is needed
     * @param filter the filter, it can be null
     * @param maxLookbackTime the maximum look-back time (absolute), can be null. In such case, internal archive default is used
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<T> retrieve(Instant time, K filter, Instant maxLookbackTime) throws ArchiveException;

    /**
     * Retrieve the data item having the provided unique ID.
     *
     * @param uniqueId the unique ID of the data item to retrieve
     * @return the retrieved data item or null if not present
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    T retrieve(IUniqueId uniqueId) throws ArchiveException;

    /**
     * Store the provided item.
     *
     * @param item the item to store
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the store operation to be completed successfully
     */
    void store(T item) throws ArchiveException;

    /**
     * Store the provided items.
     *
     * @param items the item to store
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the store operation to be completed successfully
     */
    void store(List<T> items) throws ArchiveException;

    /**
     * Retrieve the last stored unique ID for the data item handled by this archive service.
     *
     * @return the last unique ID or 0 if there is none
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    IUniqueId retrieveLastId() throws ArchiveException;

    /**
     * Retrieve the last stored unique ID for the specified data item type handled by this archive service.
     *
     * @param type the class of the data item type
     * @return the last unique ID or 0 if there is none
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    IUniqueId retrieveLastId(Class<? extends AbstractDataItem> type) throws ArchiveException;

    /**
     * Retrieve the last (largest) stored generation time for the data item handled by this archive service.
     *
     * @return the last (largest) stored generation time or null if there is none
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    Instant retrieveLastGenerationTime() throws ArchiveException;

    /**
     * Retrieve the last (largest) stored generation time for the specified data item type handled by this archive service.
     *
     * @param type the class of the data item type
     * @return the last (largest) stored generation time or null if there is none
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    Instant retrieveLastGenerationTime(Class<? extends AbstractDataItem> type) throws ArchiveException;

    /**
     * Remove the item having the specified internal ID.
     *
     * @param id the ID to remove
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    void remove(IUniqueId id) throws ArchiveException;

    /**
     * Remove the items matching the specified filter.
     *
     * @param filter the filter to use
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    void remove(K filter) throws ArchiveException;

    /**
     * Delete all entries in the archive strictly following (generationTime > referenceTime) or preceeding (generationTime < referenceTime)
     * the provided time.
     *
     * @param referenceTime the reference time
     * @param direction the delete direction: TO_FUTURE removes all data items with generationTime > referenceTime
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the purge operation to be completed successfully
     */
    void purge(Instant referenceTime, RetrievalDirection direction) throws ArchiveException;

    /**
     * Close and dispose this archive service. Further calls to store/retrieve operations will throw an exception.
     *
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the operation to be completed successfully
     */
    void dispose() throws ArchiveException;
}
