/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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

    @XmlElements({
            @XmlElement(name="calib_xy",type=XYCalibration.class),
            @XmlElement(name="calib_poly",type=PolyCalibration.class),
            @XmlElement(name="calib_log",type=LogCalibration.class),
            @XmlElement(name="calib_enum",type=EnumCalibration.class),
            @XmlElement(name="calib_expression",type=ExpressionCalibration.class),
            @XmlElement(name="calib_external",type=ExternalCalibration.class),
    })
    private CalibrationDefinition calibration;

    @XmlElementWrapper(name = "checks")
    @XmlElements({
            @XmlElement(name="limit",type=LimitCheck.class),
            @XmlElement(name="expected",type=ExpectedCheck.class),
            @XmlElement(name="delta",type=DeltaCheck.class),
            @XmlElement(name="expression",type=ExpressionCheck.class),
            @XmlElement(name="external",type=ExternalCheck.class),
    })
    private List<CheckDefinition> checks = new LinkedList<>();

    public ParameterProcessingDefinition() {
        super();
    }

    public ParameterProcessingDefinition(int id, String description, String location, ValueTypeEnum rawType, ValueTypeEnum engineeringType, String unit, ValidityCondition validity, CalibrationDefinition calibration, List<CheckDefinition> checks, ExpressionDefinition expression) {
        super(id, description, location);
        this.rawType = rawType;
        this.engineeringType = engineeringType;
        this.unit = unit;
        this.validity = validity;
        this.calibration = calibration;
        this.checks = checks;
        this.expression = expression;
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

    public ValidityCondition getValidity() {
        return validity;
    }

    public void setValidity(ValidityCondition validity) {
        this.validity = validity;
    }

    public CalibrationDefinition getCalibration() {
        return calibration;
    }

    public void setCalibration(CalibrationDefinition calibration) {
        this.calibration = calibration;
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
}
