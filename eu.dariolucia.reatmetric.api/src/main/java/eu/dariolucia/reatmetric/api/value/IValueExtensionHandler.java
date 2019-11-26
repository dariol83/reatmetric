/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
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
