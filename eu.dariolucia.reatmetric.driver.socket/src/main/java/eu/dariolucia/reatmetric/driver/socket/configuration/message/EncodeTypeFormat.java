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

package eu.dariolucia.reatmetric.driver.socket.configuration.message;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * This class is used to specify how to encode a specific type in the case of DERIVED fields.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class EncodeTypeFormat {

    @XmlAttribute(required = true)
    private ValueTypeEnum id;

    @XmlAttribute(name = "encode-format", required = true)
    private String encodeFormat;

    public ValueTypeEnum getId() {
        return id;
    }

    public void setId(ValueTypeEnum id) {
        this.id = id;
    }

    public String getEncodeFormat() {
        return encodeFormat;
    }

    public void setEncodeFormat(String encodeFormat) {
        this.encodeFormat = encodeFormat;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    public String encodeValue(Object o) {
        return String.format(encodeFormat, o);
    }
}
