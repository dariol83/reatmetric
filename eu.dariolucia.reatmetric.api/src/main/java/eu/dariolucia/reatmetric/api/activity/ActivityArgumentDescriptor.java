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

package eu.dariolucia.reatmetric.api.activity;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.util.List;

/**
 * The descriptor of an activity argument.
 *
 * Objects of this class are immutable.
 */
public class ActivityArgumentDescriptor {

    private final String name;
    private final String description;
    private final ValueTypeEnum rawDataType;
    private final ValueTypeEnum engineeringDataType;
    private final String unit;
    private final boolean fixed;
    private final boolean defaultValuePresent;
    private final Object engineeringDefaultValue;
    private final Object rawDefaultValue;
    private final boolean decalibrationSet;
    private final boolean checkSet;
    private final List<Object> expectedRawValues;
    private final List<Object> expectedEngineeringValues;

    public ActivityArgumentDescriptor(String name, String description, ValueTypeEnum rawDataType, ValueTypeEnum engineeringDataType, String unit, boolean fixed, boolean defaultValuePresent, Object engineeringDefaultValue, Object rawDefaultValue, boolean decalibrationSet, boolean checkSet, List<Object> expectedRawValues, List<Object> expectedEngineeringValues) {
        this.name = name;
        this.description = description;
        this.rawDataType = rawDataType;
        this.engineeringDataType = engineeringDataType;
        this.unit = unit;
        this.fixed = fixed;
        this.defaultValuePresent = defaultValuePresent;
        this.engineeringDefaultValue = engineeringDefaultValue;
        this.rawDefaultValue = rawDefaultValue;
        this.decalibrationSet = decalibrationSet;
        this.checkSet = checkSet;
        if(expectedRawValues != null) {
            this.expectedRawValues = List.copyOf(expectedRawValues);
        } else {
            this.expectedRawValues = null;
        }
        if(expectedEngineeringValues != null) {
            this.expectedEngineeringValues = List.copyOf(expectedEngineeringValues);
        } else {
            this.expectedEngineeringValues = null;
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ValueTypeEnum getRawDataType() {
        return rawDataType;
    }

    public ValueTypeEnum getEngineeringDataType() {
        return engineeringDataType;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean isDefaultValuePresent() {
        return defaultValuePresent;
    }

    public Object getEngineeringDefaultValue() {
        return engineeringDefaultValue;
    }

    public Object getRawDefaultValue() {
        return rawDefaultValue;
    }

    public boolean isDecalibrationSet() {
        return decalibrationSet;
    }

    public boolean isCheckSet() {
        return checkSet;
    }

    public List<Object> getExpectedRawValues() {
        return expectedRawValues;
    }

    public List<Object> getExpectedEngineeringValues() {
        return expectedEngineeringValues;
    }
}
