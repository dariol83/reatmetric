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

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import eu.dariolucia.reatmetric.api.rawdata.IRawDataSubscriber;
import eu.dariolucia.reatmetric.api.rawdata.Quality;
import eu.dariolucia.reatmetric.api.rawdata.RawData;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import eu.dariolucia.reatmetric.core.api.IRawDataBroker;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MonitoringDecoder implements IRawDataSubscriber {

    private final Map<Integer, Consumer<RawData>> monitoringMap = new HashMap<>();
    private final Map<Integer, Function<RawData, LinkedHashMap<String, Pair<Integer, Object>>>> renderingMap = new HashMap<>();
    private final IProcessingModel model;

    public MonitoringDecoder(IProcessingModel model, IRawDataBroker broker) {
        this.model = model;
        // Initialise monitoring processing map
        monitoringMap.put(1, this::powerSupplyMon);
        monitoringMap.put(2, this::powerSupplyMon);
        monitoringMap.put(3, this::powerSupplyMon);
        monitoringMap.put(4, this::matrixMon);
        monitoringMap.put(5, this::switchMon);
        monitoringMap.put(6, this::splitterMon);
        monitoringMap.put(7, this::ventilationMon);
        monitoringMap.put(8, this::thermalMon);
        monitoringMap.put(9, this::turbineMon);
        // Initialise rendering map
        renderingMap.put(1, this::powerSupplyParamMap);
        renderingMap.put(2, this::powerSupplyParamMap);
        renderingMap.put(3, this::powerSupplyParamMap);
        renderingMap.put(4, this::matrixParamMap);
        renderingMap.put(5, this::switchParamMap);
        renderingMap.put(6, this::splitterParamMap);
        renderingMap.put(7, this::ventilationParamMap);
        renderingMap.put(8, this::thermalParamMap);
        renderingMap.put(9, this::turbineParamMap);
        // Since this object is performing the parameter decoding function, it registers to the broker to receive
        // notification of received TM data
        broker.subscribe(this, null, new RawDataFilter(true, null, null, Collections.singletonList(TestDriver.STATION_TM), null, Collections.singletonList(Quality.GOOD)), null);
    }

    private void powerSupplyMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = powerSupplyParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> powerSupplyParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Double.class, Double.class, Byte.class, Byte.class };
        String[] names = { "Status", "Tension", "Current", "Protection", "Output"};
        return extract(rawData, names, dataTypes);
    }

    private void matrixMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = matrixParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> matrixParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Byte.class, Byte.class, Byte.class, Byte.class, Byte.class, Byte.class, Byte.class, Byte.class };
        String[] names = { "Status", "Input1", "Input2", "Output", "Wiring", "Diagnostics 1", "Diagnostics 2", "Diagnostics 3", "Diagnostics 4"};
        return extract(rawData, names, dataTypes);
    }

    private void switchMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = switchParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> switchParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class };
        String[] names = { "Position"};
        return extract(rawData, names, dataTypes);
    }

    private void splitterMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = splitterParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> splitterParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Byte.class, Byte.class, Byte.class, Byte.class, Double.class };
        String[] names = { "Status", "Input", "Output1", "Output2", "Output3", "Power"};
        return extract(rawData, names, dataTypes);
    }

    private void ventilationMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = ventilationParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> ventilationParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Byte.class, Double.class, Double.class, Double.class, Double.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class };
        String[] names = { "Status", "Input", "Fan1", "Fan2", "Fan3", "Fan4", "Fan1 Status", "Fan1 Input", "Fan2 Status", "Fan2 Input", "Fan3 Status", "Fan3 Input", "Fan4 Status", "Fan4 Input"};
        return extract(rawData, names, dataTypes);
    }

    private void thermalMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = thermalParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> thermalParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Byte.class, Byte.class, Byte.class, Double.class, Byte.class, Byte.class, Byte.class, Double.class, Byte.class, Byte.class };
        String[] names = { "Status", "Global Status", "Input", "Status A", "Temperature A", "Override A",  "Protection A", "Status B", "Temperature B", "Override B",  "Protection B"};
        return extract(rawData, names, dataTypes);
    }

    private void turbineMon(RawData rawData) {
        Instant genTime = rawData.getGenerationTime();
        LinkedHashMap<String, Pair<Integer, Object>> paramMap = turbineParamMap(rawData);
        inject(genTime, paramMap);
    }

    public LinkedHashMap<String, Pair<Integer, Object>> turbineParamMap(RawData rawData) {
        Class<?>[] dataTypes = { Byte.class, Byte.class, Double.class, Double.class };
        String[] names = { "Status", "Input", "Output", "RPM" };
        return extract(rawData, names, dataTypes);
    }

    private LinkedHashMap<String, Pair<Integer, Object>> extract(RawData rawData, String[] names, Class<?>[] dataTypes) {
        LinkedHashMap<String, Pair<Integer, Object>> toReturn = new LinkedHashMap<>();
        ByteBuffer bb = ByteBuffer.wrap(rawData.getContents());
        // Equipment id * 100
        int equipmentId = Byte.toUnsignedInt(bb.get()) >>> 4;
        equipmentId *= 100;
        // Timestamp
        bb.getLong();
        // Data
        for(int i = 0; i < names.length; ++i) {
            if(dataTypes[i] == Byte.class) {
                toReturn.put(names[i], Pair.of(equipmentId + i, (int) bb.get()));
            }
            if(dataTypes[i] == Double.class) {
                toReturn.put(names[i], Pair.of(equipmentId + i, (int) bb.getDouble()));
            }
            if(dataTypes[i] == Integer.class) {
                toReturn.put(names[i], Pair.of(equipmentId + i, bb.getInt()));
            }
        }
        return toReturn;
    }

    private void inject(Instant genTime, LinkedHashMap<String, Pair<Integer, Object>> paramMap) {
        List<ParameterSample> samples = paramMap.values().stream().map(o -> toParameterSample(genTime, o)).collect(Collectors.toList());
        model.injectParameters(samples);
    }

    private ParameterSample toParameterSample(Instant generationTime, Pair<Integer, Object> integerObjectPair) {
        return ParameterSample.of(integerObjectPair.getFirst(), generationTime, Instant.now(), integerObjectPair.getSecond());
    }

    private void processMonitoringData(RawData rd) {
        ByteBuffer bb = ByteBuffer.wrap(rd.getContents());
        // Read the tag
        int eqId = Byte.toUnsignedInt(bb.get());
        eqId = eqId >>> 4;
        monitoringMap.get(eqId).accept(rd);
    }

    public LinkedHashMap<String, String> render(RawData r) {
        ByteBuffer bb = ByteBuffer.wrap(r.getContents());
        // Read the tag
        int eqId = Byte.toUnsignedInt(bb.get());
        eqId = eqId >>> 4;
        LinkedHashMap<String, Pair<Integer, Object>> decodedMap = renderingMap.get(eqId).apply(r);
        LinkedHashMap<String, String> renderMap = new LinkedHashMap<>();
        for(Map.Entry<String, Pair<Integer, Object>> entry : decodedMap.entrySet()) {
            renderMap.put(entry.getKey(), Objects.toString(entry.getValue().getSecond()));
        }
        return renderMap;
    }

    @Override
    public void dataItemsReceived(List<RawData> messages) {
        for(RawData rd : messages) {
            processMonitoringData(rd);
        }
    }
}
