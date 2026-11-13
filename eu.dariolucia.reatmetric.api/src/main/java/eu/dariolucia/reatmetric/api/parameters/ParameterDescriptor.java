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

/**
 * Describes a parameter in the ReatMetric system.
 * This class contains metadata about a parameter, including its data types, unit, and other characteristics.
 */
public class ParameterDescriptor extends AbstractSystemEntityDescriptor {

    /**
     * The description of the parameter.
     */
    private final String description;
    
    /**
     * The raw data type of the parameter.
     */
    private final ValueTypeEnum rawDataType;
    
    /**
     * The engineering data type of the parameter (calibrated/processed).
     */
    private final ValueTypeEnum engineeringDataType;
    
    /**
     * The unit of measurement for the parameter.
     */
    private final String unit;
    
    /**
     * Whether the parameter is synthetic (derived from other parameters).
     */
    private final boolean synthetic;
    
    /**
     * Whether the parameter can be set (commandable).
     */
    private final boolean settable;
    
    /**
     * Whether the parameter is a user-defined parameter.
     */
    private final boolean userParameter;
    
    /**
     * The type of setter for settable parameters.
     */
    private final String setterType;
    
    /**
     * The default route for setter commands.
     */
    private final String setterDefaultRoute;
    
    /**
     * The list of expected raw values for the parameter.
     */
    private final List<Object> expectedRawValues;
    
    /**
     * The list of expected engineering values for the parameter.
     */
    private final List<Object> expectedEngineeringValues;

    /**
     * Constructor for a parameter descriptor.
     *
     * @param path the system entity path, cannot be null
     * @param externalId the external ID of the parameter
     * @param description the description of the parameter, can be null
     * @param rawDataType the raw data type, cannot be null
     * @param engineeringDataType the engineering data type, cannot be null
     * @param unit the unit of measurement, can be null
     * @param synthetic whether the parameter is synthetic
     * @param settable whether the parameter can be set
     * @param userParameter whether the parameter is user-defined
     * @param setterType the type of setter for settable parameters, can be null
     * @param setterDefaultRoute the default route for setter commands, can be null
     * @param expectedRawValues the list of expected raw values, can be null
     * @param expectedEngineeringValues the list of expected engineering values, can be null
     */
    public ParameterDescriptor(SystemEntityPath path, int externalId, String description, ValueTypeEnum rawDataType, ValueTypeEnum engineeringDataType, String unit, boolean synthetic, boolean settable, boolean userParameter, String setterType, String setterDefaultRoute, List<Object> expectedRawValues, List<Object> expectedEngineeringValues) {
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
        this.setterDefaultRoute = setterDefaultRoute;
        this.userParameter = userParameter;
    }

    /**
     * Returns the description of the parameter.
     *
     * @return the description (can be null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the raw data type of the parameter.
     *
     * @return the raw data type (cannot be null)
     */
    public ValueTypeEnum getRawDataType() {
        return rawDataType;
    }

    /**
     * Returns the engineering data type of the parameter (calibrated/processed).
     *
     * @return the engineering data type (cannot be null)
     */
    public ValueTypeEnum getEngineeringDataType() {
        return engineeringDataType;
    }

    /**
     * Returns the unit of measurement for the parameter.
     *
     * @return the unit (can be null)
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Returns whether the parameter is synthetic (derived from other parameters).
     *
     * @return true if the parameter is synthetic, false otherwise
     */
    public boolean isSynthetic() {
        return synthetic;
    }

    /**
     * Returns whether the parameter can be set (commandable).
     *
     * @return true if the parameter can be set, false otherwise
     */
    public boolean isSettable() {
        return settable;
    }

    /**
     * Returns whether the parameter is a user-defined parameter.
     *
     * @return true if the parameter is user-defined, false otherwise
     */
    public boolean isUserParameter() {
        return userParameter;
    }

    /**
     * Returns the list of expected raw values for the parameter.
     *
     * @return the list of expected raw values (can be null)
     */
    public List<Object> getExpectedRawValues() {
        return expectedRawValues;
    }

    /**
     * Returns the list of expected engineering values for the parameter.
     *
     * @return the list of expected engineering values (can be null)
     */
    public List<Object> getExpectedEngineeringValues() {
        return expectedEngineeringValues;
    }

    /**
     * Returns the type of setter for settable parameters.
     *
     * @return the setter type (can be null)
     */
    public String getSetterType() {
        return setterType;
    }

    /**
     * Returns the default route for setter commands.
     *
     * @return the default route (can be null)
     */
    public String getSetterDefaultRoute() {
        return setterDefaultRoute;
    }
}
