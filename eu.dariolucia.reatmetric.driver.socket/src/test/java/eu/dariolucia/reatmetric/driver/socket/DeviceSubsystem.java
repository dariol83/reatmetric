/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class DeviceSubsystem {

    private final Map<String, Object> id2value = new LinkedHashMap<>();
    private final Map<String, ValueTypeEnum> id2type = new LinkedHashMap<>();
    private final Map<String, IHandler> id2handler = new LinkedHashMap<>();
    private final String name;
    private final ExecutorService executor;

    public DeviceSubsystem(String name) {
        this.name = name;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, this.name + " Command Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public String getName() {
        return name;
    }

    public DeviceSubsystem addParameter(String id, ValueTypeEnum type, Object initialValue) {
        this.id2value.put(id, initialValue);
        this.id2type.put(id, type);
        return this;
    }

    public DeviceSubsystem addHandler(String command, IHandler handler) {
        this.id2handler.put(command, handler);
        return this;
    }

    public synchronized Map<String, Object> poll() {
        return new LinkedHashMap<>(this.id2value);
    }

    public synchronized Object get(String name) {
        return this.id2value.get(name);
    }

    public boolean set(String id, Object value, boolean synchronous, Consumer<Boolean> executionCompleted) throws InterruptedException {
        Future<Boolean> result = executor.submit(() -> internalSet(id, value, executionCompleted));
        if (synchronous) {
            try {
                return result.get();
            } catch (ExecutionException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean set(int parameterIdx, Object value, boolean synchronous, Consumer<Boolean> executionCompleted) throws InterruptedException {
        List<String> params = new ArrayList<>(this.id2value.keySet());
        if(params.size() <= parameterIdx) {
            return false;
        }
        String parameter = params.get(parameterIdx);
        Future<Boolean> result = executor.submit(() -> internalSet(parameter, value, executionCompleted));
        if (synchronous) {
            try {
                return result.get();
            } catch (ExecutionException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean internalSet(String id, Object value, Consumer<Boolean> executionCompleted) {
        boolean result = parameterSetter(id, value);
        if(executionCompleted != null) {
            executionCompleted.accept(result);
        }
        return result;
    }

    public boolean invoke(String command, String[] args, boolean synchronous, Consumer<Boolean> executionCompleted) throws InterruptedException {
        if(id2handler.containsKey(command)) {
            IHandler h = id2handler.get(command);
            System.out.println(new Date() + " [" + getName() + "] INVOKE " + command + " : " + Arrays.toString(args));
            Future<Boolean> result = executor.submit(() -> h.handle(command, args, this::parameterSetter, executionCompleted));
            if (synchronous) {
                try {
                    return result.get();
                } catch (ExecutionException e) {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean parameterSetter(String id, Object value) {
        if(id2type.containsKey(id)) {
            if(ValueUtil.typeMatch(id2type.get(id), value)) {
                synchronized (this) {
                    System.out.println(new Date() + " [" + getName() + "] SET " + id + " -> " + value);
                    this.id2value.put(id, value);
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void dispose() {
        this.executor.shutdown();
    }

    public ValueTypeEnum getTypeOf(String key) {
        return id2type.get(key);
    }

    public void encodeStateTo(ByteArrayOutputStream bos) throws IOException {
        Map<String, Object> polled = poll();
        for(Map.Entry<String, Object> e : polled.entrySet()) {
            if(e.getValue() instanceof Integer) {
                bos.write(fromInt((Integer) e.getValue()));
            } else if(e.getValue() instanceof Double) {
                bos.write(fromDouble((Double) e.getValue()));
            } else if(e.getValue() instanceof Long) {
                bos.write(fromLong((Long) e.getValue()));
            } else { // String
                String val = Objects.toString(e.getValue());
                bos.write(fromString(val));
            }
        }
    }

    private byte[] fromInt(Integer value) {
        byte[] b = new byte[4];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putInt(value);
        return b;
    }

    private byte[] fromLong(Long value) {
        byte[] b = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putLong(value);
        return b;
    }

    private byte[] fromDouble(Double value) {
        byte[] b = new byte[8];
        ByteBuffer bb = ByteBuffer.wrap(b);
        bb.putDouble(value);
        return b;
    }

    private byte[] fromString(String value) throws IOException {
        byte[] encodedString = value.getBytes(StandardCharsets.US_ASCII);
        byte[] prefix = fromInt(encodedString.length);
        //
        int rest = encodedString.length % 4;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(prefix);
        bos.write(encodedString);
        if(rest != 0) {
            bos.write(new byte[4 - rest]);
        }
        return bos.toByteArray();
    }

    public Object decodeValueFrom(int parameterIdx, ByteBuffer bb) {
        List<String> params = new ArrayList<>(this.id2value.keySet());
        if(params.size() <= parameterIdx) {
            return null;
        }
        String parameter = params.get(parameterIdx);
        ValueTypeEnum type = id2type.get(parameter);
        switch (type) {
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER:
                return bb.getLong();
            case ENUMERATED:
                return bb.getInt();
            case REAL:
                return bb.getDouble();
            case CHARACTER_STRING:
                return toCharString(bb);
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }

    private Object toCharString(ByteBuffer bb) {
        // Read string length
        int len = bb.getInt();
        // Read the string
        byte[] data = new byte[len];
        bb.get(data);
        return new String(data, StandardCharsets.US_ASCII);
    }

    public interface IHandler {
        boolean handle(String command, String[] args, BiFunction<String, Object, Boolean> parameterSetter, Consumer<Boolean> executionCompleted);
    }
}
