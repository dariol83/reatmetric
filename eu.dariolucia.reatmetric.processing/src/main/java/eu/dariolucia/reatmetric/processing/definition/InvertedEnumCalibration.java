/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class InvertedEnumCalibration extends CalibrationDefinition {

    @XmlAttribute(name = "default")
    private Long defaultValue;

    @XmlElement(name = "point", required = true)
    private List<InvertedEnumCalibrationPoint> points;

    public InvertedEnumCalibration() {
    }

    public InvertedEnumCalibration(Long defaultValue, List<InvertedEnumCalibrationPoint> points) {
        this.defaultValue = defaultValue;
        this.points = points;
    }

    public Long getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Long defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<InvertedEnumCalibrationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<InvertedEnumCalibrationPoint> points) {
        this.points = points;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Map<String, Long> point2values = new HashMap<>();

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        // If the valueToCalibrate can become an integer number somehow, then calibrate, otherwise error
        if(point2values.isEmpty()) {
            for(InvertedEnumCalibrationPoint p : points) {
                point2values.put(p.getInput(), p.getValue());
            }
        }
        Long calibratedValue =  point2values.getOrDefault((String) valueToCalibrate, defaultValue);
        if(defaultValue == null && calibratedValue == null) {
            // Not found and no default: calibration failed
            throw new CalibrationException("Cannot calibrate " + valueToCalibrate + " using inverted enumeration calibration: no correspondence found and no default value defined");
        } else {
            return calibratedValue;
        }
    }
}
