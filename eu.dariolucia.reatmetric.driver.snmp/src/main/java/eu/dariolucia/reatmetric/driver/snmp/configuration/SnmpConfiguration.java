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
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "snmp-configuration", namespace = "http://dariolucia.eu/reatmetric/driver/snmp")
@XmlAccessorType(XmlAccessType.FIELD)
public class SnmpConfiguration {

    private static final String HOME_VAR = "$HOME";
    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String PREFIX_VAR = "$PREFIX";
    private static final String PREFIX_DIR = System.getProperty("reatmetric.prefix.dir", "");

    public static SnmpConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SnmpConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SnmpConfiguration sc = (SnmpConfiguration) u.unmarshal(is);
            for (SnmpDevice d : sc.getSnmpDeviceList()) {
                d.setConfiguration(d.getConfiguration().replace(HOME_VAR, HOME_DIR));
                d.setConfiguration(d.getConfiguration().replace(PREFIX_VAR, PREFIX_DIR));
            }
            sc.initialise();
            return sc;
        } catch (JAXBException | ReatmetricException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name="device")
    private List<SnmpDevice> snmpDeviceList = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SnmpDevice> getSnmpDeviceList() {
        return snmpDeviceList;
    }

    public void setSnmpDeviceList(List<SnmpDevice> snmpDeviceList) {
        this.snmpDeviceList = snmpDeviceList;
    }

    private void initialise() throws ReatmetricException {
        this.snmpDeviceList.forEach(d -> d.initialise());
    }
}
