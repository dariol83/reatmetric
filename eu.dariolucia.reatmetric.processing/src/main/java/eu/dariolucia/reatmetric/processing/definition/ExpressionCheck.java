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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionCheck extends CheckDefinition implements Serializable {

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
            violated = (Boolean) definition.execute(resolver, Collections.singletonMap(INPUT_BINDING, currentValue), ValueTypeEnum.BOOLEAN);
        } catch (ScriptException e) {
            throw new CheckException("Cannot check value " + currentValue + " using expression", e);
        } catch (ClassCastException e) {
            throw new CheckException("Cannot check value " + currentValue + " using expression: wrong return type", e);
        } catch (Exception e) {
            throw new CheckException("Cannot (unexpected) check value " + currentValue + " using expression: unexpected exception", e);
        }
        // Return result
        return deriveState(violated, currentViolations);
    }

    @Override
    public void preload() throws Exception {
        definition.preload();
    }
}
