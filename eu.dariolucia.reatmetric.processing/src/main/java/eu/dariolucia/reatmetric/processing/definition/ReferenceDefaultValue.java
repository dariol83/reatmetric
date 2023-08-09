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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;

/**
 * This class is used to provide a default value to be used for initialisation of activity
 * arguments, by reading it from the provided parameter.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ReferenceDefaultValue extends AbstractDefaultValue implements Serializable {

    @XmlIDREF
    @XmlAttribute(name = "parameter", required = true)
    private ParameterProcessingDefinition parameter;

    @XmlAttribute(name = "target_value_type", required = true)
    private DefaultValueType targetValueType;

    public ReferenceDefaultValue() {
    }

    public ReferenceDefaultValue(DefaultValueType type, ParameterProcessingDefinition parameter, DefaultValueType targetValueType) {
        super(type);
        this.parameter = parameter;
        this.targetValueType = targetValueType;
    }

    /**
     * The {@link ParameterProcessingDefinition} from which the value must be read.
     * <p></p>
     * Attribute: parameter (mandatory)
     *
     * @return the {@link ParameterProcessingDefinition} from which the value must be read
     */
    public ParameterProcessingDefinition getParameter() {
        return parameter;
    }

    public void setParameter(ParameterProcessingDefinition parameter) {
        this.parameter = parameter;
    }

    /**
     * The target parameter value to be read: raw or engineering
     * <p></p>
     * Attribute: target_value_type (mandatory)
     *
     * @return the {@link DefaultValueType} to read from the target parameter
     */
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
