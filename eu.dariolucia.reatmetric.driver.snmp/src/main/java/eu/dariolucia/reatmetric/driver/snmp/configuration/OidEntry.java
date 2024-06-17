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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

import java.util.logging.Level;

@XmlAccessorType(XmlAccessType.FIELD)
public class OidEntry {

    @XmlAttribute(name = "oid", required = true)
    private String oid;

    @XmlAttribute(name = "path", required = true)
    private String path;

    @XmlAttribute(name = "type", required = true)
    private ValueTypeEnum type;

    public OidEntry() {
        // Nothing
    }

    public OidEntry(String oid, String path, ValueTypeEnum type) {
        this.oid = oid;
        this.path = path;
        this.type = type;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public OID toOid() {
        return new OID(oid);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ValueTypeEnum getType() {
        return type;
    }

    public void setType(ValueTypeEnum type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "{'" + oid + "' -> " + path + " (" + type + ')';
    }

    @XmlTransient
    private int externalId;

    public void setExternalId(int id) {
        this.externalId = id;
    }

    public int getExternalId() {
        return externalId;
    }

    public Object extractValue(Variable variable) {
        switch (type) {
            case ENUMERATED: return extractInt(variable);
            case SIGNED_INTEGER:
            case UNSIGNED_INTEGER: return extractLong(variable);
            case OCTET_STRING: return extractByteArray(variable);
            case CHARACTER_STRING: return extractString(variable);
            case REAL: return extractReal(variable);
            default: return null;
        }
    }

    private Object extractReal(Variable variable) {
        if (variable instanceof OctetString) {
            String itemValue = variable.toString();
            try {
                return Double.parseDouble(itemValue);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Object extractByteArray(Variable variable) {
        if (variable instanceof OctetString) {
            return ((OctetString) variable).toByteArray();
        }
        return null;
    }

    private Object extractString(Variable variable) {
        if (variable instanceof OctetString) {
            return variable.toString();
        }
        return null;
    }

    private Object extractLong(Variable variable) {
        if (variable instanceof OctetString) {
            String itemValue = variable.toString();
            try {
                return Long.parseLong(itemValue);
            } catch (Exception e) {
                return null;
            }
        }
        return variable.toLong();
    }

    private Object extractInt(Variable variable) {
        if (variable instanceof OctetString) {
            String itemValue = variable.toString();
            try {
                return Integer.parseInt(itemValue);
            } catch (Exception e) {
                return null;
            }
        }
        return variable.toInt();
    }
}
