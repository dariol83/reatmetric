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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

/**
 * This class is used to provide a fixed value (to be used for initialisation of parameters and activity arguments).
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FixedDefaultValue extends AbstractDefaultValue implements Serializable {

    @XmlAttribute(name = "value", required = true)
    private String value;

    public FixedDefaultValue() {
    }

    public FixedDefaultValue(DefaultValueType type, String value) {
        super(type);
        this.value = value;
    }

    /**
     * The value as string literal (to be interpreted depending on the {@link DefaultValueType} specified.
     * <p></p>
     * Attribute: value (mandatory)
     *
     * @return the value as string literal
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
