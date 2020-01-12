/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Set<Object> values = new HashSet<>();

    @Override
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) {
        // Prepare transient state
        prepareMapping();
        // Other transients
        if(values.isEmpty()) {
            for(String s : expectedValues) {
                values.add(ValueUtil.parse(type, s));
            }
        }
        // Check
        boolean violated = !values.contains(currentValue);
        // Return result
        return deriveState(violated, currentViolations);
    }
}
