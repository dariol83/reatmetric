/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.definition.scripting.IParameterBinding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class MatcherDefinition {

    @XmlIDREF
    @XmlAttribute(name = "parameter", required = true)
    private ParameterProcessingDefinition parameter;

    @XmlAttribute(name = "operator", required = true)
    private MatcherType operator;

    @XmlAttribute(name = "valueType")
    private ValueTypeEnum valueType;

    @XmlAttribute(name = "value")
    private String value;

    @XmlIDREF
    @XmlAttribute(name = "reference")
    private ParameterProcessingDefinition reference;

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
        Object paramValue = param.value();
        Object compareValue = null;
        if(valueType != null && value != null) {
            // Construct the value
            compareValue = ValueUtil.parse(valueType, value);
        } else if(reference != null) {
            IParameterBinding ref = (IParameterBinding) resolver.resolve(reference.getId());
            compareValue = ref.value();
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
            throw new MatcherException("Provided values cannot be casted to Comparable, cannot compare");
        }
    }
}
