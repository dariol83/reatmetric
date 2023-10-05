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

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.api.value.ValueUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolTypeFormat {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = true)
    private ValueTypeEnum type;

    @XmlAttribute
    private RadixEnum radix = RadixEnum.DEC;

    /**
     * In case of value types that are not DERIVED, it is possible to define a specific
     * encoding format.
     */
    @XmlAttribute(name = "encode-format")
    private String format = null;

    @XmlAttribute(name = "encode-null")
    private String encodeNull = null;

    @XmlAttribute(name = "decode-empty-null")
    private boolean decodeEmpty = false;

    @XmlElement(name = "type")
    private List<EncodeTypeFormat> encodeTypeFormatList = new LinkedList<>();

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

    public List<EncodeTypeFormat> getEncodeTypeFormatList() {
        return encodeTypeFormatList;
    }

    public void setEncodeTypeFormatList(List<EncodeTypeFormat> encodeTypeFormatList) {
        this.encodeTypeFormatList = encodeTypeFormatList;
    }

    public String getEncodeNull() {
        return encodeNull;
    }

    public void setEncodeNull(String encodeNull) {
        this.encodeNull = encodeNull;
    }

    public boolean isDecodeEmpty() {
        return decodeEmpty;
    }

    public void setDecodeEmpty(boolean decodeEmpty) {
        this.decodeEmpty = decodeEmpty;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    public Object decode(String valueString) {
        if(valueString.isEmpty() && isDecodeEmpty()) {
            return null;
        }
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
        } else if(type == ValueTypeEnum.DERIVED) {
            Pair<Object, ValueTypeEnum> attemptedParse = ValueUtil.tryParse(valueString);
            if(attemptedParse == null) {
                return null;
            } else {
                return attemptedParse.getFirst();
            }
        } else {
            return ValueUtil.parse(type, valueString);
        }
    }

    public String encode(Object value) {
        // Null value? Then if encode-null is provided, return it
        if(value == null && getEncodeNull() != null) {
            return getEncodeNull();
        }
        // DERIVED type? Then check the type and go ahead with the encoding
        if(type == ValueTypeEnum.DERIVED) {
            // Derive the type
            ValueTypeEnum derived = ValueTypeEnum.fromClass(value.getClass());
            for(EncodeTypeFormat etf : encodeTypeFormatList) {
                if(etf.getId() == derived) {
                    return etf.encodeValue(value);
                }
            }
            return ValueUtil.toString(derived, value);
        }
        // Not DERIVED --> Check format
        if(format != null) {
            // Use format
            return String.format(format, value);
        } else {
            if (type == ValueTypeEnum.ENUMERATED) {
                // Check the radix
                if (radix == RadixEnum.DEC) {
                    return ValueUtil.toString(type, value);
                } else {
                    return Integer.toString((Integer) value, radix.getRadix());
                }
            } else if (type == ValueTypeEnum.UNSIGNED_INTEGER || type == ValueTypeEnum.SIGNED_INTEGER) {
                // Check the radix
                if (radix == RadixEnum.DEC) {
                    return ValueUtil.toString(type, value);
                } else {
                    return Long.toString((Long) value, radix.getRadix());
                }
            } else {
                // Standard output
                return ValueUtil.toString(type, value);
            }
        }
    }
}
