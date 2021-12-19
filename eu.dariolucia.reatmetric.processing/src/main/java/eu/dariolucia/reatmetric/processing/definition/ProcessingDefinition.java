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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Read and aggregate all files present in the provided location. File names with a leading dot are ignored.
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

    public boolean isSyntheticParameterProcessingEnabled() {
        return syntheticParameterProcessingEnabled;
    }

    public void setSyntheticParameterProcessingEnabled(boolean syntheticParameterProcessingEnabled) {
        this.syntheticParameterProcessingEnabled = syntheticParameterProcessingEnabled;
    }

    /**
     * If a definition is marked as mirrored, it means that the processing object can be updated with a full state
     * from an external processing model: updatable objects are not processed by the processing model.
     *
     * @return true if mirrored, otherwise false
     */
    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    /**
     * The next transient field is used to provide the location folder where the cache file for the topological sorting will
     * be stored:
     * <ul>
     * <li>If it is not set, the topological sorting will be created from the definitions but not cached.</li>
     * <li>If it is set and the cache file is not present, the topological sorting will be created from the definitions and a cache file created.</li>
     * <li>If it is set and the cache file is present, the topological sort will be loaded from the cache file.</li>
     * </ul>
     */
    private transient String cacheFolder;

    public String getCacheFolder() {
        return cacheFolder;
    }

    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }
}
