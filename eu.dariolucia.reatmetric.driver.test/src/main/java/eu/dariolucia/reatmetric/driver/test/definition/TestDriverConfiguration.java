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

package eu.dariolucia.reatmetric.driver.test.definition;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "test-driver", namespace = "http://dariolucia.eu/reatmetric/driver/test")
@XmlAccessorType(XmlAccessType.FIELD)
public class TestDriverConfiguration {

    public static TestDriverConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(TestDriverConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (TestDriverConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * System entity offset: it is applied (added) to the system entity ID of parameters, events and (subtracted) telecommands.
     */
    @XmlAttribute(name = "system-entity-offset")
    private int systemEntityOffset = 0;

    public int getSystemEntityOffset() {
        return systemEntityOffset;
    }

    public void setSystemEntityOffset(int systemEntityOffset) {
        this.systemEntityOffset = systemEntityOffset;
    }
}
