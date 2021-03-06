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

package eu.dariolucia.reatmetric.core.api;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.rawdata.RawData;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * This interface provides human readable information about the contents of a {@link RawData} object. An object implementing
 * this interface must be able to report the handler and the list of supported raw data types that it can render.
 */
public interface IRawDataRenderer {

    /**
     * The handler that this object can render.
     *
     * @return the handler
     */
    String getHandler();

    /**
     * The list of supported raw data types that this object can render, for the supported source identifier.
     *
     * @return the list of supported raw data types
     */
    List<String> getSupportedTypes();

    /**
     * This method returns a map of raw data properties, for visualisation and further processing.
     *
     * @param rawData the raw data to render
     * @return the map of properties related to the provided raw data
     */
    LinkedHashMap<String, String> render(RawData rawData) throws ReatmetricException;
}
