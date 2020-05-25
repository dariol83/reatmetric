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

import java.util.List;

/**
 * This interface is the parent interface of the data item type-specific subscription interfaces.
 */
public interface IDataItemSubscriber<T extends UniqueItem> {

    /**
     * Inform the subscriber the given list of data items is available according to the specified filter at subscription time.
     *
     * @param dataItems the new available data items
     */
    void dataItemsReceived(List<T> dataItems);
    
}
