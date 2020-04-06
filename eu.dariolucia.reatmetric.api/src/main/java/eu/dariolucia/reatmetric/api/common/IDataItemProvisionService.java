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
public interface IDataItemProvisionService<T extends IDataItemSubscriber<K>, R extends AbstractDataItemFilter, K extends UniqueItem> {
    
    void subscribe(T subscriber, R filter);
    
    void unsubscribe(T subscriber);
    
    List<K> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, R filter) throws ReatmetricException;
    
    List<K> retrieve(K startItem, int numRecords, RetrievalDirection direction, R filter) throws ReatmetricException;

}
