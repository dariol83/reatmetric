/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.snmp.configuration;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;

public enum OidEntryType {
    STRING,
    INTEGER,
    OID,
    LONG,
    DOUBLE,
    BYTE_ARRAY;

    public ValueTypeEnum toValueTypeEnum() {
        switch (this) {
            case OID: return ValueTypeEnum.ENUMERATED;
            case STRING: return ValueTypeEnum.CHARACTER_STRING;
            case BYTE_ARRAY: return ValueTypeEnum.OCTET_STRING;
            case INTEGER: return ValueTypeEnum.ENUMERATED;
            case LONG: return ValueTypeEnum.SIGNED_INTEGER;
            case DOUBLE: return ValueTypeEnum.REAL;
            default: throw new IllegalStateException("Type " + this + " cannot be mapped to ValueTypeEnum");
        }
    }
}
