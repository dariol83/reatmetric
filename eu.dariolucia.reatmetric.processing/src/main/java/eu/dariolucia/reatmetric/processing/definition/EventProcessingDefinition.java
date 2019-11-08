/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventProcessingDefinition {

    @XmlID
    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute
    private String description = "";

    @XmlAttribute(required = true)
    private String location;

    @XmlAttribute
    private EventSeverity severity = EventSeverity.INFO;

    public EventProcessingDefinition() {
    }

    public EventProcessingDefinition(String id, String name, String description, String location, EventSeverity severity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.severity = severity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }
}
