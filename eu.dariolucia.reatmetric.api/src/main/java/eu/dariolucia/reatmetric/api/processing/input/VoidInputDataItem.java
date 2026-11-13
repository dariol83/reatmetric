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

package eu.dariolucia.reatmetric.api.processing.input;

/**
 * This class is used for system entity processors that do not need any input data item to work, as they use only
 * standard operations.
 */
public final class VoidInputDataItem extends AbstractInputDataItem {

    private static final VoidInputDataItem INSTANCE = new VoidInputDataItem();

    /**
     * Returns the singleton instance of VoidInputDataItem.
     *
     * @return the singleton instance
     */
    public static VoidInputDataItem instance() {
        return INSTANCE;
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private VoidInputDataItem() {
        // private constructor, nothing to do here
    }
}
