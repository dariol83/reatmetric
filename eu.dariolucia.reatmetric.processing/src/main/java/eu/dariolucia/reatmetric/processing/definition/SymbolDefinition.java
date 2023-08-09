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

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

/**
 * This class is used to map a name, appearing in expressions, to a system entity and a related property, whose value is
 * then used during expression evaluation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolDefinition implements Serializable {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "reference", required = true)
    @XmlJavaTypeAdapter(IntToStringAdapter.class)
    private Integer reference;

    @XmlAttribute(name = "binding")
    private PropertyBinding binding = PropertyBinding.OBJECT;

    public SymbolDefinition() {
    }

    public SymbolDefinition(String name, Integer reference, PropertyBinding binding) {
        this.name = name;
        this.reference = reference;
        this.binding = binding;
    }

    /**
     * The name of the symbol, as it appears in the expression.
     * <p></p>
     * Attribute: name (mandatory)
     *
     * @return the name of the symbol
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The ID, {@link AbstractProcessingDefinition#getId()}, of the system element, the symbol refers to.
     * <p></p>
     * Attribute: reference (mandatory)
     *
     * @return the ID of the system element, the symbol refers to
     */
    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    /**
     * The property of the referenced system element, whose value must be used in the evaluation of the expression.
     * <p></p>
     * Attribute: binding (default: {@link PropertyBinding#OBJECT})
     *
     * @return the property of the referenced system element
     */
    public PropertyBinding getBinding() {
        return binding;
    }

    public void setBinding(PropertyBinding binding) {
        this.binding = binding;
    }
}
