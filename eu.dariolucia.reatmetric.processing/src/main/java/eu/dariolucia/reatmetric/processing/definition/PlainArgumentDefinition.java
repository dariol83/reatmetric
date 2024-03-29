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

import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class PlainArgumentDefinition extends AbstractArgumentDefinition implements Serializable {

    @XmlAttribute(name = "raw_type", required = true)
    private ValueTypeEnum rawType;

    @XmlAttribute(name = "eng_type", required = true)
    private ValueTypeEnum engineeringType;

    @XmlAttribute(name = "eng_unit")
    private String unit = "";

    @XmlAttribute(name = "fixed")
    private boolean fixed = false;

    @XmlElements({
            @XmlElement(name="default_fixed",type=FixedDefaultValue.class),
            @XmlElement(name="default_ref",type=ReferenceDefaultValue.class)
    })
    private AbstractDefaultValue defaultValue;

    /**
     * The decalibration converts the activity argument's engineering value into the corresponding raw value. If no
     * calibration is specified, the engineering value is set as raw value.
     */
    @XmlElements({
            @XmlElement(name="decalib_xy",type=XYCalibration.class),
            @XmlElement(name="decalib_poly",type=PolyCalibration.class),
            @XmlElement(name="decalib_log",type=LogCalibration.class),
            @XmlElement(name="decalib_enum",type=EnumCalibration.class),
            @XmlElement(name="decalib_range_enum",type=RangeEnumCalibration.class),
            @XmlElement(name="decalib_ienum",type=InvertedEnumCalibration.class),
            @XmlElement(name="decalib_expression",type=ExpressionCalibration.class),
            @XmlElement(name="decalib_external",type=ExternalCalibration.class),
    })
    private CalibrationDefinition decalibration;

    /**
     * The list of checks applied to the <b>raw value</b> of the activity argument. In this case, there is no need to specify
     * severity and number of violations. Checks are specified in an AND fashion: all applicable checks must be fulfilled, in
     * order to verify the argument.
     *
     */
    @XmlElementWrapper(name = "checks")
    @XmlElements({
            @XmlElement(name="limit",type=LimitCheck.class),
            @XmlElement(name="expected",type=ExpectedCheck.class),
            @XmlElement(name="expression",type=ExpressionCheck.class),
            @XmlElement(name="external",type=ExternalCheck.class),
    })
    private List<CheckDefinition> checks = new LinkedList<>();

    public PlainArgumentDefinition() {
    }

    public PlainArgumentDefinition(String name, ValueTypeEnum rawType, ValueTypeEnum engineeringType, String unit, boolean fixed, AbstractDefaultValue defaultValue, CalibrationDefinition decalibration, List<CheckDefinition> checks) {
        super(name, "");
        this.rawType = rawType;
        this.engineeringType = engineeringType;
        this.unit = unit;
        this.fixed = fixed;
        this.defaultValue = defaultValue;
        this.decalibration = decalibration;
        this.checks = checks;
    }

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

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public AbstractDefaultValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(AbstractDefaultValue defaultValue) {
        this.defaultValue = defaultValue;
    }

    public CalibrationDefinition getDecalibration() {
        return decalibration;
    }

    public void setDecalibration(CalibrationDefinition decalibration) {
        this.decalibration = decalibration;
    }

    public List<CheckDefinition> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckDefinition> checks) {
        this.checks = checks;
    }

    public List<Object> buildExpectedValuesRaw() {
        if(getDecalibration() != null) {
            if(getDecalibration() instanceof InvertedEnumCalibration) {
                InvertedEnumCalibration iec = (InvertedEnumCalibration) getDecalibration();
                List<Object> rawValues = new LinkedList<>();
                for(InvertedEnumCalibrationPoint p : iec.getPoints()) {
                    Object valueToAdd = p.getValue();
                    if(getRawType() == ValueTypeEnum.ENUMERATED) {
                        valueToAdd = ((Long) valueToAdd).intValue();
                    }
                    rawValues.add(valueToAdd);
                }
                if(iec.getDefaultValue() != null) {
                    Object valueToAdd = iec.getDefaultValue();
                    if(getRawType() == ValueTypeEnum.ENUMERATED) {
                        valueToAdd = (iec.getDefaultValue()).intValue();
                    }
                    if(!rawValues.contains(valueToAdd)) {
                        rawValues.add(valueToAdd);
                    }
                }
                return rawValues;
            }
            if(getDecalibration() instanceof EnumCalibration) {
                EnumCalibration iec = (EnumCalibration) getDecalibration();
                List<Object> rawValues = new LinkedList<>();
                for(EnumCalibrationPoint p : iec.getPoints()) {
                    rawValues.add(p.getValue());
                }
                if(iec.getDefaultValue() != null) {
                    if(!rawValues.contains(iec.getDefaultValue())) {
                        rawValues.add(iec.getDefaultValue());
                    }
                }
                return rawValues;
            }
        }
        return null;
    }

    public List<Object> buildExpectedValuesEng() {
        if(getDecalibration() != null) {
            if(getDecalibration() instanceof InvertedEnumCalibration) {
                InvertedEnumCalibration iec = (InvertedEnumCalibration) getDecalibration();
                List<Object> engValues = new LinkedList<>();
                for(InvertedEnumCalibrationPoint p : iec.getPoints()) {
                    engValues.add(p.getInput());
                }
                return engValues;
            }
            if(getDecalibration() instanceof EnumCalibration) {
                EnumCalibration iec = (EnumCalibration) getDecalibration();
                List<Object> engValues = new LinkedList<>();
                for(EnumCalibrationPoint p : iec.getPoints()) {
                    Object valueToAdd =  p.getInput();
                    if(getEngineeringType() == ValueTypeEnum.ENUMERATED) {
                        valueToAdd = ((Long)  p.getInput()).intValue();
                    }
                    engValues.add(valueToAdd);
                }
                return engValues;
            }
        }
        return null;
    }

}
