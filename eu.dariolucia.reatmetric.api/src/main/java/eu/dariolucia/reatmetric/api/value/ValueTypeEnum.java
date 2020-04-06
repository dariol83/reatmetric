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
 * @author dario
 */
public enum ValueTypeEnum {
    /**
     * Boolean
     */
    BOOLEAN(1, Boolean.class, o -> Boolean.toString(o), Boolean::parseBoolean),
    /**
     * Enumerated value
     */
    ENUMERATED(2, Integer.class, String::valueOf, Integer::parseInt),
    /**
     * Unsigned integer
     */
    UNSIGNED_INTEGER(3, Long.class, String::valueOf, Long::parseLong),
    /**
     * Signed integer (two complement)
     */
    SIGNED_INTEGER(4, Long.class, String::valueOf, Long::parseLong),
    /**
     * Real number
     */
    REAL(5, Double.class, String::valueOf, Double::parseDouble),
    /**
     * Sequence of bits
     */
    BIT_STRING(6, BitString.class, BitString::toString, BitString::parse),
    /**
     * Sequence of bytes (8 bits)
     */
    OCTET_STRING(7, byte[].class, StringUtil::toHexDump, StringUtil::toByteArray),
    /**
     * Sequence of ASCII characters
     */
    CHARACTER_STRING(8, String.class, Function.identity(), Function.identity()),
    /**
     * Absolute time
     */
    ABSOLUTE_TIME(9, Instant.class, Instant::toString, Instant::parse),
    /**
     * Relative time
     */
    RELATIVE_TIME(10, Duration.class, Duration::toString, Duration::parse),
    /**
     * Extension
     */
    EXTENSION(11, Object.class, null, null);

    private int code;
    private Class<?> assignedClass;
    private Function<?, String> toString;
    private Function<String, ?> toObject;

    <T extends Object> ValueTypeEnum(int code, Class<T> assignedClass, Function<T, String> toString, Function<String, T> toObject) {
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

    public <T> T parse(String s) {
        if (this == EXTENSION) {
            throw new IllegalStateException("Extension values cannot be parsed by this class");
        }
        return (T) toObject.apply(s);
    }

    public String toString(Object object) {
        if (this == EXTENSION) {
            throw new IllegalStateException("Extension values cannot be formatted as string by this class");
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
        if (code <= 0 || code >= 12) {
            throw new IllegalArgumentException("Value type code " + code + " not supported");
        }
        return ValueTypeEnum.values()[code - 1];
    }
}
