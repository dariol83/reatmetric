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
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalCalibration extends CalibrationDefinition {

    @XmlAttribute(required = true)
    private String function;

    public ExternalCalibration() {
    }

    public ExternalCalibration(String function) {
        this.function = function;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    @Override
    public Object calibrate(Object valueToCalibrate, IParameterResolver resolver) {
        // TODO
        return valueToCalibrate;
    }
}
