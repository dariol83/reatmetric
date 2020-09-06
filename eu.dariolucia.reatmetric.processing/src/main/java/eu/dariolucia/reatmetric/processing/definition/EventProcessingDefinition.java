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

import eu.dariolucia.reatmetric.api.messages.Severity;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class EventProcessingDefinition extends AbstractProcessingDefinition {

    @XmlAttribute
    private Severity severity = Severity.INFO;

    @XmlAttribute
    private String type = "";

    /**
     * Inhibition period in milliseconds
     */
    @XmlAttribute(name = "inhibition_period")
    private int inhibitionPeriod = 0;

    @XmlAttribute(name = "log_enabled")
    private boolean logEnabled = true;

    @XmlElement(name = "condition")
    private ExpressionDefinition condition;

    /**
     * The inhibition period in milliseconds. If a new event is detected/reported within the inhibition period window,
     * the event is ignored.
     *
     * @return the inhibition period in milliseconds
     */
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

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }
}
