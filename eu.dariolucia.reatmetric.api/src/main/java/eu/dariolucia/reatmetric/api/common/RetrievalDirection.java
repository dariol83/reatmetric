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

/**
 * This enumeration specifies the two possible retrieval directions in a retrieval request: from the time/data item reference to the future,
 * or to the past.
 */
public enum RetrievalDirection {
    /**
     * Retrieve data item from the reference to the future: data items are returned in ascending generation time order
     */
    TO_FUTURE,
    /**
     * Retrieve data item from the reference to the past: data items are returned in descending generation time order
     */
    TO_PAST
}
