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

package eu.dariolucia.reatmetric.scheduler.definition;

import eu.dariolucia.reatmetric.api.processing.input.AbstractActivityArgument;
import eu.dariolucia.reatmetric.api.processing.input.PlainActivityArgument;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class PlainArgumentInvocationDefinition extends AbstractArgumentInvocationDefinition implements Serializable {

    @XmlAttribute(name = "value")
    private String value;

    @XmlAttribute(name = "raw-value")
    private boolean rawValue = false;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRawValue() {
        return rawValue;
    }

    public void setRawValue(boolean rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public AbstractActivityArgument build() {
        return rawValue ? PlainActivityArgument.ofSource(getName(), value) : PlainActivityArgument.ofEngineering(getName(), value);
    }
}
