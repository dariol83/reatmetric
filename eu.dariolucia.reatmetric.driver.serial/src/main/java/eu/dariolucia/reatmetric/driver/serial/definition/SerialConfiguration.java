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

package eu.dariolucia.reatmetric.driver.serial.definition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "serial", namespace = "http://dariolucia.eu/reatmetric/driver/serial")
@XmlAccessorType(XmlAccessType.FIELD)
public class SerialConfiguration {

    public static SerialConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(SerialConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            SerialConfiguration o = (SerialConfiguration) u.unmarshal(is);
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "bus", required = true)
    private int bus;

    @XmlAttribute(name = "device", required = true)
    private int device;

    @XmlAttribute(name = "timeout")
    private int timeout = 10;

    public int getBus() {
        return bus;
    }

    public SerialConfiguration setBus(int bus) {
        this.bus = bus;
        return this;
    }

    public int getDevice() {
        return device;
    }

    public SerialConfiguration setDevice(int device) {
        this.device = device;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public SerialConfiguration setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
