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

package eu.dariolucia.reatmetric.driver.remote.definition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "remote", namespace = "http://dariolucia.eu/reatmetric/driver/remote")
@XmlAccessorType(XmlAccessType.FIELD)
public class RemoteConfiguration {

    public static RemoteConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(RemoteConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (RemoteConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "remote-system-name", required = true)
    private String name;

    @XmlAttribute(name = "local-path-prefix", required = true)
    private String localPathPrefix;

    @XmlAttribute(name = "route", required = true)
    private String route;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalPathPrefix() {
        return localPathPrefix;
    }

    public void setLocalPathPrefix(String localPathPrefix) {
        this.localPathPrefix = localPathPrefix;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }
}
