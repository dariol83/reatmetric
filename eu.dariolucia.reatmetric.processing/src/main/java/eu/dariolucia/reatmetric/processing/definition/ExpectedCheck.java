/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.ccsds.encdec.definition.DataTypeEnum;
import eu.dariolucia.reatmetric.api.common.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.processing.impl.IParameterResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpectedCheck extends CheckDefinition {

    @XmlAttribute(required = true)
    private ValueTypeEnum type;

    @XmlElement(name = "value", required = true)
    private List<String> expectedValues;

    public ExpectedCheck() {
    }

    public ExpectedCheck(String name, CheckSeverity severity, int numViolations, ValueTypeEnum type, List<String> expectedValues) {
        super(name, severity, numViolations);
        this.type = type;
        this.expectedValues = expectedValues;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    public List<String> getExpectedValues() {
        return expectedValues;
    }

    public void setExpectedValues(List<String> expectedValues) {
        this.expectedValues = expectedValues;
    }


    @Override
    public AlarmState check(Object currentValue, int currentViolations, IParameterResolver resolver) {
        // TODO
        return AlarmState.NOMINAL;
    }
}
