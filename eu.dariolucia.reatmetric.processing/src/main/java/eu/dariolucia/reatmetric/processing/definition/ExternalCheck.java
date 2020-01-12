/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.extension.ICheckExtension;
import eu.dariolucia.reatmetric.processing.extension.internal.ExtensionRegistry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalCheck extends CheckDefinition {

    @XmlAttribute(required = true)
    private String function;

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    public ExternalCheck() {
    }

    public ExternalCheck(String name, CheckSeverity severity, int numViolations, String function, List<KeyValue> properties) {
        super(name, severity, numViolations);
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
    private transient ICheckExtension externalCheck = null;

    @Override
    public AlarmState check(Object currentValue, Instant generationTime, int currentViolations, IBindingResolver resolver) throws CheckException {
        // Initialise the properties
        if(!properties.isEmpty() && key2values.isEmpty()) {
            properties.forEach(o -> key2values.put(o.getKey(), o.getValue()));
        }
        if(externalCheck == null) {
            // Retrieve calibration
            externalCheck = ExtensionRegistry.resolveCheck(this.function);
            if(externalCheck == null) {
                throw new CheckException("External calibration function " + function + " not found");
            }
        }
        boolean violated = externalCheck.check(currentValue, generationTime, key2values, resolver);
        return deriveState(violated, currentViolations);
    }

}
