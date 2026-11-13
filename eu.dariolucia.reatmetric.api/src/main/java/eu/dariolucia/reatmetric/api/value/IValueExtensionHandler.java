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

/**
 * Handler interface for extending value type support.
 */
public interface IValueExtensionHandler {

    /**
     * Returns the class that matches with the object type.
     *
     * @return the type class
     */
    Class<?> typeClass();

    /**
     * Returns the ID that identifies the type.
     * Negative values are reserved and shall not be used.
     *
     * @return the type ID (positive value only)
     */
    short typeId();

    /**
     * Converts the value to its string representation.
     *
     * @param v the value to convert
     * @return the string representation of the value
     */
    String toString(Object v);

    /**
     * Parses a string to create a value.
     *
     * @param s the string to parse
     * @return the parsed value
     */
    Object parse(String s);

    /**
     * Serializes a value to a byte array.
     *
     * @param v the value to serialize
     * @return the serialized byte array
     */
    byte[] serialize(Object v);

    /**
     * Deserializes a value from a byte array.
     *
     * @param b the byte array containing the serialized data
     * @param offset the offset into the byte array where the data starts
     * @param length the length of the data to deserialize
     * @return the deserialized value
     */
    Object deserialize(byte[] b, int offset, int length);
}
