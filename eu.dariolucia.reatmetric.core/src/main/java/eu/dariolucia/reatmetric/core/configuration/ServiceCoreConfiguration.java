/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "core", namespace = "http://dariolucia.eu/reatmetric/core/configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceCoreConfiguration {

    @XmlElement(name = "archiveLocation", required = true)
    private String archiveLocation;

    @XmlElement(name = "driver", required = true)
    private List<DriverConfiguration> drivers = new LinkedList<>();


}
