/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.processing.impl.IParameterResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionCheck extends CheckDefinition{

    @XmlElement(required = true)
    private ExpressionDefinition definition;

    public ExpressionCheck() {
    }

    public ExpressionCheck(String name, CheckSeverity severity, int numViolations, ExpressionDefinition definition) {
        super(name, severity, numViolations);
        this.definition = definition;
    }

    public ExpressionDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ExpressionDefinition definition) {
        this.definition = definition;
    }


    @Override
    public AlarmState check(Object currentValue, int currentViolations, IParameterResolver resolver) {
        // TODO
        return AlarmState.NOMINAL;
    }
}
