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

package eu.dariolucia.reatmetric.driver.spacecraft.definition.services;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "event-service", namespace = "http://dariolucia.eu/reatmetric/driver/spacecraft/event-service")
@XmlAccessorType(XmlAccessType.FIELD)
public class OnboardEventServiceConfiguration {

    public static OnboardEventServiceConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(OnboardEventServiceConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            OnboardEventServiceConfiguration o = (OnboardEventServiceConfiguration) u.unmarshal(is);
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "event-id-offset")
    private int eventIdOffset = 0; // The offset to apply to the packet external ID to raise the event

    public int getEventIdOffset() {
        return eventIdOffset;
    }

    public void setEventIdOffset(int eventIdOffset) {
        this.eventIdOffset = eventIdOffset;
    }
}
