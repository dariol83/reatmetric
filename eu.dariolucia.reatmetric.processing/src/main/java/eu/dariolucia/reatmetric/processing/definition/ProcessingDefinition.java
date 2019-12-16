/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "processing", namespace = "http://dariolucia.eu/reatmetric/processing/definition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessingDefinition {

    public static ProcessingDefinition load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ProcessingDefinition.class);
        Unmarshaller u = jc.createUnmarshaller();
        ProcessingDefinition o = (ProcessingDefinition) u.unmarshal(is);
        return o;
    }

    @XmlElementWrapper(name = "parameters")
    @XmlElement(name = "parameter")
    private List<ParameterProcessingDefinition> parameterDefinitions = new LinkedList<>();

    @XmlElementWrapper(name = "events")
    @XmlElement(name = "event")
    private List<EventProcessingDefinition> eventDefinitions = new LinkedList<>();

    @XmlElementWrapper(name = "activities")
    @XmlElement(name = "activity")
    private List<ActivityProcessingDefinition> activityDefinitions = new LinkedList<>();

    public ProcessingDefinition() {
    }

    public ProcessingDefinition(List<ParameterProcessingDefinition> parameterDefinitions, List<EventProcessingDefinition> eventDefinitions, List<ActivityProcessingDefinition> activityDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
        this.eventDefinitions = eventDefinitions;
        this.activityDefinitions = activityDefinitions;
    }

    public List<ParameterProcessingDefinition> getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setParameterDefinitions(List<ParameterProcessingDefinition> parameterDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
    }

    public List<EventProcessingDefinition> getEventDefinitions() {
        return eventDefinitions;
    }

    public void setEventDefinitions(List<EventProcessingDefinition> eventDefinitions) {
        this.eventDefinitions = eventDefinitions;
    }

    public List<ActivityProcessingDefinition> getActivityDefinitions() {
        return activityDefinitions;
    }

    public ProcessingDefinition setActivityDefinitions(List<ActivityProcessingDefinition> activityDefinitions) {
        this.activityDefinitions = activityDefinitions;
        return this;
    }
}
