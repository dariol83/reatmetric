/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.impl.IParameterResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class EnumCalibration extends CalibrationDefinition {

    @XmlAttribute
    private String defaultValue;

    @XmlElement(name = "point", required = true)
    private List<EnumCalibrationPoint> points;

    public EnumCalibration() {
    }

    public EnumCalibration(String defaultValue, List<EnumCalibrationPoint> points) {
        this.defaultValue = defaultValue;
        this.points = points;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<EnumCalibrationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<EnumCalibrationPoint> points) {
        this.points = points;
    }

    @Override
    public Object calibrate(Object valueToCalibrate, IParameterResolver resolver) {
        // TODO
        return Objects.toString(valueToCalibrate, "null");
    }
}
