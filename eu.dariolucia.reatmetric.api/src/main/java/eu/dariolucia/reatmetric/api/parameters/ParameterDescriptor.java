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

package eu.dariolucia.reatmetric.api.parameters;

import eu.dariolucia.reatmetric.api.common.AbstractSystemEntityDescriptor;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import java.util.List;

public class ParameterDescriptor extends AbstractSystemEntityDescriptor {

    private final String description;
    private final ValueTypeEnum rawDataType;
    private final ValueTypeEnum engineeringDataType;
    private final String unit;
    private final boolean synthetic;
    private final boolean settable;
    private final String setterType;
    private final List<Object> expectedRawValues;
    private final List<Object> expectedEngineeringValues;

    public ParameterDescriptor(SystemEntityPath path, int externalId, String description, ValueTypeEnum rawDataType, ValueTypeEnum engineeringDataType, String unit, boolean synthetic, boolean settable, String setterType, List<Object> expectedRawValues, List<Object> expectedEngineeringValues) {
        super(path, externalId, SystemEntityType.PARAMETER);
        this.description = description;
        this.rawDataType = rawDataType;
        this.engineeringDataType = engineeringDataType;
        this.unit = unit;
        this.synthetic = synthetic;
        this.settable = settable;
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
        this.setterType = setterType;
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

    public boolean isSynthetic() {
        return synthetic;
    }

    public boolean isSettable() {
        return settable;
    }

    public List<Object> getExpectedRawValues() {
        return expectedRawValues;
    }

    public List<Object> getExpectedEngineeringValues() {
        return expectedEngineeringValues;
    }

    public String getSetterType() {
        return setterType;
    }
}
