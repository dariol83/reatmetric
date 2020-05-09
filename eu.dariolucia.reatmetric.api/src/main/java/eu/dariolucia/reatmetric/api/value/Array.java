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

package eu.dariolucia.reatmetric.api.value;

import eu.dariolucia.reatmetric.api.common.Pair;

import java.io.Serializable;
import java.util.List;

public class Array implements Serializable {

    private final List<Record> records;

    public Array(List<Record> records) {
        this.records = List.copyOf(records);
    }

    public List<Record> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for(Record r : records) {
            sb.append(r.toString());
        }
        sb.append(" ]");
        return sb.toString();
    }

    public static Array parse(String s) {
        throw new UnsupportedOperationException("Cannot parse Array yet: " + s);
    }

    public static class Record implements Serializable {

        private final List<Pair<String, Object>> elements;

        public Record(List<Pair<String, Object>> elements) {
            this.elements = List.copyOf(elements);
        }

        public List<Pair<String, Object>> getElements() {
            return elements;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ ");
            for(Pair<String, Object> e : elements) {
                sb.append(e.getFirst()).append(" : ");
                sb.append(ValueUtil.toString(e.getSecond()));
                sb.append(" | ");
            }
            if(elements.size() > 0) {
                sb.delete(sb.length() - 3, sb.length());
            }
            sb.append(" }");
            return sb.toString();
        }
    }
}
