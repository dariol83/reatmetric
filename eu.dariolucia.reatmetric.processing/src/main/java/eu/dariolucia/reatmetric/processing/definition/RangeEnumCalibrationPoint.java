/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class RangeEnumCalibrationPoint {

    @XmlAttribute(name = "min", required = true)
    private double minValue;

    @XmlAttribute(name = "min", required = true)
    private double maxValue;

    @XmlAttribute(name = "y", required = true)
    private String value;

    public RangeEnumCalibrationPoint() {
    }

    public RangeEnumCalibrationPoint(double minValue, double maxValue, String value) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = value;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean contains(Number valueToUse) {
        return valueToUse.doubleValue() <= maxValue && valueToUse.doubleValue() >= minValue;
    }
}
