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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ValueUtil {

    public static String toString(Object valueObject) {
        if(valueObject instanceof byte[]) {
            return StringUtil.toHexDump((byte[]) valueObject);
        } else {
            return Objects.toString(valueObject);
        }
    }

    public static String toString(ValueTypeEnum type, Object valueObject) {
        if(type != ValueTypeEnum.EXTENSION || valueObject == null) {
            return valueObject == null ? null : type.toString(valueObject);
        } else {
            IValueExtensionHandler handler = CLASS2HANDLER.get(valueObject.getClass());
            if(handler == null) {
                // Try not to fail: use toString()
                return valueObject.toString();
            } else {
                return handler.toString(valueObject);
            }
        }
    }

    /**
     * This method attempts to parse and detect the type of the provided string.
     *
     * @param s the string to parse
     * @return a pair with the value (as object) and the detected {@link ValueTypeEnum}
     */
    public static Pair<Object, ValueTypeEnum> tryParse(String s) {
        if(s == null) {
            return null;
        }
        // String is empty -> can be an empty BIT_STRING, an empty OCTET_STRING, an empty CHARACTER_STRING. The latter is probable.
        if(s.isEmpty()) {
            return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
        }
        // String starts with '[ ' and ends with ' ]'  -> try GROUP, if you fail it is a CHARACTER_STRING
        if(s.startsWith("[ ") && s.endsWith(" ]")) {
            try {
                return Pair.of(parse(ValueTypeEnum.ARRAY, s), ValueTypeEnum.ARRAY);
            } catch (Exception e) {
                return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
            }
        }
        // String starts with 0x -> try OCTET_STRING, if you fail it is a CHARACTER_STRING
        if(s.startsWith("0x")) {
            try {
                return Pair.of(parse(ValueTypeEnum.OCTET_STRING, s), ValueTypeEnum.OCTET_STRING);
            } catch (Exception e) {
                return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
            }
        }
        // String ends with Z and contains T, -, : -> try ABSOLUTE_TIME, if you fail it is a CHARACTER_STRING
        if(s.endsWith("Z") && s.contains("T") && s.contains(":") && s.contains("-")) {
            try {
                return Pair.of(parse(ValueTypeEnum.ABSOLUTE_TIME, s), ValueTypeEnum.ABSOLUTE_TIME);
            } catch (Exception e) {
                return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
            }
        }
        // String starts with PT -> try RELATIVE_TIME, if you fail it is a CHARACTER_STRING
        if(s.startsWith("PT")) {
            try {
                return Pair.of(parse(ValueTypeEnum.RELATIVE_TIME, s), ValueTypeEnum.RELATIVE_TIME);
            } catch (Exception e) {
                return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
            }
        }
        // String starts with _ -> try BIT_STRING, if you fail it is a CHARACTER_STRING
        if(s.startsWith("_")) {
            try {
                return Pair.of(parse(ValueTypeEnum.BIT_STRING, s), ValueTypeEnum.BIT_STRING);
            } catch (Exception e) {
                return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
            }
        }
        // If string is equal to true or false -> boolean
        if(s.equals(Boolean.TRUE.toString()) || s.equals(Boolean.FALSE.toString())) {
            return Pair.of(s.equals(Boolean.TRUE.toString()), ValueTypeEnum.BOOLEAN);
        }
        // At this stage, try with an integer number... if fail, try with long, if fail, try with double.
        try {
            int value = Integer.parseInt(s);
            if(value < 0) {
                return Pair.of((long) value, ValueTypeEnum.SIGNED_INTEGER);
            } else {
                return Pair.of(value, ValueTypeEnum.ENUMERATED);
            }
        } catch (NumberFormatException e) {
            // Skip
        }
        try {
            long value = Long.parseLong(s);
            if(value < 0) {
                return Pair.of(value, ValueTypeEnum.SIGNED_INTEGER);
            } else {
                return Pair.of(value, ValueTypeEnum.UNSIGNED_INTEGER);
            }
        } catch (NumberFormatException e) {
            // Skip
        }
        try {
            double value = Double.parseDouble(s);
            return Pair.of(value, ValueTypeEnum.REAL);
        } catch (NumberFormatException e) {
            // Skip
        }
        // If fail, it is a string.
        return Pair.of(s, ValueTypeEnum.CHARACTER_STRING);
    }

    public static Object parse(ValueTypeEnum type, String valueAsString) {
        if(type == ValueTypeEnum.EXTENSION) {
            throw new IllegalArgumentException("Cannot parse an extension using this method, use parseExtension(Class<?>,String)");
        } else if(valueAsString == null) {
            return null;
        } else {
            return type.parse(valueAsString);
        }
    }

    public static <T> Object parseExtension(Class<T> type, String valueAsString) {
        if(valueAsString == null) {
            return null;
        } else {
            IValueExtensionHandler handler = CLASS2HANDLER.get(type);
            if(handler == null) {
                // Here you have to fail
                throw new IllegalStateException("Value handler for type class " + type.getName() + " not registered");
            }
            return handler.parse(valueAsString);
        }
    }

    public static Object deserialize(byte[] dump) {
        // Type
        ValueTypeEnum type = ValueTypeEnum.fromCode(dump[0] & 0x7F);
        boolean nullValue = (dump[0] & 0x80) != 0;
        if(nullValue) {
            return null;
        }
        // Decode
        switch(type) {
            case BOOLEAN: {
                return (dump[1] != 0);
            }
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER: {
                return ByteBuffer.wrap(dump, 1, 8).getLong();
            }
            case REAL: {
                return ByteBuffer.wrap(dump, 1, 8).getDouble();
            }
            case ENUMERATED: {
                return ByteBuffer.wrap(dump, 1, 4).getInt();
            }
            case CHARACTER_STRING: {
                return new String(dump, 1, dump.length - 1, StandardCharsets.US_ASCII);
            }
            case OCTET_STRING: {
                return Arrays.copyOfRange(dump, 1, dump.length);
            }
            case BIT_STRING: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                int numBits = bb.getInt();
                return new BitString(Arrays.copyOfRange(dump, 5, dump.length), numBits);
            }
            case ABSOLUTE_TIME: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                return Instant.ofEpochSecond(bb.getLong(), bb.getInt());
            }
            case RELATIVE_TIME: {
                ByteBuffer bb = ByteBuffer.wrap(dump, 1, dump.length - 1);
                return Duration.ofSeconds(bb.getLong(), bb.getInt());
            }
            case ARRAY: {
                try {
                    return deserializeArray(dump, 1, dump.length - 1);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot deserialize value of type ARRAY", e);
                }
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
                            return vv;
                        } catch (IOException | ClassNotFoundException e) {
                            throw new IllegalStateException("Cannot deserialize value of type ID " + typeId, e);
                        }
                    } else {
                        throw new IllegalStateException("Cannot deserialize value of type ID " + typeId + ": type ID not found");
                    }
                } else {
                    return handler.deserialize(dump, 3, dump.length - 3);
                }
            }
            default:
                throw new IllegalAccessError("Enumeration type " + type + " not recognized, this is a software bug");
        }
    }

    public static byte[] serialize(Object valueObject) {
        if(valueObject == null) {
            return new byte[]{(byte) (ValueTypeEnum.EXTENSION.getCode() | 0x80)}; // Not really important
        }
        for(ValueTypeEnum vte : ValueTypeEnum.values()) {
            if(vte.getAssignedClass().equals(valueObject.getClass())) {
                return serialize(vte, valueObject);
            }
        }
        return serialize(ValueTypeEnum.EXTENSION, valueObject);
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
     * @param type the type
     * @param valueObject the value
     * @return the serialized Value
     */
    public static byte[] serialize(ValueTypeEnum type, Object valueObject) {
        if(valueObject == null) {
            return new byte[]{(byte) (type.getCode() | 0x80)};
        }
        switch(type) {
            case BOOLEAN: {
                return new byte[] { (byte) type.getCode(), (boolean) valueObject ? (byte) 1 : (byte) 0};
            }
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER: {
                ByteBuffer bb = ByteBuffer.allocate(9);
                bb.put((byte) type.getCode());
                bb.putLong((Long) valueObject);
                return bb.array();
            }
            case REAL: {
                ByteBuffer bb = ByteBuffer.allocate(9);
                bb.put((byte) type.getCode());
                bb.putDouble((Double) valueObject);
                return bb.array();
            }
            case ENUMERATED: {
                ByteBuffer bb = ByteBuffer.allocate(5);
                bb.put((byte) type.getCode());
                bb.putInt((Integer) valueObject);
                return bb.array();
            }
            case CHARACTER_STRING: {
                String val = (String) valueObject;
                byte[] valChars = val.getBytes(StandardCharsets.US_ASCII);
                ByteBuffer bb = ByteBuffer.allocate(1 + valChars.length);
                bb.put((byte) type.getCode());
                bb.put(valChars);
                return bb.array();
            }
            case OCTET_STRING: {
                byte[] val = (byte[]) valueObject;
                ByteBuffer bb = ByteBuffer.allocate(1 + val.length);
                bb.put((byte) type.getCode());
                bb.put(val);
                return bb.array();
            }
            case BIT_STRING: {
                BitString val = (BitString) valueObject;
                byte[] toStore = val.getData();
                ByteBuffer bb = ByteBuffer.allocate(5 + toStore.length);
                bb.put((byte) type.getCode());
                // Number of bits, it is used to derive the bytes needed when deserializing
                bb.putInt(val.getLength());
                bb.put(toStore);
                return bb.array();
            }
            case ABSOLUTE_TIME: {
                Instant val = (Instant) valueObject;
                ByteBuffer bb = ByteBuffer.allocate(13);
                bb.put((byte) type.getCode());
                bb.putLong(val.getEpochSecond());
                bb.putInt(val.getNano());
                return bb.array();
            }
            case RELATIVE_TIME: {
                Duration val = (Duration) valueObject;
                ByteBuffer bb = ByteBuffer.allocate(13);
                bb.put((byte) type.getCode());
                bb.putLong(val.getSeconds());
                bb.putInt(val.getNano());
                return bb.array();
            }
            case ARRAY: {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.write(type.getCode());
                    serializeArray(oos, (Array) valueObject);
                    oos.flush();
                    oos.close();
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot serialize value of type " + valueObject.getClass().getName(), e);
                }
            }
            case EXTENSION: {
                byte[] serialized;
                IValueExtensionHandler handler = CLASS2HANDLER.get(valueObject.getClass());
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

    private static void serializeArray(ObjectOutputStream oos, Array valueObject) throws IOException {
        // Add number of records
        oos.writeShort((short)valueObject.getRecords().size());
        // Process the records
        for(Array.Record record : valueObject.getRecords()) {
            serializeArrayRecord(oos, record);
        }
    }

    private static void serializeArrayRecord(ObjectOutputStream oos, Array.Record record) throws IOException {
        // Write number of record elements
        oos.writeShort((short) record.getElements().size());
        //
        for(Pair<String, Object> elem : record.getElements()) {
            // Serialize the string directly with length (4 bytes) plus string
            byte[] strBytes = elem.getFirst().getBytes(StandardCharsets.ISO_8859_1);
            oos.writeInt(strBytes.length);
            oos.write(strBytes);
            // Serialize the object: derive its type from the class, and use the serialize method
            byte[] value = serialize(elem.getSecond());
            oos.writeInt(value.length);
            oos.write(value);
        }
    }

    private static Array deserializeArray(byte[] dump, int offset, int length) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(dump, offset, length);
        ObjectInputStream oin = new ObjectInputStream(bin);
        // Read number of records
        short numRecords = oin.readShort();
        List<Array.Record> records = new ArrayList<>(Math.min(1, numRecords));
        for(int i = 0; i < numRecords; ++i) {
            // Read one record
            Array.Record record = deserializeArrayRecord(oin);
            records.add(record);
        }
        return new Array(records);
    }

    private static Array.Record deserializeArrayRecord(ObjectInputStream oin) throws IOException {
        // Read number of record elements
        short numElems = oin.readShort();
        List<Pair<String, Object>> elems = new ArrayList<>(Math.min(1, numElems));
        //
        for(int i = 0; i < numElems; ++i) {
            // Deserialize the string directly with length (4 bytes) plus string
            int strLen = oin.readInt();
            byte[] strBytes = oin.readNBytes(strLen);
            String strName = StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(strBytes)).toString();
            // Deserialize the object: derive its type from the class, and use the serialize method
            int objLen = oin.readInt();
            byte[] objBytes = oin.readNBytes(objLen);
            Object objValue = deserialize(objBytes);
            elems.add(Pair.of(strName, objValue));
        }
        return new Array.Record(elems);
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

    public static int compare(Object o1, Object o2) {
        // Null always less than anything else
        if(o1 == null && o2 == null) {
            return 0;
        } else if(o1 == null) {
            return -1;
        } else if(o2 == null) {
            return 1;
        } else {
            return ((Comparable) o1).compareTo(o2);
        }
    }

    public static Object convert(Object result, ValueTypeEnum type) throws ValueException {
        if(result == null) {
            return null;
        }
        if(type == ValueTypeEnum.EXTENSION) {
            return result;
        }
        if(result.getClass().equals(type.getAssignedClass())) {
            return result;
        }
        try {
            // If the result is a string, then try to parse it
            if (result instanceof String) {
                return ValueUtil.parse(type, (String) result);
            }
            // If the result is a byte array, then try to deserialize it
            if (result instanceof byte[]) {
                return ValueUtil.deserialize((byte[]) result);
            }
            // Try to adapt the specified result to the specified type
            if(type == ValueTypeEnum.BOOLEAN) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue() != 0.0;
                }
            }
            if (type == ValueTypeEnum.UNSIGNED_INTEGER || type == ValueTypeEnum.SIGNED_INTEGER) {
                if (result instanceof Number) {
                    return ((Number) result).longValue();
                }
                if (result instanceof Boolean) {
                    return ((Boolean) result) ? 1L : 0L;
                }
            }
            if (type == ValueTypeEnum.ENUMERATED) {
                if (result instanceof Number) {
                    return ((Number) result).intValue();
                }
                if (result instanceof Boolean) {
                    return ((Boolean) result) ? 1 : 0;
                }
            }
            if (type == ValueTypeEnum.REAL) {
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
                if (result instanceof Boolean) {
                    return ((Boolean) result) ? 1.0 : 0.0;
                }
            }
            if (type == ValueTypeEnum.ABSOLUTE_TIME) {
                if (result instanceof Number) {
                    // Assume number of milliseconds since epoch
                    return Instant.ofEpochMilli(((Number) result).longValue());
                }
            }
            if (type == ValueTypeEnum.RELATIVE_TIME) {
                if (result instanceof Number) {
                    // Assume number of milliseconds
                    return Duration.ofMillis(((Number) result).longValue());
                }
            }
        } catch (Exception e) {
            throw new ValueException("Object " + result + " cannot be converted to " + type + ": " + e.getMessage(), e);
        }
        throw new ValueException("Object " + result + " cannot be converted to " + type);
    }

    public static boolean typeMatch(ValueTypeEnum type, Object value) {
        if(value == null) {
            // Absence of value is considered a type match
            return true;
        }
        if(type == ValueTypeEnum.EXTENSION) {
            // Type EXTENSION cannot be used to evaluate type match: the method returns true, because it is in the hand of the person using the extension to check properly
            return true;
        } else {
            return type.getAssignedClass().isAssignableFrom(value.getClass());
        }

    }
}
