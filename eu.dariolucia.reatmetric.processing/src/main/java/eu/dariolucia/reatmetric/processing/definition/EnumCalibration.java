/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.IDataItemStateResolver;
import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.ScriptEngine;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Map<Long, String> point2values = new HashMap<>();

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        // If the valueToCalibrate can become an integer number somehow, then calibrate, otherwise error
        long valueToUse = convertToLong(valueToCalibrate);
        if(point2values.isEmpty()) {
            for(EnumCalibrationPoint p : points) {
                point2values.put(p.getInput(), p.getValue());
            }
        }
        String calibratedValue =  point2values.getOrDefault(valueToUse, defaultValue);
        if(defaultValue == null && calibratedValue == null) {
            // Not found and no default: calibration failed
            throw new CalibrationException("Cannot calibrate " + valueToCalibrate + " using enumeration calibration: no correspondence found and no default value defined");
        } else {
            return calibratedValue;
        }
    }
}
