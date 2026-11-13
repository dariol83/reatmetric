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
 * The descriptor of a activity argument (simple).
 *
 * Objects of this class are immutable.
 */
public class ActivityPlainArgumentDescriptor extends AbstractActivityArgumentDescriptor {

    /** The raw data type. */
    private final ValueTypeEnum rawDataType;
    /** The engineering data type. */
    private final ValueTypeEnum engineeringDataType;
    /** The unit of the argument. */
    private final String unit;
    /** Whether the argument is fixed and cannot be modified. */
    private final boolean fixed;
    /** Whether a default value is present. */
    private final boolean defaultValuePresent;
    /** The engineering default value. */
    private final Object engineeringDefaultValue;
    /** The raw default value. */
    private final Object rawDefaultValue;
    /** Whether a decalibration is provided. */
    private final boolean decalibrationSet;
    /** Whether check conditions are defined. */
    private final boolean checkSet;
    /** The list of acceptable raw values. */
    private final List<Object> expectedRawValues;
    /** The list of acceptable engineering values. */
    private final List<Object> expectedEngineeringValues;

    /**
     * Constructor of the class.
     *
     * @param name the name of the argument
     * @param description the description of the argument
     * @param rawDataType the raw data type
     * @param engineeringDataType the engineering data type
     * @param unit the unit, can be null
     * @param fixed true if the argument is fixed, i.e. cannot be modified by an invocation, otherwise false
     * @param defaultValuePresent true if either the engineering or the raw default value is present, otherwise false
     * @param engineeringDefaultValue the engineering default value, can be null
     * @param rawDefaultValue the raw default value, can be null
     * @param decalibrationSet true if a decalibration is provided for this argument, otherwise false
     * @param checkSet true if check conditions are defined for this argument, otherwise false
     * @param expectedRawValues the list of acceptable raw values that can be used
     * @param expectedEngineeringValues the list of acceptable engineering values that can be used
     */
    public ActivityPlainArgumentDescriptor(String name, String description, ValueTypeEnum rawDataType, ValueTypeEnum engineeringDataType, String unit, boolean fixed, boolean defaultValuePresent, Object engineeringDefaultValue, Object rawDefaultValue, boolean decalibrationSet, boolean checkSet, List<Object> expectedRawValues, List<Object> expectedEngineeringValues) {
        super(name, description);
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

    /**
     * Returns the raw data type.
     *
     * @return the raw data type
     */
    public ValueTypeEnum getRawDataType() {
        return rawDataType;
    }

    /**
     * Returns the engineering data type.
     *
     * @return the engineering data type
     */
    public ValueTypeEnum getEngineeringDataType() {
        return engineeringDataType;
    }

    /**
     * Returns the unit of the argument.
     *
     * @return the unit, may be null
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns whether the argument is fixed and cannot be modified.
     *
     * @return true if fixed, false otherwise
     */
    public boolean isFixed() {
        return fixed;
    }

    /**
     * Returns whether a default value is present.
     *
     * @return true if a default value is present, false otherwise
     */
    public boolean isDefaultValuePresent() {
        return defaultValuePresent;
    }

    /**
     * Returns the engineering default value.
     *
     * @return the engineering default value, may be null
     */
    public Object getEngineeringDefaultValue() {
        return engineeringDefaultValue;
    }

    /**
     * Returns the raw default value.
     *
     * @return the raw default value, may be null
     */
    public Object getRawDefaultValue() {
        return rawDefaultValue;
    }

    /**
     * Returns whether a decalibration is provided.
     *
     * @return true if decalibration is set, false otherwise
     */
    public boolean isDecalibrationSet() {
        return decalibrationSet;
    }

    /**
     * Returns whether check conditions are defined.
     *
     * @return true if check conditions are set, false otherwise
     */
    public boolean isCheckSet() {
        return checkSet;
    }

    /**
     * Returns the list of acceptable raw values.
     *
     * @return the list of expected raw values, may be null
     */
    public List<Object> getExpectedRawValues() {
        return expectedRawValues;
    }

    /**
     * Returns the list of acceptable engineering values.
     *
     * @return the list of expected engineering values, may be null
     */
    public List<Object> getExpectedEngineeringValues() {
        return expectedEngineeringValues;
    }
}
