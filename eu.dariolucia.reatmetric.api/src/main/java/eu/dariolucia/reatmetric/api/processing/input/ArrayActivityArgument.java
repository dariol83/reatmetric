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

import java.util.Arrays;
import java.util.List;

public final class ArrayActivityArgument extends AbstractActivityArgument {

    public static ArrayActivityArgument of(String name, List<ArrayActivityArgumentRecord> elements) {
        return new ArrayActivityArgument(name, elements);
    }

    public static ArrayActivityArgument of(String name, ArrayActivityArgumentRecord... elements) {
        return new ArrayActivityArgument(name, Arrays.asList(elements));
    }

    private final List<ArrayActivityArgumentRecord> records;

    public ArrayActivityArgument(String name, List<ArrayActivityArgumentRecord> records) {
        super(name);
        this.records = List.copyOf(records);
    }

    public List<ArrayActivityArgumentRecord> getRecords() {
        return records;
    }
}
