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

import javax.xml.bind.annotation.*;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterSetterDefinition {

    @XmlAttribute(name="activity", required = true)
    @XmlIDREF
    private ActivityProcessingDefinition activity;

    @XmlElement(name = "argument")
    private List<ArgumentInvocationDefinition> arguments = new LinkedList<>();

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    public ActivityProcessingDefinition getActivity() {
        return activity;
    }

    public void setActivity(ActivityProcessingDefinition activity) {
        this.activity = activity;
    }

    public List<ArgumentInvocationDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<ArgumentInvocationDefinition> arguments) {
        this.arguments = arguments;
    }

    public List<KeyValue> getProperties() {
        return properties;
    }

    public void setProperties(List<KeyValue> properties) {
        this.properties = properties;
    }
}
