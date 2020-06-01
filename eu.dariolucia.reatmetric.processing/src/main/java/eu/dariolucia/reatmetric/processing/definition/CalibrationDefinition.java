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
import eu.dariolucia.reatmetric.api.value.ValueException;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CalibrationDefinition {

    @XmlElement(name = "applicability")
    private ValidityCondition applicability = null;

    public ValidityCondition getApplicability() {
        return applicability;
    }

    public void setApplicability(ValidityCondition applicability) {
        this.applicability = applicability;
    }

    public abstract Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException;

    protected final double convertToDouble(Object valueToCheck) throws CalibrationException {
        if(valueToCheck instanceof Number) {
            return ((Number) valueToCheck).doubleValue();
        } else {
            throw new CalibrationException("Cannot check " + valueToCheck + " as input to limit check: value cannot be converted to double");
        }
    }

    protected final long convertToLong(Object valueToCalibrate) throws CalibrationException {
        if(valueToCalibrate instanceof Number) {
            return ((Number) valueToCalibrate).longValue();
        } else {
            throw new CalibrationException("Cannot calibrate " + valueToCalibrate + " as input to enumeration calibration: value cannot be converted to long");
        }
    }

    public static Object performCalibration(List<CalibrationDefinition> definitions, Object inputValue, ValueTypeEnum outputType, IBindingResolver resolver) throws CalibrationException {
        // If the value is null, then set to null (no value)
        if(inputValue == null) {
            return null;
        }
        Object result = inputValue;
        // Otherwise, calibrate it
        if(definitions != null && !definitions.isEmpty()) {
            boolean calibrated = false;
            for(CalibrationDefinition cd : definitions) {
                try {
                    if (cd.getApplicability() == null || cd.getApplicability().execute(resolver)) {
                        result = cd.calibrate(inputValue, resolver);
                        calibrated = true;
                        break;
                    }
                } catch (ValidityException e) {
                    throw new CalibrationException("Applicability of calibration raised error: " + e.getMessage(), e);
                }
            }
            if(!calibrated) {
                // Potential problem: return null
                throw new CalibrationException("No applicable calibration found to calibrate value " + inputValue + " to output type " + outputType);
            }
        }
        return sanitize(outputType, result);
    }

    public static Object performDecalibration(CalibrationDefinition definition, Object inputValue, ValueTypeEnum outputType, IBindingResolver resolver) throws CalibrationException {
        // If the value is null, then set to null (no value)
        if(inputValue == null) {
            return null;
        }
        Object result = inputValue;
        // Otherwise, calibrate it
        if(definition != null) {
            result = definition.calibrate(inputValue, resolver);
        }
        return sanitize(outputType, result);
    }

    private static Object sanitize(ValueTypeEnum outputType, Object result) throws CalibrationException {
        // Sanitize, if possible, based on type
        try {
            result = ValueUtil.convert(result, outputType);
        } catch (ValueException ve) {
            throw new CalibrationException("Conversion of value " + result + " to output type " + outputType + " failed: " + ve.getMessage(), ve);
        }
        // Return the result
        return result;
    }
}
