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

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

public final class Value implements Serializable {

    public static Value of(int value) {
        return of(Integer.valueOf(value));
    }

    public static Value of(Integer value) {
        return new Value(ValueTypeEnum.ENUMERATED, Integer.class, value);
    }

    public static Value of(long value, boolean unsigned) {
        return of(Long.valueOf(value), unsigned);
    }

    public static Value of(Long value, boolean unsigned) {
        return new Value(unsigned ? ValueTypeEnum.UNSIGNED_INTEGER : ValueTypeEnum.SIGNED_INTEGER, Long.class, value);
    }

    public static Value of(double value) {
        return of(Double.valueOf(value));
    }

    public static Value of(Double value) {
        return new Value(ValueTypeEnum.REAL, Double.class, value);
    }

    public static Value of(Instant value) {
        return new Value(ValueTypeEnum.ABSOLUTE_TIME, Instant.class, value);
    }

    public static Value of(Duration value) {
        return new Value(ValueTypeEnum.RELATIVE_TIME, Duration.class, value);
    }

    public static Value of(boolean value) {
        return of(Boolean.valueOf(value));
    }

    public static Value of(Boolean value) {
        return new Value(ValueTypeEnum.BOOLEAN, Boolean.class, value);
    }

    public static Value of(String value) {
        return new Value(ValueTypeEnum.CHARACTER_STRING, String.class, value);
    }

    public static Value of(byte[] value) {
        return new Value(ValueTypeEnum.OCTET_STRING, byte[].class, value);
    }

    public static Value of(BitString value) {
        return new Value(ValueTypeEnum.BIT_STRING, BitString.class, value);
    }

    public static Value nill(ValueTypeEnum type) {
        return new Value(type, null, null);
    }

    public static Value ofExtension(Object value) {
        if(value == null) {
            throw new NullPointerException("value cannot be null, use Value.ofNull(type) instead of this method");
        }
        return new Value(ValueTypeEnum.EXTENSION, value.getClass(), value);
    }

    public static Value of(Object value) {
        if(value == null) {
            throw new NullPointerException("value cannot be null, use Value.ofNull(type) instead of this method");
        }
        // Check any matching
        if(value instanceof Integer) {
            return new Value(ValueTypeEnum.ENUMERATED, Integer.class, value);
        }
        if(value instanceof Double || value instanceof Float) {
            return of(((Number) value).doubleValue());
        }
        if(value instanceof Number) {
            return of(((Number) value).longValue(), false);
        }
        if(value instanceof Boolean) {
            return of((Boolean) value);
        }
        if(value instanceof Instant) {
            return of((Instant) value);
        }
        if(value instanceof Duration) {
            return of((Duration) value);
        }
        if(value instanceof String) {
            return of((String) value);
        }
        if(value instanceof byte[]) {
            return of((byte[]) value);
        }
        if(value instanceof BitString) {
            return of((BitString) value);
        }
        // If you arrive here, it is a value type that this class cannot handle natively
        return ofExtension(value);
    }

    public static Value parse(ValueTypeEnum type, String valueAsString) {
        if(type == ValueTypeEnum.EXTENSION) {
            throw new IllegalArgumentException("Cannot parse an extension using this method, use parseExtension(Class<?>,String)");
        } else {
            Object o = ValueUtil.parse(type, valueAsString);
            if(o == null) {
                return nill(type);
            } else {
                return new Value(type, o.getClass(), o);
            }
        }
    }

    public static <T> Value parseExtension(Class<T> type, String valueAsString) {
        Object o = ValueUtil.parseExtension(type, valueAsString);
        if(o == null) {
            return nill(ValueTypeEnum.EXTENSION);
        } else {
            return new Value(ValueTypeEnum.EXTENSION, type, o);
        }
    }

    public static Value deserialize(byte[] dump) {
        // Type
        ValueTypeEnum type = ValueTypeEnum.fromCode(dump[0] & 0x7F);
        Object o = ValueUtil.deserialize(dump);
        // Decode
        switch(type) {
            case BOOLEAN: {
                return of((Boolean) o);
            }
            case UNSIGNED_INTEGER: {
                return of((Long) o, true);
            }
            case SIGNED_INTEGER: {
                return of((Long) o, false);
            }
            case REAL: {
                return of((Double) o);
            }
            case ENUMERATED: {
                return of((Integer) o);
            }
            case CHARACTER_STRING: {
                return of((String) o);
            }
            case OCTET_STRING: {
                return of((byte[]) o);
            }
            case BIT_STRING: {
                return of((BitString) o);
            }
            case ABSOLUTE_TIME: {
                return of((Instant) o);
            }
            case RELATIVE_TIME: {
                return of((Duration) o);
            }
            case EXTENSION: {
                return ofExtension(o);
            }
            default:
                throw new IllegalAccessError("Enumeration type " + type + " not recognized, this is a software bug");
        }
    }

    private final ValueTypeEnum type;
    private final Class<?> classType;
    private final Object valueObject;

    private Value(ValueTypeEnum type, Class<?> classType, Object valueObject) {
        this.type = type;
        this.classType = classType;
        this.valueObject = valueObject;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public Class<?> getClassType() {
        return classType;
    }

    public <T> T value() {
        return (T) valueObject;
    }

    public String toString() {
        return ValueUtil.toString(type, valueObject);
    }

    public byte[] serialize() {
        return ValueUtil.serialize(type, valueObject);
    }
}
