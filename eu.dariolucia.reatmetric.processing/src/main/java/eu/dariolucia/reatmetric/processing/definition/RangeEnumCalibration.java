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

import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@XmlAccessorType(XmlAccessType.FIELD)
public class RangeEnumCalibration extends CalibrationDefinition implements Serializable {

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
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver, ValueTypeEnum expectedOutput) throws CalibrationException {
        // If the valueToCalibrate can become an integer number somehow, then calibrate, otherwise error
        // To support a very old UNIX-way to process things, if the input is a boolean, then FALSE -> 0 and TRUE -> 1
        Number valueToUse;
        if(valueToCalibrate instanceof Boolean) {
            valueToUse = Boolean.TRUE.equals(valueToCalibrate) ? 1 : 0;
        } else {
            valueToUse = (Number) valueToCalibrate;
        }
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
