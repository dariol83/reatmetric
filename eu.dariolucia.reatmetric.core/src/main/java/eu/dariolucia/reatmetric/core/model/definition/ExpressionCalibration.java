/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.model.definition;

import eu.dariolucia.reatmetric.core.model.impl.IParameterResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionCalibration extends CalibrationDefinition {

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
    public Object calibrate(Object valueToCalibrate, IParameterResolver resolver) {
        // TODO
        return valueToCalibrate;
    }
}
