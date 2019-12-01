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
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collections;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionCalibration extends CalibrationDefinition {

    public static final String INPUT_BINDING = "x";

    @XmlElement(required = true)
    private ExpressionDefinition definition;

    public ExpressionCalibration() {
    }

    public ExpressionCalibration(ExpressionDefinition definition) {
        this.definition = definition;
    }

    public ExpressionDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ExpressionDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        try {
            return definition.execute(resolver, Collections.singletonMap(INPUT_BINDING, valueToCalibrate));
        } catch (ScriptException e) {
            throw new CalibrationException("Cannot calibrate value " + valueToCalibrate + " using expression", e);
        }
    }
}
