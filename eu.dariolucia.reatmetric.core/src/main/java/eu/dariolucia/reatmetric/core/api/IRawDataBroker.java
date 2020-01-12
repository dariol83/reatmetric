/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.api;

import eu.dariolucia.reatmetric.api.common.IUniqueId;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;

import java.util.List;
import java.util.function.Predicate;

public interface IRawDataBroker {

    default void distribute(List<RawData> items) throws ReatmetricException {
        distribute(items, true);
    }

    void distribute(List<RawData> items, boolean store) throws ReatmetricException;

    void subscribe(IRawDataSubscriber subscriber, Predicate<RawData> preFilter, RawDataFilter filter, Predicate<RawData> postFilter);

    void unsubscribe(IRawDataSubscriber subscriber);

    IUniqueId nextRawDataId();
}
