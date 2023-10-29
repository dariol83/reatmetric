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

import java.util.LinkedHashMap;
import java.util.Map;
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

    public interface IHandler {
        boolean handle(String command, String[] args, BiFunction<String, Object, Boolean> parameterSetter, Consumer<Boolean> executionCompleted);
    }
}
