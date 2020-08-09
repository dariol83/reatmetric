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
                // FIXME: (expression) decomment after investigation on memory footprint
                // Object o = this.condition.execute(resolver, null);
                Object o = true;
                if(!(o instanceof Boolean)) {
                    throw new ValidityException("Error while evaluating validity condition: expression returned a non-boolean value");
                }
                return (Boolean) o;
            // } catch (ScriptException e) {
            //     throw new ValidityException("Error while evaluating validity condition: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ValidityException("Error (unexpected) while evaluating validity condition: " + e.getMessage(), e);
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
