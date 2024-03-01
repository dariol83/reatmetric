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

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Enumeration specifying the types supported as values in the ReatMetric systems and the related Java mapping.
 */
public enum ValueTypeEnum {
    /**
     * Boolean, mapped to Java Boolean primitive type
     */
    BOOLEAN(1, Boolean.class, o -> Boolean.toString(o), Boolean::parseBoolean),
    /**
     * Enumerated value, mapped to Java Integer primitive type
     */
    ENUMERATED(2, Integer.class, String::valueOf, Integer::parseInt),
    /**
     * Unsigned integer, mapped to Java Long primitive type
     */
    UNSIGNED_INTEGER(3, Long.class, String::valueOf, Long::parseLong),
    /**
     * Signed integer (two complement), mapped to Java Long primitive type
     */
    SIGNED_INTEGER(4, Long.class, String::valueOf, Long::parseLong),
    /**
     * Real number, mapped to Java Double primitive type
     */
    REAL(5, Double.class, String::valueOf, Double::parseDouble),
    /**
     * Sequence of bits, mapped to ReatMetric {@link BitString} class
     */
    BIT_STRING(6, BitString.class, BitString::toString, BitString::parse),
    /**
     * Sequence of bytes (8 bits), mapped to Java byte[] class
     */
    OCTET_STRING(7, byte[].class, StringUtil::toHexDump, StringUtil::toByteArray),
    /**
     * Sequence of ASCII characters, mapped to Java {@link String} class
     */
    CHARACTER_STRING(8, String.class, Function.identity(), Function.identity()),
    /**
     * Absolute time, mapped to Java {@link Instant} class
     */
    ABSOLUTE_TIME(9, Instant.class, Instant::toString, Instant::parse),
    /**
     * Relative time, mapped to Java {@link Duration} class
     */
    RELATIVE_TIME(10, Duration.class, Duration::toString, Duration::parse),
    /**
     * Array, mapped to ReatMetric {@link Array} class
     */
    ARRAY(11, Array.class, Array::toString, Array::parse),
    /**
     * Extension, mapped to Java Object class
     */
    EXTENSION(12, Object.class, null, null),
    /**
     * Derived, only used for activity arguments and in those situation when the value type should be derived from the
     * actual object
     */
    DERIVED(13, Object.class, null, null);

    private final int code;
    private final Class<?> assignedClass;
    private final Function<?, String> toString;
    private final Function<String, ?> toObject;

    <T> ValueTypeEnum(int code, Class<T> assignedClass, Function<T, String> toString, Function<String, T> toObject) {
        this.code = code;
        this.assignedClass = assignedClass;
        this.toString = toString;
        this.toObject = toObject;
    }

    /**
     * This method returns the code linked to the enumeration literal.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * This method returns the class that can used for the type identified by the enumeration literal.
     *
     * @return the class
     */
    public Class<?> getAssignedClass() {
        return assignedClass;
    }

    @SuppressWarnings("unchecked")
    public <T> T parse(String s) {
        if (this == EXTENSION || this == DERIVED) {
            throw new IllegalStateException("Extension and derived values cannot be parsed by this class");
        }
        return (T) toObject.apply(s);
    }

    @SuppressWarnings("unchecked")
    public String toString(Object object) {
        if (this == EXTENSION || this == DERIVED) {
            throw new IllegalStateException("Extension and derived values cannot be formatted as string by this class");
        }
        return ((Function<Object, String>) toString).apply(object);
    }

    public static ValueTypeEnum fromClass(Class<?> assignedClass) {
        for(ValueTypeEnum vte : values()) {
            if(vte.getAssignedClass().isAssignableFrom(assignedClass)) {
                return vte;
            }
        }
        return EXTENSION;
    }

    /**
     * This function maps the provided code to the corresponding enumeration literal.
     *
     * @param code the code to map
     * @return the corresponding literal
     * @throws IllegalArgumentException if no literal corresponds to the provided code
     */
    public static ValueTypeEnum fromCode(int code) {
        if (code <= 0 || code >= values().length + 1) {
            throw new IllegalArgumentException("Value type code " + code + " not supported");
        }
        return ValueTypeEnum.values()[code - 1];
    }

    public String asBeautyString() {
        return name().replace('_', ' ');
    }
}
