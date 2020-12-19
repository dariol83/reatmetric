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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class InvertedEnumCalibration extends CalibrationDefinition implements Serializable {

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
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver, ValueTypeEnum expectedOutput) throws CalibrationException {
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
