/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "core", namespace = "http://dariolucia.eu/reatmetric/core/configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceCoreConfiguration {

    private static final String HOME_VAR = "$HOME";
    private static final String HOME_DIR = System.getProperty("user.home");

    public static ServiceCoreConfiguration load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ServiceCoreConfiguration.class);
        Unmarshaller u = jc.createUnmarshaller();
        ServiceCoreConfiguration configuration = (ServiceCoreConfiguration) u.unmarshal(is);
        // Update $HOME
        configuration.setArchiveLocation(configuration.getArchiveLocation().replace(HOME_VAR, HOME_DIR));
        configuration.setDefinitionsLocation(configuration.getDefinitionsLocation().replace(HOME_VAR, HOME_DIR));
        configuration.setLogPropertyFile(configuration.getLogPropertyFile().replace(HOME_VAR, HOME_DIR));
        for(DriverConfiguration dc : configuration.getDrivers()) {
            dc.setConfiguration(dc.getConfiguration().replace(HOME_VAR, HOME_DIR));
        }
        return configuration;
    }

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "logPropertyFile")
    private String logPropertyFile;

    @XmlElement(name = "archiveLocation", required = true)
    private String archiveLocation;

    @XmlElement(name = "definitionsLocation", required = true)
    private String definitionsLocation;

    @XmlElement(name = "initialisation")
    private StateInitialisationConfiguration initialisation;

    @XmlElement(name = "driver", required = true)
    private List<DriverConfiguration> drivers = new LinkedList<>();

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

    public StateInitialisationConfiguration getInitialisation() {
        return initialisation;
    }

    public void setInitialisation(StateInitialisationConfiguration initialisation) {
        this.initialisation = initialisation;
    }
}
