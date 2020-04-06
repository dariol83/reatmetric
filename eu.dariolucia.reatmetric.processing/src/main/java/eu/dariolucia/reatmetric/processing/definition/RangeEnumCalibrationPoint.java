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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class RangeEnumCalibrationPoint {

    @XmlAttribute(name = "min", required = true)
    private double minValue;

    @XmlAttribute(name = "max", required = true)
    private double maxValue;

    @XmlAttribute(name = "value", required = true)
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
