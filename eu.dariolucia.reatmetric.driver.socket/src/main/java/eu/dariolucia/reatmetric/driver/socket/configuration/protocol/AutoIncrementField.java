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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class indicates the name of the field in the {@link eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition} that
 * this protocol/route will use with as autoincrement integer value, if such value is not previously mapped/provided.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AutoIncrementField {

    // This attributes identifies a counter per route
    @XmlAttribute(name="counter-id", required = true)
    private String counterId;

    @XmlAttribute(required = true)
    private String field;

    @XmlAttribute(name="output-type")
    private ValueTypeEnum type = ValueTypeEnum.ENUMERATED;

    // Used only in case of type == ValueTypeEnum.CHARACTER_STRING
    @XmlAttribute(name="string-format")
    private String stringFormat = null;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    public String getStringFormat() {
        return stringFormat;
    }

    public void setStringFormat(String stringFormat) {
        this.stringFormat = stringFormat;
    }

    public String getCounterId() {
        return counterId;
    }

    public void setCounterId(String counterId) {
        this.counterId = counterId;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    @XmlTransient
    private RouteConfiguration parentRoute;

    public void initialise(RouteConfiguration routeConfiguration) {
        this.parentRoute = routeConfiguration;
    }

    public Object next() {
        return transform(this.parentRoute.getNextSequenceOf(getCounterId()));
    }

    private Object transform(int value) {
        switch (type) {
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER:
                return (long) value;
            case REAL:
                return (double) value;
            case CHARACTER_STRING:
                if(stringFormat != null) {
                    return String.format(stringFormat, value);
                } else {
                    return String.valueOf(value);
                }
            default:
                return value;
        }
    }
}
