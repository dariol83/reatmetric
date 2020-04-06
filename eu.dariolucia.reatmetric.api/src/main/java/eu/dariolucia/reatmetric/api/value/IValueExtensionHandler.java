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

public interface IValueExtensionHandler {

    /**
     * The class that matches with the object type.
     *
     * @return the type class
     */
    Class<?> typeClass();

    /**
     * The ID that identifies the type: negative values are reserved and shall not be used.
     *
     * @return the type ID (positive value only)
     */
    short typeId();

    String toString(Object v);

    Object parse(String s);

    byte[] serialize(Object v);

    Object deserialize(byte[] b, int offset, int length);
}
