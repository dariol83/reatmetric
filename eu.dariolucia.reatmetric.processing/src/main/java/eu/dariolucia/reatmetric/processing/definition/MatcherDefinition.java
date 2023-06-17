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
import eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class MatcherDefinition implements Serializable {

    @XmlIDREF
    @XmlAttribute(name = "parameter", required = true)
    private ParameterProcessingDefinition parameter;

    @XmlAttribute(name = "operator", required = true)
    private MatcherType operator;

    @XmlAttribute(name = "use_raw_value")
    private boolean useRawValue = false;

    @XmlAttribute(name = "value_type")
    private ValueTypeEnum valueType;

    @XmlAttribute(name = "value")
    private String value;

    @XmlIDREF
    @XmlAttribute(name = "reference")
    private ParameterProcessingDefinition reference;

    @XmlAttribute(name = "use_reference_raw_value")
    private boolean useReferenceRawValue = false;

    public MatcherDefinition() {
    }

    public MatcherDefinition(ParameterProcessingDefinition parameter, MatcherType operator, ValueTypeEnum valueType, String value) {
        this.parameter = parameter;
        this.operator = operator;
        this.valueType = valueType;
        this.value = value;
    }

    public MatcherDefinition(ParameterProcessingDefinition parameter, MatcherType operator, ParameterProcessingDefinition reference) {
        this.parameter = parameter;
        this.operator = operator;
        this.reference = reference;
    }

    public ParameterProcessingDefinition getParameter() {
        return parameter;
    }

    public void setParameter(ParameterProcessingDefinition parameter) {
        this.parameter = parameter;
    }

    public MatcherType getOperator() {
        return operator;
    }

    public void setOperator(MatcherType operator) {
        this.operator = operator;
    }

    public ValueTypeEnum getValueType() {
        return valueType;
    }

    public void setValueType(ValueTypeEnum valueType) {
        this.valueType = valueType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ParameterProcessingDefinition getReference() {
        return reference;
    }

    public void setReference(ParameterProcessingDefinition reference) {
        this.reference = reference;
    }

    public boolean isUseRawValue() {
        return useRawValue;
    }

    public void setUseRawValue(boolean useRawValue) {
        this.useRawValue = useRawValue;
    }

    public boolean isUseReferenceRawValue() {
        return useReferenceRawValue;
    }

    public void setUseReferenceRawValue(boolean useReferenceRawValue) {
        this.useReferenceRawValue = useReferenceRawValue;
    }

    /**
     * Execute the comparison between the parameter and either the provided value or the value of the referenced parameter.
     * This method does not take into consideration any validity or alarm state of the referenced parameter.
     *
     * @param resolver the entity resolver
     * @return true if the specified matching is fulfilled, otherwise false
     * @throws MatcherException in case of errors raised when evaluating this matcher
     */
    public boolean execute(IBindingResolver resolver) throws MatcherException {
        // Get the value of the parameter
        IParameterBinding param = (IParameterBinding) resolver.resolve(parameter.getId());
        Object paramValue = null;
        if(isUseRawValue()) {
            paramValue = param.rawValue();
        } else {
            paramValue = param.value();
        }
        // At this stage, if the value is null (no value at all), any comparison is meaningless, therefore we return an invalid state
        if(paramValue == null) {
            return false;
        }
        Object compareValue = null;
        if(valueType != null && value != null) {
            // Construct the value
            compareValue = ValueUtil.parse(valueType, value);
        } else if(reference != null) {
            IParameterBinding ref = (IParameterBinding) resolver.resolve(reference.getId());
            if(isUseReferenceRawValue()) {
                compareValue = ref.rawValue();
            } else {
                compareValue = ref.value();
            }
            // At this stage, if the value is null, then no way to compare
            if(compareValue == null) {
                return false;
            }
        } else {
            throw new MatcherException("Neither value nor reference attributes are set, cannot compare");
        }
        // If equality is needed, then go for it
        if(operator == MatcherType.EQUAL || operator == MatcherType.NOT_EQUAL) {
            // Watch out - type is not normalized
            boolean equals = Objects.equals(paramValue, compareValue);
            return (operator == MatcherType.EQUAL) == equals;
        }
        // If comparison is needed, then you can do it only if you cast the two objects as comparables
        if(compareValue instanceof Comparable && paramValue instanceof Comparable) {
            int result = ((Comparable) paramValue).compareTo(compareValue);
            if(result == 0) {
                return operator == MatcherType.GT_EQUAL || operator == MatcherType.LT_EQUAL;
            } else if(result < 0) {
                return operator == MatcherType.LT || operator == MatcherType.LT_EQUAL;
            } else {
                return operator == MatcherType.GT || operator == MatcherType.GT_EQUAL;
            }
        } else {
            // nulls are handled before
            throw new MatcherException("Provided values '" + paramValue + "' and '" + compareValue + "' cannot be casted to Comparable, cannot compare");
        }
    }
}
