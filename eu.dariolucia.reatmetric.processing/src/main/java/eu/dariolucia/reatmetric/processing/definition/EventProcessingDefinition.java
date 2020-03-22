/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.messages.Severity;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventProcessingDefinition extends AbstractProcessingDefinition {

    @XmlAttribute
    private Severity severity = Severity.INFO;

    @XmlAttribute
    private String type = "";

    @XmlAttribute
    private int inhibitionPeriod = 0;

    @XmlElement(name = "condition")
    private ExpressionDefinition condition;

    public int getInhibitionPeriod() {
        return inhibitionPeriod;
    }

    public void setInhibitionPeriod(int inhibitionPeriod) {
        this.inhibitionPeriod = inhibitionPeriod;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public ExpressionDefinition getCondition() {
        return condition;
    }

    public void setCondition(ExpressionDefinition condition) {
        this.condition = condition;
    }
}
