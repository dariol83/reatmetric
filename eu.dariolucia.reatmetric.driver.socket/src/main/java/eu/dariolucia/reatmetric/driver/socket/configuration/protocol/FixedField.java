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
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * This class indicates the name of the field in the {@link eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition} that
 * this protocol/route will hardcode to the value defined in this object, if such value is not previously mapped/provided.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FixedField {

    @XmlAttribute(required = true)
    private String field;

    @XmlAttribute(required = true)
    private String value = null;

    @XmlAttribute(required = true)
    private ValueTypeEnum type;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
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

    @XmlTransient
    private Object cachedValue = null;

    public Object buildObjectValue() {
        if(cachedValue == null) {
            cachedValue = ValueUtil.parse(getType(), getValue());
        }
        return cachedValue;
    }

}
