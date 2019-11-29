/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.ScriptEngine;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class CalibrationDefinition {

    public abstract Object calibrate(Object valueToCalibrate, ScriptEngine engine, IBindingResolver resolver) throws CalibrationException;

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
}
