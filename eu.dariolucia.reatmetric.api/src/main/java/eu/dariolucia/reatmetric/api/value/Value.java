/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.api.value;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
        } else if(valueAsString == null) {
            return nill(type);
        } else {
            Object o = type.parse(valueAsString);
            return new Value(type, o.getClass(), o);
        }
    }

    public static <T> Value parseExtension(Class<T> type, String valueAsString) {
        if(valueAsString == null) {
            return nill(ValueTypeEnum.EXTENSION);
        } else {
            IValueExtensionHandler handler = CLASS2HANDLER.get(type);
            if(handler == null) {
                // Here you have to fail
                throw new IllegalStateException("Value handler for type class " + type.getName() + " not registered");
            }
            return ofExtension(handler.parse(valueAsString));
        }
    }

    public static Value deserialize(byte[] dump) {
        // Type
        int ctype = dump[0] & 0x7F;
        ValueTypeEnum type = ValueTypeEnum.fromCode(ctype);
        boolean nullValue = (dump[0] & 0x80) != 0;
        if(nullValue) {
            return nill(type);
        }
        // Decode
        switch(type) {
            case BOOLEAN: {
                return of(dump[1] != 0);
            }
            case UNSIGNED_INTEGER: {
                return of(ByteBuffer.wrap(dump, 1, 8).getLong(), true);
            }
            case SIGNED_INTEGER: {
                return of(ByteBuffer.wrap(dump, 1, 8).getLong(), false);
            }
            case REAL: {
                return of(ByteBuffer.wrap(dump, 1, 8).getDouble());
            }
            case ENUMERATED: {
                return of(ByteBuffer.wrap(dump, 1, 8).getInt());
            }
            case CHARACTER_STRING: {
                String val = new String(dump, 1, dump.length - 1, StandardCharsets.US_ASCII);
                return of(val);
            }
            case OCTET_STRING: {
                return of(Arrays.copyOfRange(dump, 1, dump.length));
            }
            case BIT_STRING: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                int numBits = bb.getInt();
                return of(new BitString(Arrays.copyOfRange(dump, 5, dump.length), numBits));
            }
            case ABSOLUTE_TIME: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                return of(Instant.ofEpochSecond(bb.getLong(), bb.getInt()));
            }
            case RELATIVE_TIME: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                return of(Duration.ofSeconds(bb.getLong(), bb.getInt()));
            }
            case EXTENSION: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                int typeId = bb.getShort();
                IValueExtensionHandler handler = TYPE2HANDLER.get(typeId);
                if(handler == null) {
                    if(typeId == -1) {
                        try {
                            // Try not to fail: use Java serialisation
                            ByteArrayInputStream bos = new ByteArrayInputStream(dump, 3, dump.length - 3);
                            ObjectInputStream oos = new ObjectInputStream(bos);
                            Object vv = oos.readObject();
                            oos.close();
                            return ofExtension(vv);
                        } catch (IOException | ClassNotFoundException e) {
                            throw new IllegalStateException("Cannot deserialize value of type ID " + typeId, e);
                        }
                    } else {
                        throw new IllegalStateException("Cannot deserialize value of type ID " + typeId + ": type ID not found");
                    }
                } else {
                    Object vv = handler.deserialize(dump, 3, dump.length - 3);
                    return ofExtension(vv);
                }
            }
            default:
                throw new IllegalAccessError("Enumeration type " + type + " not recognized, this is a software bug");
        }
    }

    private static final Map<Class<?>, IValueExtensionHandler> CLASS2HANDLER = new HashMap<>();
    private static final Map<Integer, IValueExtensionHandler> TYPE2HANDLER = new HashMap<>();

    static {
        ServiceLoader<IValueExtensionHandler> codecSetLoader
                = ServiceLoader.load(IValueExtensionHandler.class);
        for (IValueExtensionHandler cp : codecSetLoader) {
            Class typeClass = cp.typeClass();
            int typeId = cp.typeId();
            if (CLASS2HANDLER.containsKey(typeClass)) {
                throw new IllegalStateException("Type class " + typeClass.getName() + " declared in value extension handler " + cp.getClass().getName() + " already registered");
            }
            if (TYPE2HANDLER.containsKey(typeId)) {
                throw new IllegalStateException("Type id " + typeId + " for type class " + typeClass.getName() + " declared in value extension handler " + cp.getClass().getName() + " already registered");
            }
            CLASS2HANDLER.put(typeClass, cp);
            TYPE2HANDLER.put(typeId, cp);
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
        if(type != ValueTypeEnum.EXTENSION || valueObject == null) {
            return valueObject == null ? null : type.toString(valueObject);
        } else {
            IValueExtensionHandler handler = CLASS2HANDLER.get(classType);
            if(handler == null) {
                // Try not to fail: use toString()
                return valueObject.toString();
            } else {
                return handler.toString(valueObject);
            }
        }
    }

    /**
     * The serialisation is based on the 'type'-'value' approach:
     * <ul>
     *     <li>'type' is encoded in 1 byte, whose value encodes the {@link ValueTypeEnum} code: the MSB bit, if set, indicates a null value</li>
     *     <li>'value' is the encoded value, depending on the type</li>
     * </ul>
     * In case of EXTENSION types, 'value' is also split into:
     * <ul>
     *     <li>a 2 bytes prefix, identifying the type ID of the {@link IValueExtensionHandler} (-1 if Java Serialization is used)</li>
     *     <li>the serialized value provided by the corresponding {@link IValueExtensionHandler}</li>
     * </ul>
     *
     * @return the serialized {@link Value}
     */
    public byte[] serialize() {
        if(valueObject == null) {
            return new byte[]{(byte) (type.getCode() | 0x80)};
        }
        switch(type) {
            case BOOLEAN: {
                return new byte[] { (byte) type.getCode(), value() ? (byte) 1 : (byte) 0};
            }
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER: {
                ByteBuffer bb = ByteBuffer.allocate(9);
                bb.put((byte) type.getCode());
                bb.putLong(value());
                return bb.array();
            }
            case REAL: {
                ByteBuffer bb = ByteBuffer.allocate(9);
                bb.put((byte) type.getCode());
                bb.putDouble(value());
                return bb.array();
            }
            case ENUMERATED: {
                ByteBuffer bb = ByteBuffer.allocate(5);
                bb.put((byte) type.getCode());
                bb.putInt(value());
                return bb.array();
            }
            case CHARACTER_STRING: {
                String val = value();
                byte[] valChars = val.getBytes(StandardCharsets.US_ASCII);
                ByteBuffer bb = ByteBuffer.allocate(1 + valChars.length);
                bb.put((byte) type.getCode());
                bb.put(valChars);
                return bb.array();
            }
            case OCTET_STRING: {
                byte[] val = value();
                ByteBuffer bb = ByteBuffer.allocate(1 + val.length);
                bb.put((byte) type.getCode());
                bb.put(val);
                return bb.array();
            }
            case BIT_STRING: {
                BitString val = value();
                byte[] toStore = val.getData();
                ByteBuffer bb = ByteBuffer.allocate(5 + toStore.length);
                bb.put((byte) type.getCode());
                // Number of bits, it is used to derive the bytes needed when deserializing
                bb.putInt(val.getLength());
                bb.put(toStore);
                return bb.array();
            }
            case ABSOLUTE_TIME: {
                Instant val = value();
                ByteBuffer bb = ByteBuffer.allocate(13);
                bb.put((byte) type.getCode());
                bb.putLong(val.getEpochSecond());
                bb.putInt(val.getNano());
                return bb.array();
            }
            case RELATIVE_TIME: {
                Duration val = value();
                ByteBuffer bb = ByteBuffer.allocate(13);
                bb.put((byte) type.getCode());
                bb.putLong(val.getSeconds());
                bb.putInt(val.getNano());
                return bb.array();
            }
            case EXTENSION: {
                byte[] serialized;
                IValueExtensionHandler handler = CLASS2HANDLER.get(classType);
                if(handler == null) {
                    try {
                        // Try not to fail: use Java serialisation
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(valueObject);
                        oos.flush();
                        oos.close();
                        serialized = bos.toByteArray();
                    } catch (IOException e) {
                        throw new IllegalStateException("Cannot serialize value of type " + valueObject.getClass().getName(), e);
                    }
                } else {
                    serialized = handler.serialize(valueObject);
                }
                ByteBuffer bb = ByteBuffer.allocate(3 + serialized.length);
                bb.put((byte) type.getCode());
                bb.putShort(handler == null ? -1 : handler.typeId());
                bb.put(serialized);
                return bb.array();
            }
            default:
                throw new IllegalAccessError("Enumeration type " + type + " not recognized, this is a software bug");
        }
    }

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
}
