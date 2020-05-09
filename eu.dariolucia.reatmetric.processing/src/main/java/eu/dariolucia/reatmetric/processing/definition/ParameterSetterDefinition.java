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

    @XmlIDREF
    @XmlAttribute(name="activity", required = true)
    private ActivityProcessingDefinition activity;

    @XmlAttribute(name="set_argument", required = true)
    private String setArgument;

    @XmlElements({
            @XmlElement(name="fixed_argument",type= PlainArgumentInvocationDefinition.class),
            @XmlElement(name="fixed_array",type= ArrayArgumentInvocationDefinition.class)
    })
    private List<AbstractArgumentInvocationDefinition> arguments = new LinkedList<>();

    @XmlElement(name = "property")
    private List<KeyValue> properties = new LinkedList<>();

    public String getSetArgument() {
        return setArgument;
    }

    public void setSetArgument(String setArgument) {
        this.setArgument = setArgument;
    }

    public ActivityProcessingDefinition getActivity() {
        return activity;
    }

    public void setActivity(ActivityProcessingDefinition activity) {
        this.activity = activity;
    }

    public List<AbstractArgumentInvocationDefinition> getArguments() {
        return arguments;
    }

    public void setArguments(List<AbstractArgumentInvocationDefinition> arguments) {
        this.arguments = arguments;
    }

    public List<KeyValue> getProperties() {
        return properties;
    }

    public void setProperties(List<KeyValue> properties) {
        this.properties = properties;
    }
}
