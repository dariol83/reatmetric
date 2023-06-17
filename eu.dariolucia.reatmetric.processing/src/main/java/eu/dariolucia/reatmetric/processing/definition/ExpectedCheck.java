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
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpectedCheck extends CheckDefinition implements Serializable {

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
