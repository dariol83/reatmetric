/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.archive.IDataItemArchive;
import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.events.EventData;
import eu.dariolucia.reatmetric.api.events.EventDataFilter;

import java.time.Instant;
import java.util.List;

public interface IActivityOccurrenceDataArchive extends IDataItemArchive<ActivityOccurrenceData, ActivityOccurrenceDataFilter> {

    /**
     * Retrieve the status of the data item matching the filter at the specified time. Not all data item archive services
     * support this operation. If the operation is not supported, the archive service is entitled to throw an {@link UnsupportedOperationException}.
     *
     * @param time the reference time for which the status is needed
     * @param filter the filter, it can be null
     * @param lookBackTime the look-back reference time for the generation time, i.e. how far back in time the archive shall check
     * @return the list of retrieved items
     * @throws ArchiveException in case of I/O problems, SQL problems or any other problem preventing the retrieval operation to be completed successfully
     */
    List<ActivityOccurrenceData> retrieve(Instant time, ActivityOccurrenceDataFilter filter, Instant lookBackTime) throws ArchiveException;
}
