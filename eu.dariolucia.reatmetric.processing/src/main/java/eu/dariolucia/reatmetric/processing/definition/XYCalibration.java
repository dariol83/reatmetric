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

@XmlAccessorType(XmlAccessType.FIELD)
public class XYCalibration extends CalibrationDefinition {

    @XmlElement(name = "point", required = true)
    private List<XYCalibrationPoint> points;

    @XmlAttribute
    private boolean extrapolate = false;

    public XYCalibration() {
    }

    public XYCalibration(List<XYCalibrationPoint> points, boolean extrapolate) {
        this.points = points;
        this.extrapolate = extrapolate;
    }

    public List<XYCalibrationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<XYCalibrationPoint> points) {
        this.points = points;
    }

    public boolean isExtrapolate() {
        return extrapolate;
    }

    public void setExtrapolate(boolean extrapolate) {
        this.extrapolate = extrapolate;
    }

    @Override
    public Object calibrate(Object valueToCalibrate, IParameterResolver resolver) {
        // TODO
        return valueToCalibrate;
    }
}
