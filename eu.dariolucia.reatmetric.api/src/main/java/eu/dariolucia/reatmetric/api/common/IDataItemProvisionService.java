/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.api.common;

import java.time.Instant;
import java.util.List;

/**
 *
 * @author dario
 * @param <T> subscriber type
 * @param <R> filter type
 * @param <K> item type
 */
public interface IDataItemProvisionService<T extends IDataItemSubscriber<K>, R extends AbstractDataItemFilter, K extends UniqueItem> {
    
    void subscribe(T subscriber, R filter);
    
    void unsubscribe(T subscriber);
    
    List<K> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, R filter);
    
    List<K> retrieve(K startItem, int numRecords, RetrievalDirection direction, R filter);
    
    List<FieldDescriptor> getAdditionalFieldDescriptors();
}
