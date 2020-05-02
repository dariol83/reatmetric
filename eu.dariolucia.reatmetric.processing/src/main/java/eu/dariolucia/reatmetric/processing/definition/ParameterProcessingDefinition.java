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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import javax.xml.bind.annotation.*;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterProcessingDefinition extends AbstractProcessingDefinition {

    @XmlAttribute(name = "raw_type",required = true)
    private ValueTypeEnum rawType;

    @XmlAttribute(name = "eng_type", required = true)
    private ValueTypeEnum engineeringType;

    @XmlAttribute(name = "eng_unit")
    private String unit = "";

    @XmlElement(name = "validity")
    private ValidityCondition validity;

    @XmlElement(name = "synthetic")
    private ExpressionDefinition expression;

    @XmlElement(name = "default_value")
    private FixedDefaultValue defaultValue = null;

    @XmlElements({
            @XmlElement(name="calib_xy",type=XYCalibration.class),
            @XmlElement(name="calib_poly",type=PolyCalibration.class),
            @XmlElement(name="calib_log",type=LogCalibration.class),
            @XmlElement(name="calib_enum",type=EnumCalibration.class),
            @XmlElement(name="calib_range_enum",type=RangeEnumCalibration.class),
            @XmlElement(name="calib_expression",type=ExpressionCalibration.class),
            @XmlElement(name="calib_external",type=ExternalCalibration.class),
    })
    private List<CalibrationDefinition> calibrations;

    @XmlElementWrapper(name = "checks")
    @XmlElements({
            @XmlElement(name="limit",type=LimitCheck.class),
            @XmlElement(name="expected",type=ExpectedCheck.class),
            @XmlElement(name="delta",type=DeltaCheck.class),
            @XmlElement(name="expression",type=ExpressionCheck.class),
            @XmlElement(name="external",type=ExternalCheck.class),
    })
    private List<CheckDefinition> checks = new LinkedList<>();

    @XmlElement(name="setter")
    private ParameterSetterDefinition setter;

    @XmlElementWrapper(name = "triggers")
    @XmlElement(name="trigger")
    private List<ParameterTriggerDefinition> triggers = new LinkedList<>();

    public ValueTypeEnum getRawType() {
        return rawType;
    }

    public void setRawType(ValueTypeEnum rawType) {
        this.rawType = rawType;
    }

    public ValueTypeEnum getEngineeringType() {
        return engineeringType;
    }

    public void setEngineeringType(ValueTypeEnum engineeringType) {
        this.engineeringType = engineeringType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ValidityCondition getValidity() {
        return validity;
    }

    public void setValidity(ValidityCondition validity) {
        this.validity = validity;
    }

    public List<CalibrationDefinition> getCalibrations() {
        return calibrations;
    }

    public void setCalibrations(List<CalibrationDefinition> calibrations) {
        this.calibrations = calibrations;
    }

    public List<CheckDefinition> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckDefinition> checks) {
        this.checks = checks;
    }

    public ExpressionDefinition getExpression() {
        return expression;
    }

    public void setExpression(ExpressionDefinition expression) {
        this.expression = expression;
    }

    public List<ParameterTriggerDefinition> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<ParameterTriggerDefinition> triggers) {
        this.triggers = triggers;
    }

    public FixedDefaultValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(FixedDefaultValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ParameterSetterDefinition getSetter() {
        return setter;
    }

    public void setSetter(ParameterSetterDefinition setter) {
        this.setter = setter;
    }
}
