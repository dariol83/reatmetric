/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.configuration;

import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;

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

    public static ServiceCoreConfiguration load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ServiceCoreConfiguration.class);
        Unmarshaller u = jc.createUnmarshaller();
        return (ServiceCoreConfiguration) u.unmarshal(is);
    }

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "logPropertyFile")
    private String logPropertyFile;

    @XmlElement(name = "archiveLocation", required = true)
    private String archiveLocation;

    @XmlElement(name = "driver", required = true)
    private List<DriverConfiguration> drivers = new LinkedList<>();

    public ServiceCoreConfiguration() {
    }

    public ServiceCoreConfiguration(String name, String logPropertyFile, String archiveLocation, List<DriverConfiguration> drivers) {
        this.name = name;
        this.logPropertyFile = logPropertyFile;
        this.archiveLocation = archiveLocation;
        this.drivers = drivers;
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

    public List<DriverConfiguration> getDrivers() {
        return drivers;
    }

    public ServiceCoreConfiguration setDrivers(List<DriverConfiguration> drivers) {
        this.drivers = drivers;
        return this;
    }
}
