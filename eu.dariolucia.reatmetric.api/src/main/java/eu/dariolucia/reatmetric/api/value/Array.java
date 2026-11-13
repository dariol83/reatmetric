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

/**
 * Represents an array value containing multiple records.
 */
public class Array implements Serializable {

    /** The list of records in this array. */
    private final List<Record> records;

    /**
     * Constructs an Array with the specified records.
     *
     * @param records the list of records
     */
    public Array(List<Record> records) {
        this.records = List.copyOf(records);
    }

    /**
     * Returns the list of records.
     *
     * @return an unmodifiable list of records
     */
    public List<Record> getRecords() {
        return records;
    }

    @Override
    /**
     * Returns a string representation of this array.
     *
     * @return a string representation
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for(Record r : records) {
            sb.append(r.toString());
        }
        sb.append(" ]");
        return sb.toString();
    }

    /**
     * Parses a string to create an Array.
     * Currently not implemented.
     *
     * @param s the string to parse
     * @return the parsed Array
     * @throws UnsupportedOperationException always, as parsing is not yet implemented
     */
    public static Array parse(String s) {
        throw new UnsupportedOperationException("Cannot parse Array yet: " + s);
    }

    /**
     * Represents a single record within an Array, containing key-value pairs.
     */
    public static class Record implements Serializable {

        /** The list of key-value pairs in this record. */
        private final List<Pair<String, Object>> elements;

        /**
         * Constructs a Record with the specified elements.
         *
         * @param elements the list of key-value pairs
         */
        public Record(List<Pair<String, Object>> elements) {
            this.elements = List.copyOf(elements);
        }

        /**
         * Returns the list of key-value pairs.
         *
         * @return an unmodifiable list of key-value pairs
         */
        public List<Pair<String, Object>> getElements() {
            return elements;
        }

        @Override
        /**
         * Returns a string representation of this record.
         *
         * @return a string representation in the format "{ key1 : value1 | key2 : value2 }"
         */
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
