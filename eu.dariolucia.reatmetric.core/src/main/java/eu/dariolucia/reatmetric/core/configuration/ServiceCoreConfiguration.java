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

package eu.dariolucia.reatmetric.core.configuration;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "core", namespace = "http://dariolucia.eu/reatmetric/core/configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceCoreConfiguration {

    private static final String HOME_VAR = "$HOME";
    private static final String HOME_DIR = System.getProperty("user.home");

    private static final String PREFIX_VAR = "$PREFIX";
    private static final String PREFIX_DIR = System.getProperty("reatmetric.prefix.dir", "");

    public static ServiceCoreConfiguration load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ServiceCoreConfiguration.class);
        Unmarshaller u = jc.createUnmarshaller();
        ServiceCoreConfiguration configuration = (ServiceCoreConfiguration) u.unmarshal(is);
        // Update $HOME and $PREFIX
        if(configuration.getArchiveLocation() != null) {
            configuration.setArchiveLocation(configuration.getArchiveLocation().replace(HOME_VAR, HOME_DIR));
            configuration.setArchiveLocation(configuration.getArchiveLocation().replace(PREFIX_VAR, PREFIX_DIR));
        }
        configuration.setDefinitionsLocation(configuration.getDefinitionsLocation().replace(HOME_VAR, HOME_DIR));
        if(configuration.getLogPropertyFile() != null) {
            configuration.setLogPropertyFile(configuration.getLogPropertyFile().replace(HOME_VAR, HOME_DIR));
            configuration.setLogPropertyFile(configuration.getLogPropertyFile().replace(PREFIX_VAR, PREFIX_DIR));
        }
        if(configuration.getSchedulerConfiguration() != null) {
            configuration.setSchedulerConfiguration(configuration.getSchedulerConfiguration().replace(HOME_VAR, HOME_DIR));
            configuration.setSchedulerConfiguration(configuration.getSchedulerConfiguration().replace(PREFIX_VAR, PREFIX_DIR));
        }
        for(DriverConfiguration dc : configuration.getDrivers()) {
            dc.setConfiguration(dc.getConfiguration().replace(HOME_VAR, HOME_DIR));
            dc.setConfiguration(dc.getConfiguration().replace(PREFIX_VAR, PREFIX_DIR));
        }
        //
        return configuration;
    }

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "log-property-file")
    private String logPropertyFile;

    @XmlElement(name = "archive-location")
    private String archiveLocation;

    @XmlElement(name = "definitions-location", required = true)
    private String definitionsLocation;

    @XmlElements({
            @XmlElement(name="init-resume",type=ResumeInitialisationConfiguration.class),
            @XmlElement(name="init-from-time",type=TimeInitialisationConfiguration.class)
    })
    private AbstractInitialisationConfiguration initialisation;

    @XmlElement(name = "scheduler-configuration")
    private String schedulerConfiguration;

    @XmlElement(name = "driver", required = true)
    private List<DriverConfiguration> drivers = new LinkedList<>();

    @XmlElement(name = "autostart-connectors")
    private AutostartConnectorsConfiguration autostartConnectors = new AutostartConnectorsConfiguration();

    public ServiceCoreConfiguration() {
    }

    public String getName() {
        return name;
    }

    public ServiceCoreConfiguration setName(String name) {
        this.name = name;
        return this;
    }

    public String getLogPropertyFile() {
        return logPropertyFile;
    }

    public ServiceCoreConfiguration setLogPropertyFile(String logPropertyFile) {
        this.logPropertyFile = logPropertyFile;
        return this;
    }

    public String getArchiveLocation() {
        return archiveLocation;
    }

    public ServiceCoreConfiguration setArchiveLocation(String archiveLocation) {
        this.archiveLocation = archiveLocation;
        return this;
    }

    public String getDefinitionsLocation() {
        return definitionsLocation;
    }

    public ServiceCoreConfiguration setDefinitionsLocation(String definitionsLocation) {
        this.definitionsLocation = definitionsLocation;
        return this;
    }

    public List<DriverConfiguration> getDrivers() {
        return drivers;
    }

    public ServiceCoreConfiguration setDrivers(List<DriverConfiguration> drivers) {
        this.drivers = drivers;
        return this;
    }

    public AbstractInitialisationConfiguration getInitialisation() {
        return initialisation;
    }

    public void setInitialisation(AbstractInitialisationConfiguration initialisation) {
        this.initialisation = initialisation;
    }

    public String getSchedulerConfiguration() {
        return schedulerConfiguration;
    }

    public void setSchedulerConfiguration(String schedulerConfiguration) {
        this.schedulerConfiguration = schedulerConfiguration;
    }

    public AutostartConnectorsConfiguration getAutostartConnectors() {
        return autostartConnectors;
    }

    public void setAutostartConnectors(AutostartConnectorsConfiguration autostartConnectors) {
        this.autostartConnectors = autostartConnectors;
    }
}
