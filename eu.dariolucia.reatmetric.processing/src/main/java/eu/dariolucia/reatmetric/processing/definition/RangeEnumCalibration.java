/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class RangeEnumCalibration extends CalibrationDefinition {

    @XmlAttribute(name = "default")
    private String defaultValue;

    @XmlElement(name = "point", required = true)
    private List<RangeEnumCalibrationPoint> points;

    public RangeEnumCalibration() {
    }

    public RangeEnumCalibration(String defaultValue, List<RangeEnumCalibrationPoint> points) {
        this.defaultValue = defaultValue;
        this.points = points;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<RangeEnumCalibrationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<RangeEnumCalibrationPoint> points) {
        this.points = points;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        // If the valueToCalibrate can become an integer number somehow, then calibrate, otherwise error
        Number valueToUse = (Number) valueToCalibrate;
        if(valueToCalibrate == null) {
            throw new CalibrationException("Cannot calibrate a null value using range enumeration calibration");
        }
        Optional<String> calibratedValue =  points.stream().filter(o -> o.contains(valueToUse)).map(RangeEnumCalibrationPoint::getValue).findFirst();
        if(defaultValue == null && calibratedValue.isEmpty()) {
            // Not found and no default: calibration failed
            throw new CalibrationException("Cannot calibrate " + valueToCalibrate + " using range enumeration calibration: no correspondence found and no default value defined");
        } else {
            return calibratedValue.orElse(defaultValue);
        }
    }
}
