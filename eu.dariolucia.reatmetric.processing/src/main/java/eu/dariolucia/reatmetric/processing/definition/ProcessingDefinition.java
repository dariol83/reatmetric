/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlRootElement(name = "processing", namespace = "http://dariolucia.eu/reatmetric/processing/definition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessingDefinition {

    private static final Logger LOG = Logger.getLogger(ProcessingDefinition.class.getName());

    public static ProcessingDefinition load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ProcessingDefinition.class);
        Unmarshaller u = jc.createUnmarshaller();
        ProcessingDefinition o = (ProcessingDefinition) u.unmarshal(is);
        return o;
    }

    public static ProcessingDefinition loadAll(String definitionsLocation) throws ReatmetricException {
        ProcessingDefinition aggregated = new ProcessingDefinition();
        File folder = new File(definitionsLocation);
        if(!folder.exists() || folder.listFiles() == null) {
            throw new ReatmetricException("Cannot read definition files in folder " + definitionsLocation);
        }
        for(File def : folder.listFiles()) {
            try {
                ProcessingDefinition eachDef = ProcessingDefinition.load(new FileInputStream(def));
                aggregated.getParameterDefinitions().addAll(eachDef.getParameterDefinitions());
                aggregated.getEventDefinitions().addAll(eachDef.getEventDefinitions());
                aggregated.getActivityDefinitions().addAll(eachDef.getActivityDefinitions());
            } catch(IOException | JAXBException e) {
                LOG.log(Level.WARNING, "Cannot read definitions at " + def.getAbsolutePath(), e);
            }
        }
        return aggregated;
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
