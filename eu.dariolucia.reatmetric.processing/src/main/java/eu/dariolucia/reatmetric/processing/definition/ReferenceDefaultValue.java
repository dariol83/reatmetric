/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

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
}
