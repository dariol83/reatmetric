/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;
import eu.dariolucia.reatmetric.processing.extension.internal.ExtensionRegistry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalCalibration extends CalibrationDefinition {

    @XmlAttribute(required = true)
    private String function;

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    public ExternalCalibration() {
    }

    public ExternalCalibration(String function, List<KeyValue> properties) {
        this.function = function;
        this.properties = properties;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public List<KeyValue> getProperties() {
        return properties;
    }

    public void setProperties(List<KeyValue> properties) {
        this.properties = properties;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient Map<String, String> key2values = new HashMap<>();
    private transient ICalibrationExtension externalCalibration = null;

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        // Initialise the properties
        if(!properties.isEmpty() && key2values.isEmpty()) {
            properties.forEach(o -> key2values.put(o.getKey(), o.getValue()));
        }
        if(externalCalibration == null) {
            // Retrieve calibration
            externalCalibration = ExtensionRegistry.resolveCalibration(this.function);
            if(externalCalibration == null) {
                throw new CalibrationException("External calibration function " + function + " not found");
            }
        }
        return externalCalibration.calibrate(valueToCalibrate, key2values, resolver);
    }
}
