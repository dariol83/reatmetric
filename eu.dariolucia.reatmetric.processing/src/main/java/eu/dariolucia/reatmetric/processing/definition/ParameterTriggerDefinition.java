/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;

@XmlAccessorType(XmlAccessType.FIELD)
public class ParameterTriggerDefinition {

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
