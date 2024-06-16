/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

@XmlRootElement(name = "snmp-device", namespace = "http://dariolucia.eu/reatmetric/driver/snmp/device")
@XmlAccessorType(XmlAccessType.FIELD)
public class SnmpDeviceConfiguration {

    public static SnmpDeviceConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SnmpDeviceConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SnmpDeviceConfiguration sc = (SnmpDeviceConfiguration) u.unmarshal(is);
            sc.initialise();
            return sc;
        } catch (JAXBException | ReatmetricException e) {
            throw new IOException(e);
        }
    }

    @XmlElement(name="group")
    private List<GroupConfiguration> groupConfigurationList = new LinkedList<>();

    public List<GroupConfiguration> getGroupConfigurationList() {
        return groupConfigurationList;
    }

    public void setGroupConfigurationList(List<GroupConfiguration> groupConfigurationList) {
        this.groupConfigurationList = groupConfigurationList;
    }

    private void initialise() throws ReatmetricException {
        //
    }
}
