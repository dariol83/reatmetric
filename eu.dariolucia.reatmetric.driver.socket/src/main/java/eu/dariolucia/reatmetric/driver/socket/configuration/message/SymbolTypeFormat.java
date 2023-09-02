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
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolTypeFormat {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = true)
    private ValueTypeEnum type;

    @XmlAttribute
    private RadixEnum radix = RadixEnum.DEC;

    @XmlAttribute(name = "encode-format")
    private String format = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    public RadixEnum getRadix() {
        return radix;
    }

    public void setRadix(RadixEnum radix) {
        this.radix = radix;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Object decode(String valueString) {
        if(type == ValueTypeEnum.ENUMERATED) {
            // Check the radix
            if (radix == RadixEnum.DEC) {
                return ValueUtil.parse(type, valueString);
            } else {
                return Integer.parseInt(valueString, radix.getRadix());
            }
        } else if(type == ValueTypeEnum.UNSIGNED_INTEGER || type == ValueTypeEnum.SIGNED_INTEGER) {
            // Check the radix
            if (radix == RadixEnum.DEC) {
                return ValueUtil.parse(type, valueString);
            } else {
                return Long.parseLong(valueString, radix.getRadix());
            }
        } else {
            return ValueUtil.parse(type, valueString);
        }
    }

    public String encode(Object value) {
        if(type == ValueTypeEnum.ENUMERATED) {
            if(format != null) {
                // Use format
                return String.format(format, value);
            } else {
                // Check the radix
                if (radix == RadixEnum.DEC) {
                    return ValueUtil.toString(type, value);
                } else {
                    return Integer.toString((Integer) value, radix.getRadix());
                }
            }
        } else if(type == ValueTypeEnum.UNSIGNED_INTEGER || type == ValueTypeEnum.SIGNED_INTEGER) {
            if (format != null) {
                // Use format
                return String.format(format, value);
            } else {
                // Check the radix
                if (radix == RadixEnum.DEC) {
                    return ValueUtil.toString(type, value);
                } else {
                    return Long.toString((Long) value, radix.getRadix());
                }
            }
        } else if(type == ValueTypeEnum.REAL && format != null) {
            // Use format
            return String.format(format, value);
        } else {
            return ValueUtil.toString(type, value);
        }
    }

}
