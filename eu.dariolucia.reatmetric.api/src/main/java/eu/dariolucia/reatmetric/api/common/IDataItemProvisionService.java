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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.List;

/**
 * This interface is the parent interface of all {@link AbstractDataItem} related provision interfaces, i.e. interfaces that
 * can be used to subscribe for or retrieve {@link AbstractDataItem} objects.
 *
 * A provision interface can deal with a single {@link AbstractDataItem} type and can return only objects of the declared K instance.
 *
 * By contract all methods of this interface shall be thread-safe, i.e. objects implementing this interface (or derivations)
 * must allow concurrent (at most serialized) access by external callers.
 *
 * @param <T> subscriber type
 * @param <R> filter type
 * @param <K> item type
 */
public interface IDataItemProvisionService<T extends IDataItemSubscriber<K>, R extends AbstractDataItemFilter<K>, K extends AbstractDataItem> extends Remote {

    /**
     * Subscribe to the provision service, to receive live updates matching the provider filter. If the filter is null,
     * all updates will be provided to the subscriber.
     *
     * @param subscriber the callback interface, cannot be null
     * @param filter the filter object, can be null
     */
    void subscribe(T subscriber, R filter) throws RemoteException;

    /**
     * Unsubscribe the callback interface from the provision service.
     *
     * @param subscriber the callback interface to unsubscribe, cannot be null
     */
    void unsubscribe(T subscriber) throws RemoteException;

    /**
     * Retrieve [numRecords] {@link AbstractDataItem} objects, starting from generation time [startTime] in the direction
     * indicated by [direction]. The returned objects shall match the provided filter and shall be consecutive in the time
     * direction, without gaps nor duplicates.
     *
     * The number of returned items can actually be less than [numRecords].
     *
     * @param startTime the generation reference time, cannot be null
     * @param numRecords the number of records to retrieve, cannot be negative
     * @param direction the time direction
     * @param filter the filter, can be null
     * @return the retrieved objects
     * @throws ReatmetricException if a problem arises with the retrieval operation
     */
    List<K> retrieve(Instant startTime, int numRecords, RetrievalDirection direction, R filter) throws ReatmetricException, RemoteException;

    /**
     * Retrieve [numRecords] {@link AbstractDataItem} objects, starting from the specified object [startItem] in the direction
     * indicated by [direction]. The returned objects shall match the provided filter and shall be consecutive in the time
     * direction, without gaps nor duplicates. The [startItem] object shall not be returned.
     *
     * The number of returned items can actually be less than [numRecords].
     *
     * @param startItem the {@link AbstractDataItem} object taken as reference
     * @param numRecords the number of records to retrieve, cannot be negative
     * @param direction the time direction
     * @param filter the filter, can be null
     * @return the retrieved objects
     * @throws ReatmetricException if a problem arises with the retrieval operation
     */
    List<K> retrieve(K startItem, int numRecords, RetrievalDirection direction, R filter) throws ReatmetricException, RemoteException;

}
