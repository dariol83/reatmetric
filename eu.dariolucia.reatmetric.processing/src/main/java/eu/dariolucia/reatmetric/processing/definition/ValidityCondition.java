/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ValidityCondition {

    @XmlElement(name = "condition")
    private ExpressionDefinition condition;

    @XmlElement(name= "match")
    private MatcherDefinition match;

    public ValidityCondition(ExpressionDefinition condition) {
        this.condition = condition;
    }

    public ValidityCondition(MatcherDefinition match) {
        this.match = match;
    }

    public ValidityCondition() {
    }

    public ExpressionDefinition getCondition() {
        return condition;
    }

    public void setCondition(ExpressionDefinition condition) {
        this.condition = condition;
    }

    public MatcherDefinition getMatch() {
        return match;
    }

    public void setMatch(MatcherDefinition match) {
        this.match = match;
    }

    public boolean execute(IBindingResolver resolver) throws ValidityException {
        if(condition != null) {
            try {
                Object o = this.condition.execute(resolver, null);
                if(!(o instanceof Boolean)) {
                    throw new ValidityException("Error while evaluating validity condition: expression returned a non-boolean value");
                }
                return (Boolean) o;
            } catch (ScriptException e) {
                throw new ValidityException("Error while evaluating validity condition: " + e.getMessage(), e);
            }
        } else if(match != null) {
            try {
                return match.execute(resolver);
            } catch (Exception e) {
                throw new ValidityException("Error while evaluating validity matcher: " + e.getMessage(), e);
            }
        } else {
            // No validity means valid
            return true;
        }
    }
}
