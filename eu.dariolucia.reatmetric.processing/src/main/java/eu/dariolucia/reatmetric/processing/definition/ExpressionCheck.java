/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.IDataItemStateResolver;
import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionCheck extends CheckDefinition{

    public static final String INPUT_BINDING = "input";

    @XmlElement(name = "definition", required = true)
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
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) throws CheckException {
        // Prepare transient state
        prepareMapping();
        // Check
        boolean violated;
        try {
            violated = (Boolean) definition.execute(resolver, Collections.singletonMap(INPUT_BINDING, currentValue));
        } catch (ScriptException e) {
            throw new CheckException("Cannot check value " + currentValue + " using expression", e);
        } catch (ClassCastException e) {
            throw new CheckException("Cannot check value " + currentValue + " using expression: wrong return type", e);
        }
        // Return result
        return deriveState(violated, currentViolations);
    }
}
