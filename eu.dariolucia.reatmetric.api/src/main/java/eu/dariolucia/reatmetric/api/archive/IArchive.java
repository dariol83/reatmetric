/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.archive;

import eu.dariolucia.reatmetric.api.archive.exceptions.ArchiveException;
import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.common.AbstractDataItemFilter;
import eu.dariolucia.reatmetric.api.events.IEventDataArchive;
import eu.dariolucia.reatmetric.api.messages.IOperationalMessageArchive;
import eu.dariolucia.reatmetric.api.parameters.IParameterDataArchive;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataArchive;

public interface IArchive {

    void connect() throws ArchiveException;

    void dispose() throws ArchiveException;

    IOperationalMessageArchive getOperationalMessageArchive();

    IEventDataArchive getEventDataArchive();

    IRawDataArchive getRawDataArchive();

    IParameterDataArchive getParameterDataArchive();

    <U extends IDataItemArchive<J,K>,J extends AbstractDataItem,K extends AbstractDataItemFilter> U getArchive(Class<U> clazz);
}
