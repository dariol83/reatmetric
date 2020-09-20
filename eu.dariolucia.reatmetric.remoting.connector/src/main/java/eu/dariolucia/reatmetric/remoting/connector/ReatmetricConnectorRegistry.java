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

package eu.dariolucia.reatmetric.remoting.connector;

import eu.dariolucia.reatmetric.api.IReatmetricRegister;
import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.remoting.connector.configuration.ConnectorConfiguration;
import eu.dariolucia.reatmetric.remoting.connector.configuration.RemotingConnectorConfiguration;
import eu.dariolucia.reatmetric.remoting.connector.proxy.ReatmetricProxy;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ReatmetricConnectorRegistry implements IReatmetricRegister {

    private static final String INIT_FILE_KEY = "reatmetric.remoting.connector.config"; // Absolute location of the configuration file, to configure the registry

    private final List<IReatmetricSystem> systems = new ArrayList<>();

    public ReatmetricConnectorRegistry() throws FileNotFoundException, JAXBException {
        String filePath = System.getProperty(INIT_FILE_KEY);
        RemotingConnectorConfiguration conf = RemotingConnectorConfiguration.load(new FileInputStream(filePath));
        // Now create a proxy object for each system
        for(ConnectorConfiguration cc : conf.getConnectors()) {
            ReatmetricProxy proxy = new ReatmetricProxy(cc);
            systems.add(proxy);
        }
    }

    @Override
    public List<IReatmetricSystem> availableSystems() {
        return systems;
    }
}
