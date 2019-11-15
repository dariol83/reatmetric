package eu.dariolucia.reatmetric.api.archive;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;

import java.time.Instant;
import java.util.List;

/**
 * This interface is a generic archive service interface, providing operations to store and retrieve data items.
 *
 * @param <T> the {@link AbstractDataItem} that this service stores/retrieves
 * @param <K> the {@link AbstractDataItemFilter} that can be used with this service
 */
public interface IDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter> {

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
     * Retrieve the status of the data item matching the filter at the specified time. Not all data item archive services
     * support this operation. If the operation is not supported, the archive service is entitled to throw an {@link UnsupportedOperationException}.
     *
     * @param time the reference time for which the status is needed
     * @param filter the filter, it can be null
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<T> retrieve(Instant time, K filter) throws ArchiveException;

    /**
     * Retrieve the data item having the provided unique ID.
     *
     * @param uniqueId the unique ID of the data item to retrieve
     * @return the retrieved data item or null if not present
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    T retrieve(IUniqueId uniqueId) throws ArchiveException;

    void store(T message) throws ArchiveException;

    void store(List<T> messages) throws ArchiveException;

    /**
     * Retrieve the last stored unique ID for the data item handled by this archive service.
     *
     * @return the last unique ID or 0 if there is none
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    IUniqueId retrieveLastId() throws ArchiveException;

    /**
     * Close and dispose this archive service. Further calls to store/retrieve operations will throw an exception.
     *
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the operation to be completed successfully
     */
    void dispose() throws ArchiveException;
}
