/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import javax.xml.bind.annotation.*;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ArgumentDefinition {

    @XmlAttribute(name = "name", required = true)
    private String name = "";

    @XmlAttribute(name = "raw_type",required = true)
    private ValueTypeEnum rawType;

    @XmlAttribute(name = "eng_type", required = true)
    private ValueTypeEnum engineeringType;

    @XmlAttribute(name = "eng_unit")
    private String unit = "";

    @XmlAttribute(name = "fixed")
    private boolean fixed;

    @XmlElements({
            @XmlElement(name="default_fixed",type=FixedDefaultValue.class),
            @XmlElement(name="default_ref",type=ReferenceDefaultValue.class)
    })
    private AbstractDefaultValue defaultValue;

    /**
     * The decalibration converts the activity argument's engineering value into the corresponding raw value. If no
     * calibration is specified, the engineering value is set as source value.
     */
    @XmlElements({
            @XmlElement(name="decalib_xy",type=XYCalibration.class),
            @XmlElement(name="decalib_poly",type=PolyCalibration.class),
            @XmlElement(name="decalib_log",type=LogCalibration.class),
            @XmlElement(name="decalib_enum",type=EnumCalibration.class),
            @XmlElement(name="decalib_ienum",type=InvertedEnumCalibration.class),
            @XmlElement(name="decalib_expression",type=ExpressionCalibration.class),
            @XmlElement(name="decalib_external",type=ExternalCalibration.class),
    })
    private CalibrationDefinition decalibration;

    /**
     * The list of checks applied to the <b>raw value</b> of the activity argument. In this case, there is no need to specify
     * severity and number of violations.
     */
    @XmlElementWrapper(name = "checks")
    @XmlElements({
            @XmlElement(name="limit",type=LimitCheck.class),
            @XmlElement(name="expected",type=ExpectedCheck.class),
            @XmlElement(name="expression",type=ExpressionCheck.class),
            @XmlElement(name="external",type=ExternalCheck.class),
    })
    private List<CheckDefinition> checks = new LinkedList<>();

    public ArgumentDefinition() {
    }

    public ArgumentDefinition(String name, ValueTypeEnum rawType, ValueTypeEnum engineeringType, String unit, boolean fixed, AbstractDefaultValue defaultValue, CalibrationDefinition decalibration, List<CheckDefinition> checks) {
        this.name = name;
        this.rawType = rawType;
        this.engineeringType = engineeringType;
        this.unit = unit;
        this.fixed = fixed;
        this.defaultValue = defaultValue;
        this.decalibration = decalibration;
        this.checks = checks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
