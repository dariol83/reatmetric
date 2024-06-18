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

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import jakarta.xml.bind.annotation.*;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class GroupConfiguration {

    private static final Logger LOG = Logger.getLogger(GroupConfiguration.class.getName());

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "polling-time")
    private int pollingTime = 2000;

    @XmlAttribute(name = "distribute-pdu")
    private boolean distributePdu = false;

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

    public boolean isDistributePdu() {
        return distributePdu;
    }

    public void setDistributePdu(boolean distributePdu) {
        this.distributePdu = distributePdu;
    }

    @XmlTransient
    private final Map<OID, OidEntry> oid2parameterMap = new HashMap<>();

    public void initialise(String prefix, IProcessingModel processingModel) {
        // Map all OIDs to parameter IDs
        for(OidEntry e : getOidEntryList()) {
            // Build path
            SystemEntityPath path = SystemEntityPath.fromString(prefix + "." + e.getPath());
            try {
                int id = processingModel.getExternalIdOf(path);
                e.setExternalId(id);
                oid2parameterMap.put(e.toOid(), e);
            } catch (ReatmetricException ex) {
                LOG.log(Level.SEVERE, "Cannot resolve parameter path " + path + " linked to OID " + e.getOid() + " to external ID: " + ex.getMessage(), e);
            }
        }
    }

    public PDU preparePollRequest() {
        PDU pdu = new PDU();
        for(OidEntry oid : oidEntryList) {
            pdu.add(new VariableBinding(oid.toOid()));
        }
        pdu.setType(PDU.GET);
        return pdu;
    }

    public List<ParameterSample> mapResponse(SnmpDevice device, ResponseEvent<?> responseEvent, Instant generationTime) {
        List<ParameterSample> toReturn = new LinkedList<>();
        String route = device.getName();
        for(VariableBinding vb : responseEvent.getResponse().getAll()) {
            OID theOid = vb.getOid();
            OidEntry theEntry = oid2parameterMap.get(theOid);
            if(theEntry != null) {
                Object value = theEntry.extractValue(vb.getVariable());
                if(value != null) {
                    ParameterSample sample = ParameterSample.of(theEntry.getExternalId(), generationTime, generationTime, null, value, route, null);
                    toReturn.add(sample);
                } else {
                    if(LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, String.format("OID %s value is null, actual value was %s", theOid, vb.getVariable().toString()));
                    }
                }
            } else {
                if(LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, String.format("OID %s not known, ignoring...", theOid));
                }
            }
        }
        return toReturn;
    }
}
