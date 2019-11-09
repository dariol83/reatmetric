/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.messages;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.RetrievalDirection;

import java.time.Instant;
import java.util.List;

public interface IOperationalMessageArchive {

    List<OperationalMessage> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ArchiveException;

    List<OperationalMessage> retrieve(OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) throws ArchiveException;

    void store(OperationalMessage message) throws ArchiveException;

    void store(List<OperationalMessage> messages) throws ArchiveException;

    IUniqueId retrieveLastId() throws ArchiveException;
}
