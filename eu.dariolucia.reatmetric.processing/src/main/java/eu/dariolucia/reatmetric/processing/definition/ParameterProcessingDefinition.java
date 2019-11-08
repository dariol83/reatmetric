/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.ccsds.encdec.definition.DataTypeEnum;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterProcessingDefinition {

    @XmlID
    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private String description = "";

    @XmlAttribute(required = true)
    private String location;

    @XmlAttribute(name = "raw_type",required = true)
    private DataTypeEnum rawType;

    @XmlAttribute(name = "eng_type", required = true)
    private DataTypeEnum engineeringType;

    @XmlAttribute
    private String unit;

    @XmlElement
    private ValidityDefinition validity;

    @XmlElementWrapper(name = "calibration")
    @XmlElements({
            @XmlElement(name="xy",type=XYCalibration.class),
            @XmlElement(name="poly",type=PolyCalibration.class),
            @XmlElement(name="log",type=LogCalibration.class),
            @XmlElement(name="enum",type=EnumCalibration.class),
            @XmlElement(name="expression",type=ExpressionCalibration.class),
            @XmlElement(name="external",type=ExternalCalibration.class),
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
    private List<CheckDefinition> checks;

    @XmlElement(name = "expression")
    private ExpressionDefinition expression;

    public ParameterProcessingDefinition() {
    }

    public ParameterProcessingDefinition(String id, String name, String description, String location, DataTypeEnum rawType, DataTypeEnum engineeringType, String unit, ValidityDefinition validity, CalibrationDefinition calibration, List<CheckDefinition> checks, ExpressionDefinition expression) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.rawType = rawType;
        this.engineeringType = engineeringType;
        this.unit = unit;
        this.validity = validity;
        this.calibration = calibration;
        this.checks = checks;
        this.expression = expression;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DataTypeEnum getRawType() {
        return rawType;
    }

    public void setRawType(DataTypeEnum rawType) {
        this.rawType = rawType;
    }

    public DataTypeEnum getEngineeringType() {
        return engineeringType;
    }

    public void setEngineeringType(DataTypeEnum engineeringType) {
        this.engineeringType = engineeringType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public ValidityDefinition getValidity() {
        return validity;
    }

    public void setValidity(ValidityDefinition validity) {
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
