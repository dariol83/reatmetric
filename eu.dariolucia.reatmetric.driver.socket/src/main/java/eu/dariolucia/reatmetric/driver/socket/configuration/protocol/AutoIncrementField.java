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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class indicates the name of the field in the {@link eu.dariolucia.reatmetric.driver.socket.configuration.message.MessageDefinition} that
 * this protocol/route will use with as autoincrement integer value, if such value is not previously mapped/provided.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AutoIncrementField {

    @XmlAttribute(required = true)
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private final AtomicInteger counter = new AtomicInteger(0);

    public int next() {
        return counter.getAndIncrement();
    }

    public int get() {
        return counter.get();
    }
}
