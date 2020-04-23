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

    public ActivityArgumentDescriptor(String name, String description, ValueTypeEnum rawDataType, ValueTypeEnum engineeringDataType, String unit, boolean fixed, boolean defaultValuePresent, Object engineeringDefaultValue, Object rawDefaultValue, boolean decalibrationSet, boolean checkSet) {
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
}
