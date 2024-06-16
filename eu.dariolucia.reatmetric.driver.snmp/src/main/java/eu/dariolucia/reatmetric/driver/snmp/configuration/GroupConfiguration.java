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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GroupConfiguration {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "polling-time")
    private int pollingTime = 2000;

    @XmlElement(name = "entry")
    private List<OidEntry> oidEntryList = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPollingTime() {
        return pollingTime;
    }

    public void setPollingTime(int pollingTime) {
        this.pollingTime = pollingTime;
    }

    public List<OidEntry> getOidEntryList() {
        return oidEntryList;
    }

    public void setOidEntryList(List<OidEntry> oidEntryList) {
        this.oidEntryList = oidEntryList;
    }

    public PDU preparePollRequest() {
        PDU pdu = new PDU();
        for(OidEntry oid : oidEntryList) {
            pdu.add(new VariableBinding(oid.toOid()));
        }
        pdu.setType(PDU.GET);
        return pdu;
    }
}
