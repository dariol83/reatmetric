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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.processing.extension.ICheckExtension;
import eu.dariolucia.reatmetric.processing.extension.internal.ExtensionRegistry;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExternalCheck extends CheckDefinition implements Serializable {

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
