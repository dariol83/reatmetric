package eu.dariolucia.reatmetric.api.archive;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;

import java.time.Instant;
import java.util.List;

public interface IDataItemArchive<T extends AbstractDataItem, K extends AbstractDataItemFilter> {

    List<T> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException;

    List<T> retrieve(T startItem, int numRecords, RetrievalDirection direction, K filter) throws ArchiveException;

    T retrieve(IUniqueId uniqueId) throws ArchiveException;

    void store(T message) throws ArchiveException;

    void store(List<T> messages) throws ArchiveException;

    IUniqueId retrieveLastId() throws ArchiveException;

    void dispose() throws ArchiveException;
}
