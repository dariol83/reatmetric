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
 * This interface specifies a method to retrieve the state of the specified set of data items (as filter), at the specified time.
 * This interface is only available for data items that support such semantic.
 *
 * @param <T> subscriber type
 * @param <R> filter type
 * @param <K> item type
 */
public interface IDataItemStateProvisionService<T extends IDataItemSubscriber<K>, R extends AbstractDataItemFilter<K>, K extends AbstractDataItem> extends IDataItemProvisionService<T, R, K> {

    /**
     * Retrieve the state at the specified time, of the data items matching the specified filter.
     *
     * @param time the time reference to use
     * @param filter the filter
     * @return the list of data items matching the filter, with their state at the specified time
     * @throws ReatmetricException if a problem arises with the retrieval operation
     */
    List<K> retrieve(Instant time, R filter) throws ReatmetricException;
    
}
