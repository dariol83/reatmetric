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

import eu.dariolucia.reatmetric.remoting.connector.configuration.ConnectorConfiguration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;

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

    /**
     * Name of the system, as registered in the remoting connector configuration
     * as 'local-name'.
     *
     * @see ConnectorConfiguration#getLocalName()
     */
    @XmlAttribute(name = "remote-system-name", required = true)
    private String remoteSystemName;

    /**
     * Prefix added to the remote definitions in the processing model.
     * This property is needed to derive the system entity paths of the remote (lower level) processing model,
     * starting from the paths of the local (higher level) processing model.
     * <p>
     *     For instance:
     *     <ul>
     *         <li>Remote path: systemA.deviceB.elementC.paramD</li>
     *         <li>Local path: domainX.systemA.deviceB.elementC.paramD</li>
     *         <li>Remote path prefix: domainX.</li>
     *     </ul>
     * </p>
     */
    @XmlAttribute(name = "remote-path-prefix")
    private String remotePathPrefix = "";

    /**
     * Path indicating the (container) system entity (in the local processing model) that maps to
     * the remote system's processing model (or part of it).
     * This property is needed to allow the driver to subscribe to the relevant parameters and events
     * defined in the local definitions.
     * <p>
     *     For instance:
     *     <ul>
     *         <li>Remote path: 'systemA' and all children</li>
     *         <li>Local path: domainX.systemA...</li>
     *         <li>Remote path selector: domainX.systemA</li>
     *     </ul>
     * </p>
     */
    @XmlAttribute(name = "remote-path-selector", required = true)
    private String remotePathSelector;

    public String getRemoteSystemName() {
        return remoteSystemName;
    }

    public void setRemoteSystemName(String remoteSystemName) {
        this.remoteSystemName = remoteSystemName;
    }

    public String getRemotePathSelector() {
        return remotePathSelector;
    }

    public void setRemotePathSelector(String remotePathSelector) {
        this.remotePathSelector = remotePathSelector;
    }

    public String getRemotePathPrefix() {
        return remotePathPrefix;
    }

    public void setRemotePathPrefix(String remotePathPrefix) {
        this.remotePathPrefix = remotePathPrefix;
    }

}
