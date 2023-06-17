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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterTriggerDefinition implements Serializable {

    @XmlIDREF
    @XmlAttribute(name = "event", required = true)
    private EventProcessingDefinition event;

    @XmlAttribute(name = "condition", required = true)
    private TriggerCondition triggerCondition;

    public ParameterTriggerDefinition() {
    }

    public ParameterTriggerDefinition(EventProcessingDefinition event, TriggerCondition triggerCondition) {
        this.event = event;
        this.triggerCondition = triggerCondition;
    }

    public EventProcessingDefinition getEvent() {
        return event;
    }

    public void setEvent(EventProcessingDefinition event) {
        this.event = event;
    }

    public TriggerCondition getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(TriggerCondition triggerCondition) {
        this.triggerCondition = triggerCondition;
    }
}
