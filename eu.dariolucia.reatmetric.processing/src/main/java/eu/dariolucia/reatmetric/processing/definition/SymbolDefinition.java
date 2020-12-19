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

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }

    public PropertyBinding getBinding() {
        return binding;
    }

    public void setBinding(PropertyBinding binding) {
        this.binding = binding;
    }
}
