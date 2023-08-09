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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the entry point of the configuration of the ReatMetric Processing module. It defines all aspects
 * related to the processing of parameters, events and activities and their organisation in a hierarchical tree.
 *
 * Root element: processing
 * Namespace: http://dariolucia.eu/reatmetric/processing/definition
 */
@XmlRootElement(name = "processing", namespace = "http://dariolucia.eu/reatmetric/processing/definition")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessingDefinition implements Serializable {

    private static final Logger LOG = Logger.getLogger(ProcessingDefinition.class.getName());

    /**
     * This method serialises the provided {@link ProcessingDefinition} object to the provided
     * {@link OutputStream}.
     *
     * @param d   the processing definition to serialise
     * @param out the output stream
     * @throws IOException in case of problems while serialising or writing to the stream
     */
    public static void save(ProcessingDefinition d, OutputStream out) throws IOException {
        try {
            JAXBContext context = JAXBContext.newInstance(ProcessingDefinition.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(d, out);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    /**
     * This method loads a {@link ProcessingDefinition} object from the provided {@link InputStream}.
     * After loading, depending on whether the definitions are mirrored or whether a path prefix is defined, the
     * properties are propagated to all elements in the tree:
     * <ul>
     *     <li>The 'mirroring' properties is set to all elements</li>
     *     <li>The 'path prefix' is prepended to the 'location' of all elements</li>
     * </ul>
     *
     * @param is    the input stream
     * @return the {@link ProcessingDefinition} object
     * @throws JAXBException
     */
    public static ProcessingDefinition load(InputStream is) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ProcessingDefinition.class);
        Unmarshaller u = jc.createUnmarshaller();
        ProcessingDefinition processingDefinition = (ProcessingDefinition) u.unmarshal(is);
        // Update properties to propagate
        if(processingDefinition.isMirrored() || processingDefinition.getPathPrefix() != null) {
            for (ParameterProcessingDefinition parameterProcessingDefinition : processingDefinition.getParameterDefinitions()) {
                propagateProperties(processingDefinition, parameterProcessingDefinition);
            }
            for (EventProcessingDefinition eventProcessingDefinition : processingDefinition.getEventDefinitions()) {
                propagateProperties(processingDefinition, eventProcessingDefinition);
            }
            for (ActivityProcessingDefinition activityProcessingDefinition : processingDefinition.getActivityDefinitions()) {
                propagateProperties(processingDefinition, activityProcessingDefinition);
            }
        }
        return processingDefinition;
    }

    private static void propagateProperties(ProcessingDefinition processingDefinition, AbstractProcessingDefinition o) {
        if(processingDefinition.isMirrored()) {
            o.setMirrored(processingDefinition.isMirrored());
        }
        if(processingDefinition.getPathPrefix() != null) {
            o.setLocation(processingDefinition.getPathPrefix() + o.getLocation());
        }
    }

    /**
     * Read and aggregate all files present in the provided folder location. File names with a leading dot are ignored.
     * The attribute 'synthetic_parameter_processing_enabled' is set to true in the returned object, if at least one
     * file contains such attribute set to true.
     *
     * @param definitionsLocation the folder containing the definition files
     * @return the aggregated definitions
     * @throws ReatmetricException in case of issues during the loading of the files
     */
    public static ProcessingDefinition loadAll(String definitionsLocation) throws ReatmetricException {
        ProcessingDefinition aggregated = new ProcessingDefinition();
        File folder = new File(definitionsLocation);
        if(!folder.exists() || folder.listFiles() == null) {
            throw new ReatmetricException("Cannot read definition files in folder " + definitionsLocation);
        }
        for(File def : folder.listFiles()) {
            // Ignore files with leading dot
            if(def.getName().startsWith(".")) {
                continue;
            }
            //
            try {
                ProcessingDefinition eachDef = ProcessingDefinition.load(new FileInputStream(def));
                aggregated.getParameterDefinitions().addAll(eachDef.getParameterDefinitions());
                aggregated.getEventDefinitions().addAll(eachDef.getEventDefinitions());
                aggregated.getActivityDefinitions().addAll(eachDef.getActivityDefinitions());
                aggregated.setSyntheticParameterProcessingEnabled(aggregated.isSyntheticParameterProcessingEnabled() && eachDef.isSyntheticParameterProcessingEnabled());
            } catch(IOException | JAXBException e) {
                LOG.log(Level.WARNING, "Cannot read definitions at " + def.getAbsolutePath(), e);
            }
        }
        return aggregated;
    }

    @XmlAttribute(name = "synthetic_parameter_processing_enabled")
    private boolean syntheticParameterProcessingEnabled = true;

    @XmlAttribute(name = "mirrored")
    private boolean mirrored = false;

    @XmlAttribute(name = "path_prefix")
    private String pathPrefix = null;

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

    /**
     * The list of defined {@link ParameterProcessingDefinition} objects.
     * <p></p>
     * Element name: parameters/parameter
     *
     * @return the list of defined {@link ParameterProcessingDefinition} objects
     */
    public List<ParameterProcessingDefinition> getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setParameterDefinitions(List<ParameterProcessingDefinition> parameterDefinitions) {
        this.parameterDefinitions = parameterDefinitions;
    }

    /**
     * The list of defined {@link EventProcessingDefinition} objects.
     * <p></p>
     * Element name: events/event
     *
     * @return the list of defined {@link EventProcessingDefinition} objects
     */
    public List<EventProcessingDefinition> getEventDefinitions() {
        return eventDefinitions;
    }

    public void setEventDefinitions(List<EventProcessingDefinition> eventDefinitions) {
        this.eventDefinitions = eventDefinitions;
    }

    /**
     * The list of defined {@link ActivityProcessingDefinition} objects.
     * <p></p>
     * Element name: activities/activity
     *
     * @return the list of defined {@link ActivityProcessingDefinition} objects
     */
    public List<ActivityProcessingDefinition> getActivityDefinitions() {
        return activityDefinitions;
    }

    public ProcessingDefinition setActivityDefinitions(List<ActivityProcessingDefinition> activityDefinitions) {
        this.activityDefinitions = activityDefinitions;
        return this;
    }

    /**
     * Whether the synthetic parameters defined in this hierarchy must be processed.
     * <p></p>
     * Attribute: synthetic_parameter_processing_enabled
     *
     * @return true if synthetic parameters must be processed, false otherwise
     */
    public boolean isSyntheticParameterProcessingEnabled() {
        return syntheticParameterProcessingEnabled;
    }

    public void setSyntheticParameterProcessingEnabled(boolean syntheticParameterProcessingEnabled) {
        this.syntheticParameterProcessingEnabled = syntheticParameterProcessingEnabled;
    }

    /**
     * If a definition is marked as mirrored, it means that the processing object can only be updated with a full state
     * from an external processing model: mirrored objects are not processed by the processing model.
     * <p></p>
     * Attribute: mirrored
     *
     * @return true if mirrored, otherwise false
     */
    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    /**
     * If a path prefix is provided, all definition locations in this hierarchy are prepended with the specified path.
     * <p></p>
     * Attribute: path_prefix
     *
     * @return the path prefix, or null
     */
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    private transient String cacheFolder;

    /**
     * This transient field is used to provide the location folder where the cache file for the topological sorting will
     * be stored:
     * <ul>
     * <li>If it is not set, the topological sorting will be created from the definitions but not cached.</li>
     * <li>If it is set and the cache file is not present, the topological sorting will be created from the definitions and a cache file created.</li>
     * <li>If it is set and the cache file is present, the topological sort will be loaded from the cache file.</li>
     * </ul>
     *
     * This method is used by the ReatMetric Processing module, and it is not supposed to be invoked by external users.
     *
     * @return the cache folder
     */
    public String getCacheFolder() {
        return cacheFolder;
    }

    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }
}
