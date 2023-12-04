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
import jakarta.xml.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventCondition {

    @XmlAttribute(required = true)
    private String field;

    @XmlAttribute(required = true)
    private String value;

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
    private Object valueObject = null;

    public boolean check(Map<String, Object> decodedMessage) {
        if(this.valueObject == null) {
            this.valueObject = ValueUtil.parse(getType(), getValue());
        }
        return Objects.equals(decodedMessage.get(getField()), this.valueObject);
    }
}
