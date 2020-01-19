/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.common;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author dario
 * @param <T> subscriber type
 * @param <R> filter type
 * @param <K> item type
 */
public interface IDataItemStateProvisionService<T extends IDataItemSubscriber<K>, R extends AbstractDataItemFilter, K extends UniqueItem> extends IDataItemProvisionService<T, R, K> {
    
    List<K> retrieve(Instant time, R filter) throws ReatmetricException;
    
}
