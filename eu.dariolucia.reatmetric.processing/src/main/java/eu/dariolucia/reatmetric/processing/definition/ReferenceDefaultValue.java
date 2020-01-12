/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

@XmlAccessorType(XmlAccessType.FIELD)
public class ReferenceDefaultValue extends AbstractDefaultValue {

    @XmlIDREF
    @XmlAttribute(name = "parameter", required = true)
    private ParameterProcessingDefinition parameter;

    @XmlAttribute(name = "targetValueType", required = true)
    private DefaultValueType targetValueType;

    public ReferenceDefaultValue() {
    }

    public ReferenceDefaultValue(DefaultValueType type, ParameterProcessingDefinition parameter, DefaultValueType targetValueType) {
        super(type);
        this.parameter = parameter;
        this.targetValueType = targetValueType;
    }

    public ParameterProcessingDefinition getParameter() {
        return parameter;
    }

    public void setParameter(ParameterProcessingDefinition parameter) {
        this.parameter = parameter;
    }

    public DefaultValueType getTargetValueType() {
        return targetValueType;
    }

    public void setTargetValueType(DefaultValueType targetValueType) {
        this.targetValueType = targetValueType;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    public Object readTargetValue(String argumentName, IBindingResolver resolver) throws ValueReferenceException {
        if(this.parameter == null) {
            throw new ValueReferenceException("Argument " + argumentName + " points to a non-existing parameter");
        }
        IParameterBinding entity = (IParameterBinding) resolver.resolve(this.parameter.getId());
        if(targetValueType == DefaultValueType.RAW) {
            return entity.rawValue();
        } else if(targetValueType == DefaultValueType.ENGINEERING) {
            return entity.value();
        } else {
            throw new ValueReferenceException("Default value of argument " + argumentName + " has undefined value target type: " + targetValueType);
        }
    }
}
