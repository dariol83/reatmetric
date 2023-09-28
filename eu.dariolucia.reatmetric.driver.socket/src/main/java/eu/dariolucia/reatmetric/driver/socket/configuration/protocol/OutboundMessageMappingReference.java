/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.socket.configuration.protocol;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;

import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class OutboundMessageMappingReference {

    @XmlIDREF
    @XmlAttribute(name="outbound-mapping", required = true)
    private OutboundMessageMapping outboundMapping;

    @XmlAttribute
    private String argument;

    @XmlAttribute
    private String value;

    @XmlAttribute
    private ValueTypeEnum type;

    public OutboundMessageMapping getOutboundMapping() {
        return outboundMapping;
    }

    public void setOutboundMapping(OutboundMessageMapping outboundMapping) {
        this.outboundMapping = outboundMapping;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private volatile Object parsedValue;

    public boolean match(CommandTracker lastCommand) {
        if(lastCommand == null) {
            return false;
        }
        if(Objects.equals(lastCommand.getMapping(), getOutboundMapping())) {
            if(getArgument() != null) {
                Object commandValue = lastCommand.getEncodedCommand().getSecond().get(getArgument());
                Object valueToCheck = parseValue();
                return ValueUtil.compare(commandValue, valueToCheck) == 0;
            } else {
                // Command matches, no argument, match
                return true;
            }
        } else {
            return false;
        }
    }

    private Object parseValue() {
     if(parsedValue == null && getValue() != null) {
            parsedValue = getType().parse(getValue());
        }
        return parsedValue;
    }
}
