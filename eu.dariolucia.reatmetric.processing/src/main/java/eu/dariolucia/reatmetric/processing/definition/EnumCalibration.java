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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class EnumCalibration extends CalibrationDefinition implements Serializable {

    @XmlAttribute(name = "default")
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
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver, ValueTypeEnum expectedOutput) throws CalibrationException {
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

    public InvertedEnumCalibration buildInvertedEnum() {
        InvertedEnumCalibration toReturn = new InvertedEnumCalibration();
        toReturn.setPoints(new ArrayList<>(points.size()));
        for(EnumCalibrationPoint point : points) {
            toReturn.getPoints().add(new InvertedEnumCalibrationPoint(point.getValue(), point.getInput()));
        }
        return toReturn;
    }
}
